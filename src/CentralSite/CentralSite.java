package CentralSite;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.*;
import DataStruct.pair;
import Interfaces.*;
import ParticipantSite.ParticipantSite;
import Transaction.*;


public class CentralSite implements CentralSiteInterface
{
    public static final String TAG = CentralSite.class.getName();
    private static int numSites;

    private static boolean consoleOutput;
    private static final int portNumber = 45;
    private int numDeadlocks = 0;
    private long deadLockTimeout;
    private int numVars;
    private LockTable LM;
    public static String rmiName = "CentralSite";
    public static Registry registry;
    public static CentralSiteInterface thisStub;


    /**Main function that creates a central site object*/
    public static void main(String [] args)
    {

        try
        {


            Scanner scanner = new Scanner(System.in);
            System.out.println("> Welcome!!!");
            System.out.println("> In Central Site");
            int portNumber;
            long deadLockTimeOut;
            boolean consoleOutput;
            boolean invalidInput = true;

            System.out.println("> Enter the timeout for deadlocks (in ms): ");

            deadLockTimeOut = Long.parseLong(scanner.nextLine());

            System.out.println("> Deadlock is checked every: " + deadLockTimeOut + "ms");

            consoleOutput = true; // We output to the console by default
            System.out.println("> Enter if you wish to output to console (true/false): ");
            consoleOutput = Boolean.parseBoolean(scanner.nextLine());



            CentralSite S = new CentralSite(deadLockTimeOut);
            S.setConsoleOutput(consoleOutput);

        }
        catch(NumberFormatException e)
        {
            print(" Number not given correctly " + e.getMessage());
        }
        catch(NullPointerException e)
        {
            print(" Null pointer " + e.getMessage());
        }
        catch(Exception e)
        {
            print(" Exception " + e.getMessage());

        }

    }
    public CentralSite(long timeOut)
    {

        try
        {
            this.numDeadlocks = 0;
            this.numSites = 0;
            this.deadLockTimeout = timeOut;
            print("Initializing central site ...\n");

            registry = LocateRegistry.createRegistry(portNumber);
            thisStub =
                    (CentralSiteInterface) UnicastRemoteObject.exportObject(this, 0);
            registry.bind(CentralSite.rmiName, thisStub);

            this.LM = new LockTable();
            print("Lock Manager Created ...");

            printRegistry(portNumber);


            Timer checkDeadLocksTimer = new Timer();

            TimerTask task = new TimerTask(){

                @Override
                public void run()
                {
                    printRegistry(portNumber);
                    checkForDeadlocks();
                    LM.printAll();
                }
            };

            checkDeadLocksTimer.schedule(task, deadLockTimeout, deadLockTimeout);

        } catch (RemoteException e) {
            print("Remote Exception "+ e.getMessage());
        } catch (AlreadyBoundException e) {
            print("Already Bound Exception " + e.getMessage());
        }

    }
    /**Checks for deadlocks in the waitforgraph*/
    public synchronized void checkForDeadlocks()
    {
        print("Checking Deadlocks ...");
        pair<Integer, ArrayList<Integer>> result = LM.removeDeadlocks();
        if(result != null)
        {
            int abortTID = result.getFirst().intValue();
            ArrayList<Integer> siteIDs = result.getSecond();
            numDeadlocks++;
            print("Deadlock detected ....");
            print("Aborting transaction " + abortTID);

            int sID = Transaction.getSiteID(abortTID);
            try {
                ParticipantSiteInterface stub = getStub(sID);
                if(stub != null)
                {
                    stub.abortTransaction();
                    print("site "+ sID + " is unBlocked");

                }
                else
                {
                    print("site "+ sID + " is either down or does not exist");
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            for(Integer siteID: siteIDs)
            {
                print("Deadlock Site ID " + siteID);
                try
                {
                    ParticipantSiteInterface stub = getStub(siteID.intValue());
                    if(stub != null)
                    {
                        stub.unBlockSite();
                        print("site "+ siteID + " is unBlocked");
                    }
                    else
                    {
                        print("site "+ siteID + " is either down or does not exist");
                    }
                } catch (RemoteException e) {
                   print("Remote Exception while removing deadlocks " + e.getMessage());
                }
            }
            print("Deadlocks removed = " + numDeadlocks + "\n");
            return;
        }

        print("No deadlocks found");
        print("Deadlocks removed = " + numDeadlocks + "\n");


    }

    /** Releases locks for a transaction*/
    public synchronized void releaseLock(Transaction transaction)
    {
        print("Releasing locks for transaction " + transaction.getTransactionID());
        try
        {
            /**Release all locks and get the site IDS*/
            ArrayList<Integer> siteIDs = LM.releaseAllLocks(transaction);

            /**loop through the sites and unblock them and print the information*/
            for(Integer siteID: siteIDs)
            {
                ParticipantSiteInterface stub = getStub(siteID);

                /*If the site does not exist then print*/
                if(stub == null)
                {
                    print("Error !!! site not available with id " + siteID.toString());
                    continue;
                }

                /*unblock the site*/
                stub.unBlockSite();
                print("site " + siteID.toString() + " is now unBlocked");
            }


        } catch (RemoteException e) {
            print("Remote Exception in release Lock: " + e.getMessage());
        }
        catch(Exception e) {
            print(" Exception in release Lock: " + e.getMessage());
        }
    }

    public synchronized boolean requestLock(Operation op) throws RemoteException
    {

        print("Lock requested by " + op.toString());
        try
        {
            boolean obtainedLock = LM.grantLockToOperation(op);
            if(obtainedLock)
            {
                print("Operation from transaction " + op.getTransactionID() + " has obtained Lock");
            }
            else
            {
                print("Operation from transaction " + op.getTransactionID() + " has not obtained Lock");

            }
            return obtainedLock;
        }
        catch(NullPointerException e)
        {
            print(" Null pointer " + e.getMessage());
        }
        catch(Exception e)
        {
            print(" Exception " + e.getMessage());

        }
        return false;
    }



    /**Returns the stub for site given siteID*/
    public static ParticipantSiteInterface getStub(Integer siteID)
    {
        try
        {

            Registry registry = LocateRegistry.getRegistry(portNumber);
            String participantName = "Participant["+siteID.intValue()+"]";

            ParticipantSiteInterface stub = (ParticipantSiteInterface) registry.lookup(participantName);
            return stub;
        } catch (RemoteException e) {
            print("Remote Exception " + e.getMessage());
        } catch (NotBoundException e) {
            print("Not Bound Exception " + e.getMessage());
        }

        return null;
    }

    public void printMsgFor(String message)
    {
        if(consoleOutput)
        {
            System.out.println(TAG + " : " + message);
        }
    }


    public static void print(String message)
    {
        if(consoleOutput)
        {
            System.out.println(timeStamp() + TAG + " : " + message +"\n");
        }

    }
    public static void printRegistry(int portNumber)
    {
        try
        {
            Registry registry = LocateRegistry.getRegistry(portNumber);
            String[] items = registry.list();
            StringBuilder result = new StringBuilder();
            result.append("Registry = {");
            for(int i = 0; i < items.length; i++)
            {
                result.append(items[i]);
                if(i != items.length - 1)
                {
                    result.append(",");
                }
            }
            result.append("}");
            print(result.toString());
            return;

        }
        catch (AccessException e) {
            print("AcessException " + e.getMessage());
        } catch (RemoteException e)
        {
            print("Remote Exception "+e.getMessage());
        }
    }


    public void printConsole(String Message)
    {
        if(canPrint())
        {
            System.out.println(timeStamp() + TAG + ": " + Message);
        }
    }



    // Get the timestamp of the central site.
    // We declare it a static function as all the central sites must  use the same timeStamp function.
    public static String timeStamp()
    {
        return "> [" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").format(new Date()) + "] ";
    }

    public void setConsoleOutput(boolean output)
    {
        consoleOutput = output;
        return;
    }

    public static CentralSiteInterface getCentralStub(){
        return thisStub;
    }

    public static Registry getRegistry()
    {
        return registry;
    }
    public boolean canPrint()
    {
        return consoleOutput;
    }

    public int getNumDeadlocks()
    {
        return numDeadlocks;
    }

    public static int getPortNumber()
    {
        return portNumber;
    }

    public long getTimeOut()
    {
        return deadLockTimeout;
    }

    public static int getNumSites()
    {
        print("Number of sites = " + numSites);
        return numSites;
    }


    public int getNewSiteID() throws RemoteException
    {
        numSites++;
        return numSites;
    }



}
