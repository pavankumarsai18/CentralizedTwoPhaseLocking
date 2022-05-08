package ParticipantSite;

import CentralSite.CentralSite;
import Interfaces.CentralSiteInterface;
import Interfaces.ParticipantSiteInterface;
import Transaction.Transaction;
import Transaction.Operation;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.*;



public class ParticipantSite implements ParticipantSiteInterface, Runnable
{
    public static final String TAG = ParticipantSite.class.getName();
    /**Console Output is set to true*/
    public static boolean consoleOutput = true;

    /**Boolean variables to store isBlocked, and abortTransaction*/
    private boolean isBlocked, abortTransaction;

    /**Variable to hold the site ID and centralPortNumber*/
    private int siteID, centralPortNumber;

    /**Variable to hold a stub to central site*/
    private CentralSiteInterface stubCentralSite;

    /**Transaction Manager*/
    private TransactionManager TM;

    /**Data Manager*/
    private DataManager DM;


    /**Site name*/
    private String siteName;

    /**Variable to store the number of committed transactions and number of aborted transactions*/
    private int numCommitted, numAborted;

    /**Variable to store the number of committed transactions and number of aborted transactions*/
    private Date startDate;
    private Registry registry;

    private ParticipantSiteInterface curSiteStub;

    /**Variable to store if site is new (has not yet committed a single transaction yet)*/
    private boolean isNew;

    /**Get the timestamp of the participant site.
     We declare it a static function as all the participant sites must use the same timeStamp function.
     */
    public static String timeStamp()
    {
        return "> ["+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").format(new Date())+"] ";
    }


    /**Main function for participant site*/
    public static void main(String[] args)
    {

        try
        {

            // Get the port number fo central site
            int centralSitePortNumber = CentralSite.getPortNumber();

            Scanner scanner = new Scanner(System.in);
            System.out.println("> Welcome!!!");
            System.out.println("> In Participant Site");

            String started = "";

            /**Let the user press start to start the site*/
            do
            {
                System.out.println("> Type 'start' to start site: ");
                started = scanner.nextLine();
            }while(!started.equals("start"));


            /**Get the file name from user*/
            System.out.println("> Enter the complete file path: ");
            String fileName = scanner.nextLine(); // Get the file which has all the transactions

            String TransactionFile = fileName;

            /**Create a new Participant site*/
            ParticipantSite ps = (new ParticipantSite(centralSitePortNumber, TransactionFile));

            /**Sleep for 1 second*/
            Thread.sleep(1000);

            /**Run the participant site*/
            ps.run();
        }
        catch(NumberFormatException e)
        {
            System.out.println(TAG + " Number not given correctly " + e.getMessage());
        }
        catch(NullPointerException e)
        {
            System.out.println(TAG + " Null pointer " + e.getMessage());
        }
        catch(Exception e)
        {
            System.out.println(TAG + " Exception " + e.getMessage());

        }

    }



    public ParticipantSite(int centralPortNumber, String TransactionFilePath)
    {

        this.isBlocked = true;
        this.abortTransaction = false;
        this.isNew = true;
        try
        {

            this.centralPortNumber = centralPortNumber;

            registry = LocateRegistry.getRegistry(centralPortNumber);
            stubCentralSite = (CentralSiteInterface) registry.lookup(CentralSite.rmiName);

            this.siteID = stubCentralSite.getNewSiteID();

            curSiteStub = (ParticipantSiteInterface) UnicastRemoteObject.exportObject(this, 0);
            siteName = "Participant["+this.siteID+"]";
            registry.bind(siteName, curSiteStub);


            print("Site registered under name "+siteName);

            printRegistry(centralPortNumber);
            this.TM = new TransactionManager(this.siteID);
            this.DM = new DataManager(this.siteID);

            if(this.siteID > 1 && isNew)
            {
                initDB();
                isNew = false;
            }

            System.out.println("DataBase initialized...");
            printDB();


            this.TM.loadTransactions(TransactionFilePath);
            print("Loaded transactions from " + TransactionFilePath);
            print("Number of Transactions is " + this.TM.getNumTransactions());
            // Register site in registry
            // Port number
            this.isBlocked = false;
            this.numCommitted = this.numAborted = 0;
            this.startDate = new Date();

            Timer printDBTimer = new Timer();

            TimerTask task = new TimerTask(){

                @Override
                public void run()
                {
                    try {
                        printDB();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            };

            printDBTimer.schedule(task, 5000, 5000);

        }

        catch (RemoteException e)
        {
            System.out.println(TAG + ": " );
            e.printStackTrace();
        }


        catch (NotBoundException e)
        {
             System.out.println("Not Bound");
             e.printStackTrace();
            System.out.println(TAG + ": lookup not working " + e.getMessage());
        }

        catch (Exception e) {

            System.out.println(TAG + ":  bind not working" + e.getMessage());
        }

    }

    @Override
    public void run()
    {
        while(true)
        {
            // We used synchornized as it provides mutually exclusive access
            try
            {
            synchronized (this)
            {
                Transaction transaction = TM.popFront();
                if (transaction != null)
                {
                    print("Transaction " + transaction.getTransactionID() + " is being processed ...");
                    for (Operation op: transaction.getOperations())
                    {
                        /**If operation is of type write or read*/
                        if (op.getType() == Operation.READ || op.getType() == Operation.WRITE)
                        {
                            /**Try to obtain lock*/
                            boolean lockGranted = stubCentralSite.requestLock(op);

                            //if lock was not granted then we block ourselves
                            if (lockGranted == false)
                            {
                                print("Lock was not granted");
                                blockSite();
                            }
                            else
                            {
                                print("Lock was granted");
                            }
                            /**If the transaction has been aborted then we break
                             * */
                            if (abortTransaction == true) {
                                numAborted++;
                                break;
                            }

                            /**else we execute the operation*/
                            transaction.executeOperation(op);
                        } /**If the operation is a caulation we simply execute it*/
                        else if (op.getType() == Operation.CALC) {
                            transaction.executeOperation(op);
                        }
                        /**If the transaction has been aborted then we and break
                         * */
                        if (abortTransaction == true)
                        {
                            break;
                        }
                    }
                    /**If the transaction has been aborted then we print message and break
                     * */
                    if (abortTransaction == true)
                    {

                        print("Transaction " + transaction.getTransactionID() + " has been aborted");
                        abortTransaction = false;
                    } /**If transaction was not aborted then we commit and release all locks held by transaction*/
                    else
                    {
                        transaction.commitTransaction();
                        stubCentralSite.releaseLock(transaction);
                        print("Transaction " + transaction.getTransactionID() + " has been successfully committed");
                        numCommitted++;
                        printDB();
                    }
                }
                else
                {
                    print("No items left to process ...");
                    Thread.sleep(3000);
                    printDB();
                }
            }}
            catch (RemoteException e)
            {

                print("Remote Exception " + e.getMessage());
            }
            catch (Exception e)
            {

                print("Remote Exception " + e.getMessage());
            }

        }

    }


    public void initDB() throws RemoteException
    {

        print("Initializing the database copying from others....");
        int otherSiteID = 1;
        if(this.siteID == otherSiteID) return;


        /**Copy data from participant site with ID 1 and write all its data to this site*/
        ParticipantSiteInterface stub = CentralSite.getStub(otherSiteID);
        if(stub != null)
        {
            // Write to this site
            this.writeAll(stub.readAll());
        }

        print("Database is as follows .... ");
        printDB();


    }

    @Override
    public void writeAll(Hashtable<String, Integer> values)
    {
        this.DM.writeALL(values);
    }

    public void print(String message)
    {
        if(consoleOutput)
        {
            System.out.println(ParticipantSite.timeStamp() + " " +TAG +": "  +message);

        }
    }
    public void setConsoleOutput(boolean consoleOutput){this.consoleOutput = consoleOutput;}



    @Override
    public void abortTransaction() throws RemoteException
    {
        abortTransaction = true;
        unBlockSite();
    }


    @Override
    public void blockSite() throws RemoteException
    {

        print("Site is being blocked ...");
        try
        {
            isBlocked = true;
            print("Site" + this.siteID + " is blocked");
            while(isBlocked)
            {
                print("Site" + this.siteID + " is waiting ...");
                Thread.sleep(1000);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void unBlockSite() throws RemoteException
    {
        print("Site is being unblocked ...");
        isBlocked = false;
        print("Site is unblocked ...");
    }

    @Override
    public void printInfo() throws RemoteException
    {
        if(consoleOutput)
        {
            Date curTime = new Date();
            long timeElapsed = curTime.getTime() - this.startDate.getTime();

            System.out.println("Info for the site ...");
            System.out.println("Working on port " + centralPortNumber);
            System.out.println("Number of transactions committed = "+numCommitted);
            System.out.println("Number of transactions aborted = "+ numAborted);
            System.out.println("Throughput = "+ (numCommitted*1.0)/timeElapsed);

        }
    }




    /*Print the registry*/
    public void printRegistry(int portNumber)
    {
        try
        {
            Registry registry = LocateRegistry.getRegistry(portNumber);

            String[] items = registry.list();
            StringBuilder result = new StringBuilder();
            result.append("Registry = [");
            for(int i = 0; i < items.length; i++)
            {
                result.append(items[i]);
                if(i != items.length - 1)
                {
                    result.append(", ");
                }
            }
            result.append("]");
            print(result.toString());
            return;

        }
        catch (AccessException e)
        {
            print("AccessException " + e.getMessage());
        } catch (RemoteException e)
        {
            print("Remote Exception "+e.getMessage());
        }
    }
    @Override
    public void printDB() throws RemoteException
    {
        print("\n"+this.DM.printDB());
        return;
    }

    @Override
    public void write(String varName, Integer value)
    {
        print("In participant write");
        this.DM.write(varName, value);
        return;
    }

    @Override
    public int read(String varName)
    {
        print("In participant read");
        return this.DM.read(varName);
    }


    @Override
    public Hashtable<String, Integer> readAll()
    {
        return this.DM.readAll();
    }

    @Override
    public void setIsNew(boolean b)
    {
        this.isNew = b;
    }

    @Override
    public boolean isNew()
    {
        return isNew;
    }
    /**Get the port number of central site*/
    @Override
    public int getCentralPortNumber(){return this.centralPortNumber;}



}
