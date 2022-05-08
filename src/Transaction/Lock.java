package Transaction;
import Transaction.Operation;
import Transaction.Transaction;

public class Lock
{
    public static final String TAG = Lock.class.getName();

    public static final int READ = 1;
    public static final int WRITE = 2;
    public static final int READ_AND_WRITE = 3;

    private int type, transactionID;
    private String key;

    public Lock(int type, String key, int transactionID)
    {
        this.type = type;
        this.key = key;
        this.transactionID = transactionID;
    }

    public int getType(){return this.type;}
    public int getTransactionID(){return this.transactionID;}
    public String getKey(){return this.key;};

    public void setType(int type){this.type = type;}
    public void setTransactionID(int transactionID){this.transactionID = transactionID;}
    public void setKey(String key){this.key = key;}


    public void upgradeLock(int newType)
    {
        // We need to upgrade locks if when the old trans was reading and the new trans wants to write
        // Or the old trans was writing and new transaction wants to READ, we upgrade it to READ_AND_WRITE LOCK
        int oldType = this.type;
        if((oldType == Lock.READ && newType == Lock.WRITE) || (oldType == Lock.WRITE && newType == Lock.READ))
        {
            this.type = Lock.READ_AND_WRITE;
        }
        return;
    }

    public String toString()
    {
        StringBuilder result = new StringBuilder();
        result.append("[");

        if(this.type == Lock.READ)
        {
            result.append("READ ("+key+"),");
        }
        else if(this.type == Lock.WRITE)
        {
            result.append("WRITE ("+key+"),");

        }
        else if(this.type == Lock.READ_AND_WRITE)
        {
            result.append("READ AND WRITE ("+key+"),");
        }

        result.append("transaction ID: " +
                transactionID + ", done by site " +
                Transaction.getSiteID(transactionID) + "]");
        return result.toString();
    }
}
