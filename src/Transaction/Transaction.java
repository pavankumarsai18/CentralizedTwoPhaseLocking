package Transaction;

import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import CentralSite.CentralSite;
import Interfaces.ParticipantSiteInterface;
import ParticipantSite.*;

public class Transaction implements Serializable
{
    public static final String TAG = Transaction.class.getName();
    public static final long serialVersionID = 3L;

    public static final int sleepTime = 50;    // Between two consequetive transactions there will be a delay
    // If this delay was not present it will not be easy to serialize
    public static final int Offset = 1000; // Each site will be offset by this amount, we use this to get the
    private int transactionID;

    // A transaction is a consequtive operations we use operations to
    private ArrayList<Operation> operations;

    // Since we need to simulate execution of multiple operations
    // We can simply Write and Commit the end value (view value) to do this we use hashtables
    // We map the key (variable x, y, z ...) to the value (1, 2, 3, ..) in this map
    Hashtable<String, Integer> writeMap;
    Hashtable<String, Integer> commitMap;

    public Transaction(int transactionID)
    {
        this.transactionID = transactionID;
        this.writeMap  = new Hashtable<String, Integer>();
        this.commitMap = new Hashtable<String, Integer>();
        this.operations = new ArrayList<Operation>();
    }
    public static String timeStamp()
    {
        return "["+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").format(new Date())+"] ";
    }

    public String commitMap_toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("commit Map: [");
        for(String var: commitMap.keySet())
        {
            builder.append(var + ":" + commitMap.get(var));
            builder.append(",");
        }
        builder.append("]");
        return builder.toString();
    }

    public String writeMap_toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Write Map: [");
        for(String var: writeMap.keySet())
        {
            builder.append(var + ":" + writeMap.get(var));
            builder.append(",");
        }
        builder.append("]");
        return builder.toString();
    }

    public void print(String message)
    {
        System.out.println(timeStamp() + TAG +" "+ message);
    }

    public int getTransactionID(){return this.transactionID;}
    public void setTransactionID(int TransactionID){this.transactionID = TransactionID;}

    public ArrayList<Operation> getOperations(){return this.operations;}
    public void setOperations(ArrayList<Operation> ops){this.operations = ops;}

    public Hashtable<String, Integer> getWriteMap(){return this.writeMap;}
    public Hashtable<String, Integer> getCommitMap(){return this.commitMap;}

    public void setWriteMap(Hashtable<String, Integer> writeMap){this.writeMap = writeMap;}
    public void setCommitMap(Hashtable<String, Integer> commitMap){this.commitMap = commitMap;}

    public void addOperation(Operation op)
    {
        op.setTransactionID(this.transactionID);
        operations.add(op);
    }

    // commit
    public void executeOps() throws Exception
    {
        for(int i = 0; i < this.operations.size(); i++)
        {
            executeOperation(this.operations.get(i));
        }
    }

    public void executeOperation(Operation op) throws Exception
    {
        print("Executing Operation ... " + op.toString());

        try
        {
            Thread.sleep(Transaction.sleepTime);
            if (op.getType() == Operation.READ)
            {
                executeRead(op);
            } else if (op.getType() == Operation.CALC) {
                executeCalc(op);
            } else if (op.getType() == Operation.WRITE) {
                executeWrite(op);
            }
        }
        catch(InterruptedException exception)
        {
            System.out.println(TAG + ": "+ exception.getMessage());
        }
    }

    public void executeWrite(Operation op) throws Exception
    {
        if(writeMap.containsKey(op.getKey()))
        {
            commitMap.put(op.getKey(), writeMap.get(op.getKey()));
            return;
        }
        throw new Exception("Unavailable Write");
    }

    public void executeRead(Operation op) throws RemoteException {


        ParticipantSiteInterface stub = CentralSite.getStub(getSiteID(transactionID));
        if(stub != null)
        {
            int value = stub.read(op.getKey());
            commitMap.put(op.getKey(), value);

        }

        return;
    }

    public int getValue(String operand)
    {
        if(Transaction.isValidNumber(operand))
        {
            return Integer.parseInt(operand);
        }
        if(commitMap.containsKey(operand))
        {
            return commitMap.get(operand);
        }
        return 0;
    }

    public void executeCalc(Operation op) throws Exception
    {

        String operand1 = op.getOperand1();
        String operand2 = op.getOperand2();
        String operator = op.getOperator();



        int operand1Val = getValue(operand1);
        int operand2Val = getValue(operand2);

        int result = 0;
        if(operator.equals("+"))
        {
            result = operand1Val + operand2Val;
        }
        else if(operator.equals("-"))
        {
            result = operand1Val - operand2Val;
        }
        else if(operator.equals("*"))
        {
            result = operand1Val * operand2Val;
        }
        else if(operator.equals("/"))
        {
            result = operand1Val/operand2Val;
        }
        else if(operator.equals("%"))
        {
            result = operand1Val%operand2Val;
        }
        else if(operator.equals("^"))
        {
            result = (int) Math.pow(operand1Val*1.0, operand2Val*1.0);
        }
        else
        {
            throw new Exception("Operator not defined");
        }

        writeMap.put(op.getKey(), result);

    }



    public static int getSiteID(int transactionID)
    {

        // String ID and get the first few chars and convert them to int to get site ID
        // Need to change
        return transactionID/Offset;
    }

    public static boolean isValidNumber(String number)
    {
        // d = [0-9] // d+ = [0-9]+ which is a valid number
        return number.matches("\\d+");
    }


    public String toString()
    {
        StringBuilder result = new StringBuilder();
        result.append("[");
        for(int i = 0; i < this.operations.size(); i++)
        {

            result.append("[" + operations.get(i).toString() + "],");
        }
        result.append("transaction ID: "+ Integer.toString(transactionID) +"]");
        return result.toString();
    }

    public void commitTransaction() throws RemoteException, NotBoundException
    {
        print("Committing transaction ....");
        int transSiteID = getSiteID(transactionID);
        ParticipantSiteInterface stub = CentralSite.getStub(transSiteID);
        if(stub.isNew())
        {
            stub.writeAll( CentralSite.getStub(1).readAll());
            stub.setIsNew(false);
        }


        for(String key: commitMap.keySet())
        {

            int value = commitMap.get(key).intValue();
            stub.write(key, value);
        }

        Registry registry = LocateRegistry.getRegistry(stub.getCentralPortNumber());
        String[] sites = registry.list();
        ArrayList<String> newSites = new ArrayList<String>();
        for(String key: commitMap.keySet())
        {
            int value = commitMap.get(key).intValue();

            for(String site: sites)
            {
                if(site.equals(CentralSite.rmiName) || site.equals("Participant[" + transSiteID + "]"))
                {
                    continue;
                }
                else
                {
                    print("Site name " + site);

                     ParticipantSiteInterface participantStub = (ParticipantSiteInterface) registry.lookup(site);
                     if(participantStub != null)
                     {

                         if(participantStub.isNew())
                         {
                             newSites.add(site);
                         }
                         else
                         {
                             participantStub.printDB();
                             print("Writing " + key + " " + value + " to site " + site);
                             participantStub.write(key, value);
                             print("After writing ...");
                             participantStub.printDB();
                         }
                     }
                }
            }


            for(String site: newSites)
            {
                ParticipantSiteInterface participantStub = (ParticipantSiteInterface) registry.lookup(site);
                if(participantStub != null)
                {
                    stub.writeAll( CentralSite.getStub(1).readAll());
                    stub.setIsNew(false);
                }
                    for(String k: commitMap.keySet())
                    {

                        int val = commitMap.get(k).intValue();
                        stub.write(k, val);
                    }

            }
        }

    }

}
