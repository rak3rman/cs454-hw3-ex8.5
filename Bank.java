import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Bank {

    private static boolean debug;

    //
    // Account
    //
    private static class Account {

        private static int id;
        private static int bal = 0;
        private final ReadWriteLock balLock = new ReentrantReadWriteLock();
        private final ReentrantLock prefLock = new ReentrantLock();

        //
        // Constructor
        //
        public Account(int identity, int starting) {
            id = identity;
            if (debug) System.out.println("(" + id + ") : Hit constructor.");
        }

        //
        // Output
        //
        public void output(String tid) {
            balLock.readLock().lock();
            try {
                System.out.println("(" + tid + ") : Balance = '" + (bal) + "'");
            } finally {
                balLock.readLock().unlock();
            }
        }

        //
        // Deposit
        //
        public void deposit(int k, String tid) {
            if (debug) System.out.println("(" + tid + ") : Deposit, acquiring write lock...");
            balLock.writeLock().lock();
            try {
                System.out.println("(" + tid + ") : Deposit, got write lock, bal now '" + (bal + k) + "'");
                bal = bal + k;
            } finally {
                balLock.writeLock().unlock();
                if (debug) System.out.println("(" + tid + ") : Deposit, write lock released");
            }
        }

        //
        // Withdraw
        //
        public void withdraw(int k, boolean isPreferred, String tid) {
            // Loop until we are successful in withdrawing
            while (true) {
                // Acquire readLock on bal
                balLock.readLock().lock();
                // Determine if (bal - k) will be positive
                if (bal - k >= 0) {
                    // (bal - k) should be positive, acquire write lock
                    if (debug) System.out.println("(" + tid + ") : Withdraw, read looks good, acquiring write lock...");
                    balLock.readLock().unlock();
                    balLock.writeLock().lock();
                    try {
                        // Has write lock, no other account can read/write
                        // Determine if (bal - k) will be negative, break if so
                        if (debug) System.out.println("(" + tid + ") : Withdraw, got write lock, checking bal - k again...");
                        if (bal - k >= 0) {
                            // Complete action
                            System.out.println("(" + tid + ") : Withdraw, bal - k good, bal now '" + (bal - k) + "'");
                            bal = bal - k;
                            return;
                        }
                    } finally {
                        balLock.writeLock().unlock();
                        if (debug) System.out.println("(" + tid + ") : Withdraw, write lock released");
                    }
                } else {
                    balLock.readLock().unlock();
                }
            }
        }

        //
        // Transfer
        //
        public void transfer(int k, Account reserve, String tid) {
            if (debug) System.out.println("(" + id + ") : Transfer, withdrawing...");
            reserve.withdraw(k, false, tid);
            if (debug) System.out.println("(" + id + ") : Transfer, depositing...");
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
//        System.out.print("Num of accounts (int): ");
//        int numAccounts = sc.nextInt();
        int numAccounts = 1;

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

        final CyclicBarrier gate = new CyclicBarrier(numAccounts * numThreads + 1);

        final Account acc0 = new Account(0, 0);
        final Account acc1 = new Account(1, 0);

        for (int j = 0; j < numThreads; j++) {
            final int finalJ = j;
            new Thread(() -> {
                // Wait on Gate
                if (debug) System.out.println("(A" + 0 + "-T" + finalJ + ") : Thread, waiting at gate...");
                try {
                    gate.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
                // Deposit into account 1
                System.out.println("(A" + 0 + "-T" + finalJ + ") : Thread, depositing 100...");
                acc0.deposit(100, "A0-T" + finalJ);
                System.out.println("(A" + 0 + "-T" + finalJ + ") : Thread, depositing 1...");
                acc1.deposit(1, "A1-T" + finalJ);
                // Transfer into account 2 from 1
                System.out.println("(A" + 0 + "-T" + finalJ + ") : Thread, transferring 100...");
                acc0.transfer(100, acc1, "A0-T" + finalJ);
                // Withdraw from account 2
                System.out.println("(A" + 0 + "-T" + finalJ + ") : Thread, withdrawing 100...");
                acc1.withdraw(90, false, "A1-T" + finalJ);
                // Print total
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                acc0.output("A0-T" + finalJ);
                acc1.output("A1-T" + finalJ);
            }).start();
        }

        // Open Gate
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("\n=====================================");
        System.out.println("BANK: Now open. Off to the races!");
        try {
            gate.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }

    }

}