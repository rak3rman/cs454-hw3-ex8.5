import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Bank {

    private static boolean debug;

    //
    // Account
    //
    private static class Account {

        private static int id;
        private static int bal = 0;
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition canWriteWithdraw = lock.newCondition();

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
                System.out.println("(" + tid + ") : Deposit, got write lock, bal now '" + (bal + k) + "'");
                bal = bal + k;
            } finally {
                lock.unlock();
                if (debug) System.out.println("(" + tid + ") : Deposit, write lock released");
            }
        }

        //
        // Withdraw
        //
        public void withdraw(int k, boolean isPreferred, String tid) {
            lock.lock();
            try {
                // Has write lock, no other account can read/write
                // Determine if (bal - k) will be negative, then check for waiters sleeping with isPreferred
                if (debug) System.out.println("(" + tid + ") : Withdraw, got write lock, checking bal - k and isPreferred...");
                while (bal - k < 0 || (isPreferred || lock.hasWaiters(canWriteWithdraw))) {
                    // Sleep here until signal-ed
                    canWriteWithdraw.await();
                }
                // Complete action
                System.out.println("(" + tid + ") : Withdraw, bal - k and isPreferred good, bal now '" + (bal - k) + "'");
                bal = bal - k;
                canWriteWithdraw.signalAll();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
                if (debug) System.out.println("(" + tid + ") : Withdraw, write lock released");
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

        for (int j = 0; j < numThreads; j++) {
            final int finalJ = j;
            new Thread(() -> {
                // Wait on Gate
                System.out.println("(A" + 0 + "-T" + finalJ + ") : Thread primed...");
                try {
                    gate.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
                // Deposit into account 0
                System.out.println("(A" + 0 + "-T" + finalJ + ") : Thread, depositing 100...");
                acc0.deposit(100, "A0-T" + finalJ);
                // Withdraw from account 0
                System.out.println("(A" + 0 + "-T" + finalJ + ") : Thread, withdrawing 100...");
                acc0.withdraw(100, false, "A0-T" + finalJ);
                // Deposit into account 0
                System.out.println("(A" + 0 + "-T" + finalJ + ") : Thread, depositing 100...");
                acc0.deposit(100, "A0-T" + finalJ);
                // Withdraw from account 0
                System.out.println("(A" + 0 + "-T" + finalJ + ") : Thread, withdrawing 100...");
                acc0.withdraw(100, false, "A0-T" + finalJ);
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