package Transaction;
import java.io.Serializable;


public class Operation implements Serializable
{
    public static final String TAG = Operation.class.getName();
    private static final long serialVersionID = 1L;
    public static final int READ = 1;
    public static final int WRITE = 2;
    public static final int CALC = 3;

    private int transactionID;
    private int type;
    private String key;
    private String operand1, operand2, operator; // operator1 operator operator2

    public Operation(int transactionID, int type)
    {
        this.transactionID = transactionID;
        this.type = type;
    }

    public Operation(int transactionID, int type, String key)
    {
        this.transactionID = transactionID;
        this.type = type;
        this.key = key;
    }

    public Operation(int transactionID, int type, String key, String operand1, String operand2, String operator)
    {
        this.transactionID = transactionID;
        this.type = type;
        this.key = key;
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.operator = operator;
    }

    public String getKey() {return this.key;}
    public void setKey(String key){ this.key = key;}

    public String getOperand1() {return this.operand1;}
    public void setOperand1(String operand1){ this.operand1 = operand1;}

    public String getOperand2() {return this.operand2;}
    public void setOperand2(String operand2){ this.operand2 = operand2;}

    public String getOperator() {return this.operator;}
    public void setOperator(String operator){ this.operator = operator;}

    public int getType() {return this.type;}
    public void setType(int type){ this.type = type;}

    public int getTransactionID() {return this.transactionID;}
    public void setTransactionID(int transactionID){ this.transactionID = transactionID;}


    public Lock getLock()
    {
        if(this.type == this.READ || this.type == this.WRITE) {
            return new Lock(this.type, this.key, this.transactionID);
        }
        return null;
    }

    public String toString()
    {
        StringBuilder result = new StringBuilder();
        result.append("[");
        if(this.type == Operation.CALC)
        {
            result.append("CALC (" + this.key + "=" + this.operand1 + this.operator + this.operand2 +"),");
        }
        else if(this.type == Operation.READ)
        {
            result.append("READ (" + this.key + "),");

        }
        else if(this.type == Operation.WRITE)
        {
            result.append("WRITE (" + this.key + "),");

        }
        result.append("tID: " +
                transactionID + ", sID" +
                Transaction.getSiteID(transactionID) + "]");

        return result.toString();
    }

}
