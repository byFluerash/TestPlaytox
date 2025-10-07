package org.playtox.model.transfer;

public interface TransferService {

    public boolean transferMoney(Account from, Account to, int amount);
}
