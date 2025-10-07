package org.playtox.model;

import org.playtox.model.transfer.Account;
import org.playtox.model.transfer.TransferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public class TransferGenerator {
    private static final Logger log = LoggerFactory.getLogger(TransferGenerator.class);

    private final List<Account> accounts;


    private final TransferService transferService;
    private final int targetTransfersCount;
    private final int threadCount;
    private final int accountsCount;
    private final ExecutorService executorService;
    private final AtomicInteger successfulTransfersCount = new AtomicInteger(0);
    private final CountDownLatch completionLatch = new CountDownLatch(1);
    private volatile boolean isShutdown = false;

    public TransferGenerator(
            int accountsCount,
            TransferService transferService,
            int targetTransfersCount,
            int threadCount) {

        if (accountsCount < 2) {
            throw new IllegalArgumentException("At least 2 accounts are required");
        }
        if (targetTransfersCount <= 0) {
            throw new IllegalArgumentException("Target transaction count must be positive");
        }
        if (threadCount <= 0) {
            throw new IllegalArgumentException("Worker thread count must be positive");
        }
        this.accountsCount = accountsCount;
        this.accounts = generateAccounts(accountsCount);
        this.transferService = transferService;
        this.targetTransfersCount = targetTransfersCount;
        this.threadCount = threadCount;
        this.executorService = Executors.newFixedThreadPool(threadCount);
    }

    public void run() {

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(new TransferWorker());
        }

        try {
            completionLatch.await();

            log.info("🎯 Target of {} transfers reached. Shutting down workers...", targetTransfersCount);
            executorService.shutdown();

            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Workers didn't terminate gracefully. Forcing shutdown.");
                executorService.shutdownNow();
            }

            log.info("✅ Application finished successfully.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Main thread was interrupted", e);
            executorService.shutdownNow();
        }


    }

    public List<Account> getAccounts() {
        return new ArrayList<>(accounts);
    }

    public int getSuccessfulTransfersCount() {
        return successfulTransfersCount.get();
    }

    private class TransferWorker implements Runnable {

        @Override
        public void run() {
            while (true) {

                int current = successfulTransfersCount.get();
                if (current >= targetTransfersCount) {
                    break;
                }

                if (!successfulTransfersCount.compareAndSet(current, current + 1)) {
                    continue;
                }


                try {
                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    Thread.sleep(1000 + random.nextInt(1001));

                    Account from = getRandomAccount(random);
                    Account to = getRandomAccountExcluding(random, from);
                    int amount = random.nextInt(1, 10_000);

                    if (transferService.transferMoney(from, to, amount)) {
                        int newCount = current + 1;
                        log.debug("Successful transaction #{}", newCount);

                        if (newCount == targetTransfersCount) {
                            if (completionLatch.getCount() > 0) {
                                completionLatch.countDown();
                                log.info("🎯 Target of {} transfers reached.", targetTransfersCount);
                            }
                        }
                    } else {
                        successfulTransfersCount.decrementAndGet();
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    // Откатываем, если прервано до завершения
                    successfulTransfersCount.decrementAndGet();
                    break;
                } catch (Exception e) {
                    log.error("Unexpected error in worker", e);
                    successfulTransfersCount.decrementAndGet();
                }
            }
            log.debug("Worker thread exiting.");
        }
    }

    private List<Account> generateAccounts(int accountsCount) {
        List<Account> resultList = new ArrayList<>();
        for (int i = 0; i < accountsCount; i++) {
            resultList.add(new Account(UUID.randomUUID().toString(), 10_000));
        }
        return resultList;
    }


    private Account getRandomAccount(ThreadLocalRandom random) {
        synchronized (accounts) {
            return accounts.get(random.nextInt(accountsCount));
        }
    }

    private Account getRandomAccountExcluding(ThreadLocalRandom random, Account exclude) {
        synchronized (accounts) {
            if (accounts.size() == 1) return null;
            Account result;
            do {
                result = accounts.get(random.nextInt(accountsCount));
            } while (result == exclude);
            return result;
        }
    }

}

