import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Bank {

    private static boolean debug;

    //
    // Account
    //
    private static class Account {

        private int bal = 0;
        private ReentrantLock lock = new ReentrantLock();
        private Condition canWriteOrdWithdraw = lock.newCondition();
        private Condition canWritePrefWithdraw = lock.newCondition();

        //
        // Output
        //
        public void output(String tid) {
            lock.lock();
            try {
                System.out.println("(" + tid + ") : Balance = '" + (bal) + "'");
            } finally {
                lock.unlock();
            }
        }

        //
        // Deposit
        //
        public void deposit(int k, String tid) {
            if (debug) System.out.println("(" + tid + ") : Deposit, acquiring write lock...");
            lock.lock();
            try {
                System.out.println("(" + tid + ") : Deposit, completed, bal = '" + (bal + k) + "', k = " + k);
                bal = bal + k;
                if (lock.hasWaiters(canWritePrefWithdraw)) {
                    canWritePrefWithdraw.signalAll();
                } else {
                    canWriteOrdWithdraw.signalAll();
                }
            } finally {
                lock.unlock();
                if (debug) System.out.println("(" + tid + ") : Deposit, write lock released");
            }
        }

        //
        // Withdraw
        //
        public void withdraw(int k, String tid) {
            lock.lock();
            try {
                // Has write lock, no other account can read/write
                // Determine if (bal - k) will be negative and if preferred is waiting
                if (debug) System.out.println("(" + tid + ") : Ordinary Withdraw, got write lock, checking bal = " + bal + ", k = " + k);
                while (bal - k < 0 || lock.hasWaiters(canWriteOrdWithdraw)) {
                    // Sleep here until signal-ed
                    if (debug) System.out.println("(" + tid + ") : Ordinary Withdraw, bal would be neg or preferred waiting, going to sleep...");
                    canWriteOrdWithdraw.await();
                    if (debug) System.out.println("(" + tid + ") : Ordinary Withdraw, woke up...");
                }
                // Complete action
                System.out.println("(" + tid + ") : Ordinary Withdraw, completed, bal = '" + (bal - k) + "', k = " + k);
                bal = bal - k;
                if (lock.hasWaiters(canWritePrefWithdraw)) {
                    canWritePrefWithdraw.signalAll();
                } else {
                    canWriteOrdWithdraw.signalAll();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
                if (debug) System.out.println("(" + tid + ") : Ordinary Withdraw, write lock released");
            }
        }

        //
        // Preferred Withdraw
        //
        public void preferredWithdraw(int k, String tid) {
            lock.lock();
            try {
                // Has write lock, no other account can read/write
                // Determine if (bal - k) will be negative
                if (debug) System.out.println("(" + tid + ") : Preferred Withdraw, got write lock, checking bal = " + bal + ", k = " + k);
                while (bal - k < 0) {
                    // Sleep here until signal-ed
                    if (debug) System.out.println("(" + tid + ") : Preferred Withdraw, bal would be neg, going to sleep...");
                    canWritePrefWithdraw.await();
                    if (debug) System.out.println("(" + tid + ") : Preferred Withdraw, woke up...");
                }
                // Complete action
                System.out.println("(" + tid + ") : Preferred Withdraw, completed, bal = '" + (bal - k) + "', k = " + k + ", ord withdraw waiting = " + lock.hasWaiters(canWriteOrdWithdraw));
                bal = bal - k;
                if (lock.hasWaiters(canWritePrefWithdraw)) {
                    canWritePrefWithdraw.signalAll();
                } else {
                    canWriteOrdWithdraw.signalAll();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
                if (debug) System.out.println("(" + tid + ") : Preferred Withdraw, write lock released");
            }
        }

        //
        // Transfer
        //
        public void transfer(int k, Account reserve, String tid) {
            if (debug) System.out.println("(" + tid + ") : Transfer, withdrawing...");
            reserve.withdraw(k, tid);
            if (debug) System.out.println("(" + tid + ") : Transfer, depositing...");
            deposit(k, tid);
        }
    }

    //
    // Main
    //
    public static void main(String[] args){
        System.out.println("Opening Radison's Super Fancy Bank\n");

        // Gather user input on n number of accounts -> numAccounts var
        Scanner sc = new Scanner(System.in);
        System.out.print("Num of accounts (int): ");
        int numAccounts = sc.nextInt();

        // Gather user input on n number of threads per account -> numThreads var
        sc = new Scanner(System.in);
        System.out.print("Num of threads (int): ");
        int numThreads = sc.nextInt();

        // Gather user input on debug mode -> debug var
        sc = new Scanner(System.in);
        System.out.print("Debug mode (y/n): ");
        debug = (Objects.equals(sc.next(), "y"));

        // Start Bank
        System.out.println("\n=====================================");
        System.out.println("BANK: Setting up accounts...");

        Account[] preferAccs = new Account[numAccounts];
        Account[] totalsAccs = new Account[numAccounts];
        Account withdrawCompAcc = new Account();

        final CyclicBarrier preferGate = new CyclicBarrier(numAccounts * numThreads + 1);
        final CyclicBarrier totalsGate = new CyclicBarrier(numAccounts * numThreads + 1);
        final CyclicBarrier withdrawCompGate = new CyclicBarrier(5);

        for (int i = 0; i < numAccounts; i++) {
            preferAccs[i] = new Account();
            final int finalI = i;
            for (int j = 0; j < numThreads; j++) {
                final int finalJ = j;
                new Thread(() -> {
                    // Wait on Gate
                    System.out.println("(P|A" + finalI + "-T" + finalJ + ") : Thread primed...");
                    try {
                        preferGate.await();
                    } catch (InterruptedException | BrokenBarrierException e) {
                        throw new RuntimeException(e);
                    }
                    // Interleaving
                    if (ThreadLocalRandom.current().nextBoolean()) {
                        // Deposit into this account
                        System.out.println("(P|A" + finalI + "-T" + finalJ + ") : Thread, depositing 100...");
                        preferAccs[finalI].deposit(100, "P|A" + finalI + "-T" + finalJ);
                    } else {
                        // Deposit into this account
                        System.out.println("(P|A" + finalI + "-T" + finalJ + ") : Thread, depositing 50...");
                        preferAccs[finalI].deposit(50, "P|A" + finalI + "-T" + finalJ);
                        // Deposit into this account
                        System.out.println("(P|A" + finalI + "-T" + finalJ + ") : Thread, depositing 25...");
                        preferAccs[finalI].deposit(25, "P|A" + finalI + "-T" + finalJ);
                        try {
                            Thread.sleep(60);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        // Deposit into this account
                        System.out.println("(P|A" + finalI + "-T" + finalJ + ") : Thread, depositing 25...");
                        preferAccs[finalI].deposit(25, "P|A" + finalI + "-T" + finalJ);
                    }
                    if (ThreadLocalRandom.current().nextBoolean()) {
                        // Withdraw from this account
                        System.out.println("(P|A" + finalI + "-T" + finalJ + ") : Thread, withdrawing 100...");
                        preferAccs[finalI].withdraw(100, "P|A" + finalI + "-T" + finalJ);
                        try {
                            Thread.sleep(30);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        // Withdraw from this account
                        System.out.println("(P|A" + finalI + "-T" + finalJ + ") : Thread, withdrawing 15...");
                        preferAccs[finalI].preferredWithdraw(15, "P|A" + finalI + "-T" + finalJ);
                        try {
                            Thread.sleep(70);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        // Withdraw from this account
                        System.out.println("(P|A" + finalI + "-T" + finalJ + ") : Thread, withdrawing 75...");
                        preferAccs[finalI].preferredWithdraw(75, "P|A" + finalI + "-T" + finalJ);
                        // Withdraw from this account
                        System.out.println("(P|A" + finalI + "-T" + finalJ + ") : Thread, withdrawing 10...");
                        preferAccs[finalI].withdraw(10, "P|A" + finalI + "-T" + finalJ);
                    }
                }).start();
            }
        }

        for (int i = 0; i < numAccounts; i++) {
            totalsAccs[i] = new Account();
            final int finalI = i;
            for (int j = 0; j < numThreads; j++) {
                final int finalJ = j;
                new Thread(() -> {
                    // Wait on Gate
                    System.out.println("(T|A" + finalI + "-T" + finalJ + ") : Thread primed...");
                    try {
                        totalsGate.await();
                    } catch (InterruptedException | BrokenBarrierException e) {
                        throw new RuntimeException(e);
                    }
                    // Deposit into this account
                    System.out.println("(T|A" + finalI + "-T" + finalJ + ") : Thread, depositing 100...");
                    totalsAccs[finalI].deposit(100, "T|A" + finalI + "-T" + finalJ);
                    // Withdraw from this account
                    System.out.println("(T|A" + finalI + "-T" + finalJ + ") : Thread, withdrawing 100...");
                    totalsAccs[finalI].withdraw(100,  "T|A" + finalI + "-T" + finalJ);
                    // Deposit into this account
                    System.out.println("(T|A" + finalI + "-T" + finalJ + ") : Thread, depositing 50...");
                    totalsAccs[finalI].deposit(50, "T|A" + finalI + "-T" + finalJ);
                    // Withdraw from this account
                    System.out.println("(T|A" + finalI + "-T" + finalJ + ") : Thread, withdrawing 15...");
                    totalsAccs[finalI].withdraw(15, "T|A" + finalI + "-T" + finalJ);
                    // Withdraw from this account
                    System.out.println("(T|A" + finalI + "-T" + finalJ + ") : Thread, withdrawing 10...");
                    totalsAccs[finalI].preferredWithdraw(10, "T|A" + finalI + "-T" + finalJ);
                    // Transfer from next account
                    System.out.println("(T|A" + finalI + "-T" + finalJ + ") : Thread, transferring 25...");
                    totalsAccs[finalI].transfer(25,  totalsAccs[(finalI + 1) % numAccounts], "T|A" + finalI + "-T" + finalJ);
                    // Withdraw from this account
                    System.out.println("(T|A" + finalI + "-T" + finalJ + ") : Thread, withdrawing 25...");
                    totalsAccs[finalI].withdraw(25, "T|A" + finalI + "-T" + finalJ);
                }).start();
            }
        }

        // Withdraw Competition
        // Create threads
        new Thread(() -> {
            final int finalJ = 0;
            // Wait on Gate
            System.out.println("(WC|A" + 0 + "-T" + finalJ + ") : Thread primed...");
            try {
                withdrawCompGate.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
            // Withdraw from this account
            System.out.println("(WC|A" + 0 + "-T" + finalJ + ") : Thread, withdrawing 100...");
            withdrawCompAcc.preferredWithdraw(100,  "WC|A" + 0 + "-T" + finalJ);
        }).start();

        new Thread(() -> {
            final int finalJ = 1;
            // Wait on Gate
            System.out.println("(WC|A" + 0 + "-T" + finalJ + ") : Thread primed...");
            try {
                withdrawCompGate.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
            // Withdraw from this account
            System.out.println("(WC|A" + 0 + "-T" + finalJ + ") : Thread, withdrawing 100...");
            withdrawCompAcc.withdraw(100,  "WC|A" + 0 + "-T" + finalJ);
        }).start();

        new Thread(() -> {
            final int finalJ = 2;
            // Wait on Gate
            System.out.println("(WC|A" + 0 + "-T" + finalJ + ") : Thread primed...");
            try {
                withdrawCompGate.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
            // Withdraw from this account
            System.out.println("(WC|A" + 0 + "-T" + finalJ + ") : Thread, withdrawing 100...");
            withdrawCompAcc.preferredWithdraw(100,  "WC|A" + 0 + "-T" + finalJ);
        }).start();

        new Thread(() -> {
            final int finalJ = 3;
            // Wait on Gate
            System.out.println("(WC|A" + 0 + "-T" + finalJ + ") : Thread primed...");
            try {
                withdrawCompGate.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            // Deposit into this account
            System.out.println("(WC|A" + 0 + "-T" + finalJ + ") : Thread, depositing 300...");
            withdrawCompAcc.deposit(300, "WC|A" + 0 + "-T" + finalJ);
        }).start();

        // Open Gate
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("\n=====================================");
        System.out.println("BANK: Now open for withdraw competition (tests withdraw priority)");
        try {
            withdrawCompGate.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }

        // Open Gate
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("\n=====================================");
        System.out.println("BANK: Now open for preferred and totals race (brute force test)");
        try {
            preferGate.await();
            totalsGate.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }

    }

}