import java.util.concurrent.Semaphore;
import java.util.Random;

public class Bank {

    static Random random = new Random();
    static public Semaphore manager = new Semaphore(1);
    static public Semaphore safe = new Semaphore(2);
    static public Semaphore tellers = new Semaphore(3);
    static public Semaphore customers = new Semaphore(3);
    static public Semaphore entry = new Semaphore(2);
    static public Teller tellerArray[];
    static public Customer customerArray[];

    static public int customersServed = 0;

    Bank() {
        tellerArray = new Teller[3];
        customerArray = new Customer[50];
    }

    public void runSimulation() throws InterruptedException {
        for (int i = 0; i < 3; i++) {
            tellerArray[i] = new Teller(i + 1);
            tellerArray[i].start();
        }

        System.out.println("Bank is now open!");

        for (int i = 0; i < 50; i++) {
            customerArray[i] = new Customer(i + 1);
            customerArray[i].start();
            Thread.sleep(10);
        }
    }

    //-----------------------------------------------------------------------------------------------------------------------------
    public static class Teller extends Thread {
        int tellerId;
        boolean status = false;
        Customer myCustomer;
        Semaphore myMonitor = new Semaphore(1);

        Teller(int id) {
            tellerId = id;
        }

        public void setStatus(boolean status) {
            this.status = status;
        }

        public void setMyCustomer(int id) {
            for (int i = 0; i < customerArray.length; i++) {
                if (customerArray[i].getCustomerId() == id) {
                    myCustomer = customerArray[i];
                    break;
                }
            }
            setStatus(true);
        }

        public int getTellerId() {
            return tellerId;
        }

        public boolean isBusy() {
            return status;
        }

        public Semaphore getMyMonitor() {
            return myMonitor;
        }

        public void withdrawal() throws InterruptedException {
            System.out.println("Teller " + tellerId + " is handling the " + myCustomer.getTransaction() + " transaction.");

            System.out.println("Teller " + tellerId + " is going to the manager.");
            manager.acquire();
            System.out.println("Teller " + tellerId + " is getting the manager's permission.");
            Thread.sleep(random.nextInt((30 - 5) + 1) + 5);
            System.out.println("Teller " + tellerId + " got the manager's permission.");
            manager.release();

            System.out.println("Teller " + tellerId + " is going to the safe.");
            safe.acquire();
            System.out.println("Teller " + tellerId + " is in the safe.");
            Thread.sleep(random.nextInt((50 - 10) + 1) + 10);
            safe.release();
            System.out.println("Teller " + tellerId + " exited the safe.");
        }

        public void deposit() throws InterruptedException {
            System.out.println("Teller " + tellerId + " is handling the " + myCustomer.getTransaction() + " transaction.");
            System.out.println("Teller " + tellerId + " is going to the safe.");
            safe.acquire();
            System.out.println("Teller " + tellerId + " entered the safe.");
            Thread.sleep(random.nextInt((50 - 10) + 1) + 10);
            safe.release();
            System.out.println("Teller " + tellerId + " exited the safe.");
        }

        public void service() throws InterruptedException {
            System.out.println("Teller " + tellerId + " is serving Customer " + myCustomer.getCustomerId() + ".");
            System.out.println("Teller " + tellerId + " asks Customer " + myCustomer.getCustomerId() + " for their transaction.");
            if (myCustomer.getTransaction().equals("withdrawal")) {
                System.out.println("Customer " + myCustomer.getCustomerId() + " asks for a " + myCustomer.getTransaction() + " transaction.");
                withdrawal();

            } else if (myCustomer.getTransaction().equals("deposit")) {
                System.out.println("Customer " + myCustomer.getCustomerId() + " asks for a " + myCustomer.getTransaction() + " transaction.");
                deposit();
            }
            System.out.println("Teller " + tellerId + " has completed Customer " + myCustomer.getCustomerId() + "'s transaction.");
            setStatus(false);
            if (customersServed <= 46) {
                System.out.println("Teller " + tellerId + " is awaiting next customer.");
            }
        }

        public void run() {
            synchronized (myMonitor) {
                try {
                    while (customersServed != 49) {
                        tellers.acquire();
                        myMonitor.wait();
                        service();
                        myMonitor.notify();
                        tellers.release();
                    }
                } catch (Exception e) {
                    System.out.println("Error in Teller Thread " + tellerId + ": " + e);
                }
            }
        }
    }

    //-----------------------------------------------------------------------------------------------------------------------------
    public static class Customer extends Thread {
        int customerId;
        Teller myTeller;
        String transaction = "";
        Semaphore monitor = new Semaphore(1);

        Customer(int id) {
            customerId = id;
        }

        public void setTransaction() {
            boolean randomTransaction = random.nextBoolean();
            if (randomTransaction == true) {
                transaction = "withdrawal";
            } else {
                transaction = "deposit";
            }
        }

        public String getTransaction() {
            return transaction;
        }

        public int getCustomerId() {
            return customerId;
        }

        public void findTeller() {
            boolean looking = true;
            System.out.println("Customer " + customerId + " is selecting a teller.");
            while (looking) {
                for (int i = 0; i < tellerArray.length; i++) {
                    if (tellerArray[i].isBusy() == false) {
                        myTeller = tellerArray[i];
                        tellerArray[i].setStatus(true);
                        myTeller.setMyCustomer(customerId);
                        System.out.println("Customer " + customerId + " goes to Teller " + myTeller.getTellerId());
                        looking = false;
                        break;
                    }
                }
            }
            monitor = myTeller.getMyMonitor();
        }

        public void entrance() throws InterruptedException {
            System.out.println("Customer " + customerId + " is going to the bank.");
            entry.acquire();
            System.out.println("Customer " + customerId + " is getting in line.");
            entry.release();
        }

        public void introduction() {
            System.out.println("Customer " + customerId + " intoduces itself to Teller " + myTeller.getTellerId() + ".");
            setTransaction();
        }

        public void leaveBank() {
            System.out.println("Customer " + customerId + " is now leaving the bank.");
            customersServed++;
            if (customersServed == 50) {
                System.out.println("Bank is now closed!");
                System.exit(0);
            }
        }

        public void run() {
            try {
                customers.acquire();
                entrance();
                findTeller();
                synchronized (monitor) {
                    introduction();
                    monitor.notify();
                    monitor.wait();
                    leaveBank();
                    customers.release();
                }
            } catch (Exception e) {
                System.out.println("Error in Customer Thread " + customerId + ": " + e);
            }
        }
    }

    //-----------------------------------------------------------------------------------------------------------------------------
    public static void main(String[] args) throws InterruptedException {
        Bank sim = new Bank();
        sim.runSimulation();
    }
}
