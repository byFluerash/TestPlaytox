package org.playtox.model.transfer;

import lombok.*;

import java.util.concurrent.locks.ReentrantLock;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class Account {

    private final String id;
    private volatile int balance;
    private final ReentrantLock lock = new ReentrantLock();

    void withdraw(int amount) {
        balance -= amount;
    }

    void deposit(int amount) {
        balance += amount;
    }

}
