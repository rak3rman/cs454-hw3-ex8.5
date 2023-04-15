# cs454-hw3-ex8.5

# Homework 3, Exercise 8.5

A savings account object holds a nonnegative balance, and provides deposit(k) and withdraw(k) methods, where deposit(k) adds k to the balance, and withdraw(k) subtracts k, if the balance is at least k, and otherwise blocks until the balance becomes k or greater.
1. Implement this savings account using locks and conditions.
2. Now suppose there are two kinds of withdrawals: ordinary and preferred. Devise
   an implementation that ensures that no ordinary withdrawal occurs if there is a
   preferred withdrawal waiting to occur.
3. Now add a transfer() method that transfers a sum from one account to another:

## Running the Project
Run Java class using the following command.
```
javac Bank.java && java Bank
```
Expects the following inputs on the command line:

- Num of accounts (int): 1 (number of accounts to create on preferred and total race)
- Num of threads (int): 10 (number of threads to run concurrently per account on preferred and total race)
- Debug mode (y/n): y (show additional debug output)

Runs through the following executions:

- Sets up all accounts and all threads pointing to accounts upfront, suspends threads from completing actions
- Waits 1 second
- Resumes 4 threads, allowing them to compete for withdraw priority, demonstrating preferred vs ordinary priority
- Waits 5 seconds
- Resumes remaining threads, running through a brute force race where threads deposit, withdraw, and transfer money at different intervals/rates, change num of threads per account to increase contention
- Returns 0 on successful completion (all threads returned)

## Example Output
```
Opening Radison's Super Fancy Bank

Num of accounts (int): 1
Num of threads (int): 1
Debug mode (y/n): n

=====================================
BANK: Setting up accounts...
(WC|A0-T0) : Thread primed...
(WC|A0-T1) : Thread primed...
(WC|A0-T2) : Thread primed...
(WC|A0-T3) : Thread primed...
(T|A0-T0) : Thread primed...
(P|A0-T0) : Thread primed...

=====================================
BANK: Now open for withdraw competition (tests withdraw priority)
(WC|A0-T0) : Thread, withdrawing 100...
(WC|A0-T2) : Thread, withdrawing 100...
(WC|A0-T1) : Thread, withdrawing 100...
(WC|A0-T3) : Thread, depositing 300...
(WC|A0-T3) : Deposit, completed, bal = '300', k = 300
(WC|A0-T0) : Preferred Withdraw, completed, bal = '200', k = 100, ord withdraw waiting = true
(WC|A0-T2) : Preferred Withdraw, completed, bal = '100', k = 100, ord withdraw waiting = false
(WC|A0-T1) : Ordinary Withdraw, completed, bal = '0', k = 100

=====================================
BANK: Now open for preferred and totals race (brute force test)
(T|A0-T0) : Thread, depositing 100...
(T|A0-T0) : Deposit, completed, bal = '100', k = 100
(P|A0-T0) : Thread, depositing 100...
(T|A0-T0) : Thread, withdrawing 100...
(P|A0-T0) : Deposit, completed, bal = '100', k = 100
(T|A0-T0) : Ordinary Withdraw, completed, bal = '0', k = 100
(P|A0-T0) : Thread, withdrawing 100...
(T|A0-T0) : Thread, depositing 50...
(P|A0-T0) : Ordinary Withdraw, completed, bal = '0', k = 100
(T|A0-T0) : Deposit, completed, bal = '50', k = 50
(T|A0-T0) : Thread, withdrawing 15...
(T|A0-T0) : Ordinary Withdraw, completed, bal = '35', k = 15
(T|A0-T0) : Thread, withdrawing 10...
(T|A0-T0) : Preferred Withdraw, completed, bal = '25', k = 10, ord withdraw waiting = false
(T|A0-T0) : Thread, transferring 25...
(T|A0-T0) : Ordinary Withdraw, completed, bal = '0', k = 25
(T|A0-T0) : Deposit, completed, bal = '25', k = 25
(T|A0-T0) : Thread, withdrawing 25...
(T|A0-T0) : Ordinary Withdraw, completed, bal = '0', k = 25

Process finished with exit code 0
```