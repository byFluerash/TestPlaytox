package org.playtox.model;

import org.playtox.model.transfer.TransferService;
import org.playtox.model.transfer.TransferServiceImpl;

import java.util.Random;

public class Main {
    public static void main(String[] args) {

        TransferService transferService = new TransferServiceImpl();
        Random random = new Random();

        int randomNumberOfAccounts = random.nextInt(4, 20);
        int randomNumberOfThreads = random.nextInt(2, 10);

        TransferGenerator generator = new TransferGenerator(
                randomNumberOfAccounts,
                transferService,
                30,
                randomNumberOfThreads
        );

        generator.run();
    }
}
