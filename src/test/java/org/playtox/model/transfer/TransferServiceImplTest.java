package org.playtox.model.transfer;

import org.junit.jupiter.api.Test;
import org.playtox.model.TransferGenerator;


import static org.junit.jupiter.api.Assertions.*;

class TransferServiceImplTest {

    private final TransferService transferService = new TransferServiceImpl();

    @Test
    void shouldTransferMoneySuccessfully() {
        Account from = new Account("A1", 1000);
        Account to = new Account("A2", 500);

        boolean result = transferService.transferMoney(from, to, 300);

        assertTrue(result);
        assertEquals(700, from.getBalance());
        assertEquals(800, to.getBalance());
    }
    @Test
    void shouldFailTransferWhenInsufficientFunds() {
        Account from = new Account("A1", 200);
        Account to = new Account("A2", 1000);

        boolean result = transferService.transferMoney(from, to, 300);

        assertFalse(result);
        assertEquals(200, from.getBalance()); // не изменился
        assertEquals(1000, to.getBalance());   // не изменился
    }

    @Test
    void shouldExecuteExactlyTargetTransfersAndPreserveTotalBalance() throws InterruptedException {
        int accountsCount = 4;
        int targetTransfers = 20;
        int threadCount = 3;
        int initialBalance = 10_000;

        TransferService transferService = new TransferServiceImpl();
        TransferGenerator generator = new TransferGenerator(accountsCount, transferService, targetTransfers, threadCount);

        Thread runner = new Thread(generator::run);
        runner.start();
        runner.join(1000*targetTransfers);


        long total = generator.getAccounts().stream()
                .mapToLong(Account::getBalance)
                .sum();
        assertEquals((long) accountsCount * initialBalance, total);


        assertEquals(targetTransfers, generator.getSuccessfulTransfersCount());
    }

}