package ParticipantSite;

import Transaction.Transaction;
import Transaction.Operation;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransactionManager
{
    public static final String TAG = TransactionManager.class.getName();

    private Queue<Transaction> Q; // queue to hold the transactions
    private int siteID;
    private int numTransactions = 1;
    private int transactionOffset = 1000;

    public TransactionManager(int sID)
    {
        this.siteID = sID;
        this.Q = new LinkedList<Transaction>();
    }

    public int getNextTrasactionID()
    {
        // Can change this
        int nextTransactionID = (siteID)*transactionOffset + (numTransactions++);
        return nextTransactionID;
    }

    public int getSiteID(){return siteID;}
    public void setSiteID(int siteID){this.siteID = siteID;}

    public int getNumTransactions(){return this.Q.size();}

    public void setNumTransactions(int numTransactions){this.numTransactions = numTransactions;}

    public int getTransactionOffset(){return transactionOffset;}

    public Queue<Transaction> getQ(){return Q;}
    public void setQ(Queue<Transaction> Q){this.Q = Q;}

    public ArrayList<String> parseFile(String filePath) throws IOException
    {
        ArrayList<String> lines = new ArrayList<String>();

        try
        {
            BufferedReader fileReader = new BufferedReader(new FileReader(filePath));

            String line = "";
            while((line = fileReader.readLine()) != null)
            {
                // remove trailing spaces
                line.trim();
                if(line.length() > 0)
                {
                    lines.add(line);
                }
            }
            fileReader.close();

        }
        catch(IOException exception)
        {
            System.out.println(TAG + ": "+ exception.getMessage());
        }

        return lines;
    }

    public void loadTransactions(String filePath) throws Exception
    {

        Queue<Transaction> q = loadTransactionsFromFile(filePath);
        if(q.size() > 0)
        {
            this.Q = q;
        }
    }

    public Transaction popFront()
    {
        if(Q != null && Q.size() > 0)
        {
            // We always remove from the head
            return Q.remove();
        }

        return null;
    }


    public Queue<Transaction> loadTransactionsFromFile(String filePath) throws Exception
    {
        ArrayList<String> lines = parseFile(filePath);
        Queue<Transaction> Queue = new LinkedList<Transaction>();

        Transaction curTransaction = null;
        for(int i = 0; i < lines.size(); i++)
        {
            String operation = lines.get(i);
            operation.trim();
            operation = operation.toLowerCase();

            if(operation.contains("begin transaction"))
            {
                if(curTransaction != null)
                {
                    Queue.add(curTransaction);
                }
                curTransaction = new Transaction(getNextTrasactionID());
            }
            else
            {
                char operationType = operation.charAt(0);
                if(operationType == 'r' || operationType == 'w')
                {

                    // This is a read operation
                    // The operation will look as follows "read(variable)" so we can math it with .* ( .* )
                    // Or it will look as "write(variable)"
                    Pattern p = Pattern.compile(".*\\((.*)\\).*");
                    Matcher m = p.matcher(operation);
                    // This is a valid read operation
                    if(m.matches())
                    {
                        // Now m will have [read/write, variableName], sine we would like to get the variable name
                        // It will be in the index 1

                        String variableName = m.group(1);

                        // Now we create a operation with transtion ID as curTransaction
                        // Set the type to READ operation
                        // Set the variable name to the one obtained

                        Operation op = null;
                        if(operationType == 'r')
                            op = new Operation(curTransaction.getTransactionID(), Operation.READ, variableName);
                        else
                            op = new Operation(curTransaction.getTransactionID(), Operation.WRITE, variableName);
                        curTransaction.addOperation(op);

                    }
                    else{
                        throw new Exception("File has not been formated properly, operation "+operation+" has wrong format");
                    }
                }
                else if(operationType == 'c')
                {
                    // This is a calculation operation CX = X+1
                    // The operation will look as follows "Cvar1=var2+var3" so we can math it with .* ( .* )
                    Pattern p = Pattern.compile("c(.*)\\=(.*)(\\+|\\-|\\*|\\/|\\^|\\%)(.*)");
                    Matcher m = p.matcher(operation);
                    // This is a valid read operation
                    if(m.matches())
                    {
                        // Now m will have [c, var1, var2, operator, var3], sine we would like to get the variable name
                        // It will be in the index 1
                        String var1 = m.group(1);
                        String var2 = m.group(2);
                        String operator = m.group(3);
                        String var3 = m.group(4);
                        // Now we create a operation with transtion ID as curTransaction
                        // Set the type to READ operation
                        // Set the variable name to the one obtained


                        Operation op = new Operation
                                (curTransaction.getTransactionID(), Operation.CALC, var1, var2, var3, operator);


                        curTransaction.addOperation(op);


                    }
                    else
                    {
                        Pattern newP = Pattern.compile("c(.*)\\=(.*)");
                        Matcher newM = newP.matcher(operation);
                        if(newM.matches())
                        {
                            String var1 = newM.group(1);
                            String var2 = newM.group(2);
                            Operation op = new Operation(curTransaction.getTransactionID(),
                                    Operation.CALC, var1, var2, "0", "+");
                            curTransaction.addOperation(op);

                        }
                        else{
                            throw new Exception("File has not been formated properly, operation "+operation+" has wrong format");
                        }
                    }

                }
                else if(operation.contains("end transaction"))
                {
                    if(curTransaction != null)
                    {
                        Queue.add(curTransaction);
                        curTransaction = null;
                    }
                }
                else
                {
                    throw new Exception
                            ("File has not been formatted properly. The operation " + operation +" is not supported in this DDBMS");
                }
            }
        }
        if(curTransaction != null)
        {
            Queue.add(curTransaction);
            curTransaction = null;
        }

        return Queue;
    }

}
