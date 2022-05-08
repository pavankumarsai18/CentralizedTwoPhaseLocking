package CentralSite;

import Transaction.Lock;
import Transaction.Operation;
import Transaction.Transaction;
import DataStruct.pair;
import WaitForGraph.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class LockTable {
    public static final String TAG = LockTable.class.getName();

    // Table holds all the locks
    // Maps the variable with to lock
    private Hashtable<String, ArrayList<Lock>> lockTable;

    // Table holds the queue for each variable
    private Hashtable<String, Queue<Operation>> variableQueue;

    // wait for graph
    private WaitForGraph wfg;

    public LockTable()
    {
        this.lockTable = new Hashtable<String, ArrayList<Lock>>();
        this.variableQueue = new Hashtable<String, Queue<Operation>>();
    }


    public void printTable()
    {
        for(String variable: this.lockTable.keySet())
        {
            StringBuilder builder = new StringBuilder();
            builder.append(variable + ": [");

            for(Lock locks: this.lockTable.get(variable))
            {
                builder.append(locks.toString() + " ");
            }

            builder.append("]");
            System.out.println(builder.toString());
        }
    }

    public void printQueue()
    {
        for(String variable: this.variableQueue.keySet())
        {
            StringBuilder builder = new StringBuilder();
            builder.append(variable + ": [");

            for(Operation op: this.variableQueue.get(variable))
            {
                builder.append(op.toString() + " ");
            }

            builder.append("]");
            System.out.println(builder.toString());
        }
    }


    public void printAll()
    {
        printMsg("Lock Table: \n");
        printTable();
        printMsg("Queue: \n");
        printQueue();
        printMsg("wait for graph: \n");
        printMsg(wfg.toString());
        printMsg("\n\n");

    }


    /**
     * Returns the timeStamp*/
    public static String timeStamp() {
        return "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").format(new Date()) + "] ";
    }

    /** Returns the site IDs from which the transaction has released locks
    * Aborts a given transaction by removing it
    *  from the queue and releasing all locks from lockTable
    *  */
    public ArrayList<Integer> abortTransaction(Transaction transaction)
    {
        try
        {
            printAll();
            removeOperationsOfTransaction(transaction);
            ArrayList<Integer> siteIDs = releaseAllLocks(transaction);
            return siteIDs;
        }
        catch(Exception exception)
        {
            printMsg("Exception while aborting transaction "+ transaction.getTransactionID());
            return new ArrayList<Integer>();
        }
    }

    /** Given a transaction we remove
    * all the operations in the VariableQueue Table
    * */
    public void removeOperationsOfTransaction(Transaction transaction)
    {
        for(String variable: variableQueue.keySet())
        {
            Set<Integer> removeIndices = new HashSet<Integer>();
            ArrayList<Operation> ops = new ArrayList<Operation>();

            int i = 0;
            for(Operation op: variableQueue.get(variable))
            {
                if(op.getTransactionID() == transaction.getTransactionID())
                {
                    removeIndices.add(i);
                }
                ops.add(op);
                i++;
            }


            Queue<Operation> undeletedOps = new LinkedList<Operation>();
            for(i = 0; i < ops.size(); i++)
            {
                if(removeIndices.contains(i))
                {
                    continue;
                }
                else
                {
                    undeletedOps.add(ops.get(i));
                }
            }

            variableQueue.put(variable, undeletedOps);
        }
    }

    /**Returns the transaction ID and the sites that were locked*/
    public pair<Integer, ArrayList<Integer>> removeDeadlocks()
    {
        // create a wait for graph
        // check for cycles
        // abort transaction
        printMsg("In remove Deadlocks ....");
        wfg = new WaitForGraph();

        for(String variable: lockTable.keySet())
        {
            Queue<Operation> varOperations = variableQueue.get(variable);
            if(varOperations != null && varOperations.size() > 0)
            {
                // lock represts the lock of the transactions that have obtained the lock int the lock table
                for(Lock lock: lockTable.get(variable))
                {
                    // op represents the operations that are in the queue that need to be processed
                    for(Operation op: varOperations)
                    {
                        if(lock.getTransactionID() != op.getTransactionID())
                        {
                            wfg.addDependency(lock.getTransactionID(), op.getTransactionID());
                        }
                    }
                }
            }
        }


        pair<Vertex, Vertex> v = wfg.getCycle();
        if(v != null)
        {
            int abortingTransactionID = v.getSecond().getValue();
            System.out.println("Deadlock exists from transaction " +  v.getFirst().getValue() + " to " + v.getSecond().getValue());

            ArrayList<Integer> siteIDs = abortTransaction(new Transaction(abortingTransactionID));
            return new pair<Integer, ArrayList<Integer>>(abortingTransactionID, siteIDs);
        }

        return null;
    }



    public boolean isCompatible(Lock newLock)
    {
        // Get the variable name
        String variableName = newLock.getKey();

        // Get all the locks that are set with the coressponding variable (READ, WRITE, READ_AND_WRITE)
        ArrayList<Lock> locks = lockTable.get(variableName);

        // If no locks are set then the current lock is compataible so we return true
        if (locks == null) {
            return true;
        }

        // If the lock type is a read or a write we need to check if it can be granted
        if (newLock.getType() == Lock.READ) {
            for (int i = 0; i < locks.size(); i++) {
                // If any transaction is writing then we cannot grant a lock
                // as allowing such a transaction will lead to dirty reads
                // So we return false
                if (locks.get(i).getTransactionID() != newLock.getTransactionID())
                {
                    if (locks.get(i).getType() == Lock.READ)
                    {
                        continue;
                    } else {
                        return false;
                    }
                }
            }
            return true;
        } else if (newLock.getType() == Lock.WRITE) {
            // Only one transaction can hold a write lock so we iterate over all locks
            // And check if there is any transaction holding a write lock
            for (int i = 0; i < locks.size(); i++) {
                if (locks.get(i).getTransactionID() != newLock.getTransactionID()) {
                    return false;
                }
            }
            return true;
        } else {
            printMsg("ERROR !!! lock type not defined");
            return false;
        }

    }


    public boolean addLock(Lock lock)
    {
        String variableName = lock.getKey();

        // If the lock table does not contain the variable key then we create
        // a new ArrayList and we append the lock
        if (lockTable.containsKey(variableName) == false)
        {
            lockTable.put(variableName, new ArrayList<Lock>());
        }

        boolean result =  lockTable.get(variableName).add(lock);
        return result;
    }

    /*
    * Removes lock from locktable
    * Return true if successfully removed false otherwise
    * */
    public boolean removeLock(Lock lock) {
        String varName = lock.getKey();
        ArrayList<Lock> locksVar = lockTable.get(varName);

        for (Lock curLock : locksVar) {
            if (curLock.getTransactionID() == lock.getTransactionID()) {
                lockTable.get(varName).remove(curLock);

                if (lockTable.get(varName).isEmpty()) {
                    lockTable.remove(varName);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Adds the operation to the Queue for corresponding variable
     * */

    public boolean addOperationToQueue(Operation op)
    {

        String variableName = op.getKey();
        // If the variable Queue does not contain the variable key then we create a new
        // LinkedList and append the operation
        if (variableQueue.containsKey(variableName) == false)
        {
            variableQueue.put(variableName, new LinkedList<Operation>());
        }
        boolean result =  variableQueue.get(variableName).add(op);

        if(result)
        {
            printMsg("Added Operation to Queue " + op.toString());
        }
        else
        {
            printMsg("Couldn't add Operation to Queue " + op.toString());

        }
        return result;

    }

    /**
     * Removes the topmost operation from the Queue for corresponding variable
     * */
    public Operation removeOperationFromQueue(String variable) {
        if (variableQueue.containsKey(variable)) {
            Queue<Operation> Q = variableQueue.get(variable);
            if (Q.size() > 0) {
                /*
                 * Check if the top transaction is compatible with the lock table
                 * If it is then pop it, and return the popped operation
                 * Else we keep it in the queue and return null
                 * */

                if (isCompatible(Q.peek().getLock()) == true) {
                    Operation removedOp = Q.remove();
                    // Delete the variable from the Q if it is empty
                    if (Q.isEmpty()) {
                        variableQueue.remove(variable);
                    }
                    return removedOp;
                } else {
                    return null;
                }
            } else {

                return null;
            }
        }
        return null;
    }


    /**
     * Release all locks corresponding to a transaction from lockTable
     * Get all siteIDs
     * */

    public ArrayList<Integer> releaseAllLocks(Transaction transaction) {
        ArrayList<Integer> siteIDs = new ArrayList<Integer>();
        ArrayList<Lock> allLocks = getAllLocks(transaction);

        for (Lock lock : allLocks) {
            removeLock(lock);

            Operation nextOp = removeOperationFromQueue(lock.getKey());
            if (nextOp != null) {
                Lock nextLock = nextOp.getLock();
                if (isCompatible(nextLock)) {
                    Lock prevLock = getLock(lock.getKey(), lock.getTransactionID());
                    // If the prevLock was not in the lockTable then we insert it to LockTable
                    // Granting access to variable
                    // Else if the prevLock was in the lockTable we upgrade the lock type to the nextLock type
                    if (prevLock == null) {
                        addLock(nextLock);
                    } else {
                        prevLock.upgradeLock(nextLock.getType());
                    }

                    // Now that we obtained a lock we add it to siteIDs
                    int siteID = Transaction.getSiteID(nextOp.getTransactionID());
                    siteIDs.add(siteID);
                }
            }

        }

        return siteIDs;

    }


    /**
     * Returns true the lock was granted to an operation false otherwise
     * If the lock is compatible we add it to lockTable
     * Else we queue it to queueTable
     * */
    public boolean grantLockToOperation(Operation op)
    {
        Lock lock = op.getLock();

        if(isCompatible(lock))
        {
            printMsg("Lock is compatible");

            Lock prevLock = getLock(op.getKey(), op.getTransactionID());
            if(prevLock != null)
            {
                prevLock.upgradeLock(lock.getType());
            }
            else
            {
                addLock(lock);
            }
            return true;
        }

        addOperationToQueue(op);

        return false;
    }



    /**
    * Returns an ArrayList of locks with the same transaction ID
    * from the lockTable
    * */

    public ArrayList<Lock> getAllLocks(Transaction transaction)
    {
        int transactionID = transaction.getTransactionID();
        ArrayList<Lock> result = new ArrayList<Lock>();
        for(String variable: lockTable.keySet())
        {
            for(Lock lock: lockTable.get(variable))
            {
                if(lock.getTransactionID() == transactionID)
                {
                    result.add(lock);
                }
            }
        }
        return result;
    }

    /**
    * Returns the first lock with the same transactionId and variable name
    * From the lockTable
    * */

    public Lock getLock(String variable, int transactionID)
    {

        ArrayList<Lock> locks = lockTable.get(variable);
        if(locks == null)
            return null;
        for(Lock lock: locks)
        {
            if(lock.getTransactionID() == transactionID)
            {
                return lock;
            }
        }
        return null;
    }

    public void setLockTable(Hashtable<String, ArrayList<Lock>> L) {
        this.lockTable = L;
    }

    public void setVariableQueue(Hashtable<String, Queue<Operation>> Q) {
        this.variableQueue = Q;
    }

    public Hashtable<String, ArrayList<Lock>> getLockTable() {
        return this.lockTable;
    }

    public Hashtable<String, Queue<Operation>> getVariableQueue()
    {
        return this.variableQueue;
    }

    public void printMsg(String message)
    {
        System.out.println(timeStamp() + TAG + " : " + message);
    }


}
