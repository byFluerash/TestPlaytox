package org.playtox.model.transfer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class TransferServiceImpl implements TransferService {
    private static final Logger log = LoggerFactory.getLogger(TransferServiceImpl.class);


    @Override
    public boolean transferMoney(Account from, Account to, int amount) {
        if (!isParamsValid(from, to, amount)) return false;

        boolean whoFirst = from.getId().compareTo(to.getId()) < 0;
        ReentrantLock firstLock = whoFirst ? from.getLock() : to.getLock();
        ReentrantLock secondLock = whoFirst ? to.getLock() : from.getLock();

        try {
            if (firstLock.tryLock(1, TimeUnit.SECONDS)) {
                try {
                    if (secondLock.tryLock(1, TimeUnit.SECONDS)) {
                        try {
                            if (from.getBalance() < amount) {
                                log.info("Transfer failed: insufficient funds from {}. Missing are {} units", from.getId(), amount - from.getBalance());
                                return false;
                            }
                            from.withdraw(amount);
                            to.deposit(amount);

                            log.info("Transfer succeeded: {} units from {} to {}. New balances: {} -> {}, {} -> {}",
                                    amount,
                                    from.getId(),
                                    to.getId(),
                                    from.getId(), from.getBalance(),
                                    to.getId(), to.getBalance());
                            return true;
                        } finally {
                            secondLock.unlock();
                        }
                    } else {
                        log.warn("Transfer failed: could not acquire lock on second account ({}) within timeout",
                                (whoFirst ? to.getId() : from.getId()));
                    }

                } finally {
                    firstLock.unlock();
                }

            } else {
                log.warn("Transfer failed: could not acquire lock on second account ({}) within timeout",
                        whoFirst ? from.getId() : to.getId());
            }

            return false;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Transfer interrupted while waiting for locks: from {} to {}, amount {}",
                    from.getId(), to.getId(), amount, e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during transfer from {} to {}, amount {}",
                    from.getId(), to.getId(), amount, e);
            return false;
        }
    }

    private boolean isParamsValid(Account from, Account to, int amount) {
        if (from == null || to == null) {
            log.warn("Transfer failed: from or to account is null. From: {}, To: {}", from, to);
            return false;
        }
        if (from == to) {
            log.warn("Transfer failed: cannot transfer from account {} to itself", from.getId());
            return false;
        }

        if (amount < 0) {
            log.warn("Transfer failed: amount is negative: {} ", amount);
            return false;
        }
        return true;
    }
}
