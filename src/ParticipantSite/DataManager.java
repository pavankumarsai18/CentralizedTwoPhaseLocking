package ParticipantSite;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeMap;

public class DataManager
{
    public static final String TAG = DataManager.class.getName();
    public static final String DB_DRIVER = "org.sqlite.JDBC"; // Default driver for sqllit in JDBC
    private String DB_URL = "jdbc:sqlite:C2PL";
    public static final int timeOut = 15; // seconds
    private int siteID;
    private ParticipantSite site;

    public int getSiteID()
    {
        return this.siteID;
    }

    public void setSite(ParticipantSite site){this.site = site;}
    public static String timeStamp()
    {
        return "["+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").format(new Date())+"] ";
    }
    public void print(String msg)
    {
        System.out.println(timeStamp() + TAG+ " "+ msg);
    }

    /**Given a site ID this function creates a Database with file name 'C2PL{siteID} and creates the table'*/
    public DataManager(int siteID)
    {

        int success = 0;
        Connection conn = null;
        try
        {
            Class.forName(DB_DRIVER);
            String newURL = "jdbc:sqlite:C2PL" + String.valueOf(siteID) +".db";
            this.DB_URL = newURL;
            this.siteID = siteID;

            conn = DriverManager.getConnection(this.DB_URL);

            // The table we want to create may exist in the DB so we drop it if it exists
            Statement stmt = conn.createStatement();
            stmt.setQueryTimeout(timeOut); // Set query time to 15 seconds
            String SQL_QUERY = "DROP TABLE IF EXISTS variables";
            stmt.executeUpdate(SQL_QUERY);

            // Now we create the table called variables
            SQL_QUERY = "CREATE TABLE variables (variable STRING, value INTEGER)";

            success = stmt.executeUpdate(SQL_QUERY);
            stmt.close();
            conn.close();
        }
        catch(ClassNotFoundException exception)
        {
            System.out.println(TAG + ": "+ exception.getMessage());
        }
        catch(SQLException exception)
        {

            System.out.println(TAG + ": " + exception.getMessage());
        }

    }
    /**Returns the value of the variable in the database*/
    public int read(String variableName)
    {
        int varValue = 0;

        Connection conn = null;
        try
        {
            Class.forName(DB_DRIVER);
            conn = DriverManager.getConnection(DB_URL);

            // The table we want to create may exist in the DB so we drop it if it exists
            Statement stmt = conn.createStatement();
            stmt.setQueryTimeout(timeOut); // Set query time to 15 seconds
            String SQL_QUERY = "SELECT value FROM variables WHERE variable = \'" + variableName +"\'";

            ResultSet results = stmt.executeQuery(SQL_QUERY);

            if(results.next())
            {
                varValue = results.getInt("value");
            }
            else
            {
                write(variableName, varValue);
            }

            results.close();
            stmt.close();
            conn.close();
        }
        catch(ClassNotFoundException exception)
        {
            System.out.println(TAG + ": "+ exception.getMessage());
        }
        catch(SQLException exception)
        {
            System.out.println(TAG + ": " + exception.getMessage());
        }
        print("Result of read = " + varValue);
        return varValue;

    }

    /**Writes all the rows in the Hashtable and writes in the DB*/
    public void writeALL(Hashtable<String, Integer> values)
    {
        for(String varName: values.keySet())
        {
            int value = values.get(varName);
            this.write(varName, value);
        }
    }


    /**Reads all the rows in the database and returns it in Hashtable*/
    public Hashtable<String, Integer> readAll()
    {

        Hashtable<String, Integer> result = null;

        Connection conn = null;
        try
        {
            Class.forName(DB_DRIVER);
            conn = DriverManager.getConnection(DB_URL);

            // The table we want to create may exist in the DB so we drop it if it exists
            Statement stmt = conn.createStatement();
            stmt.setQueryTimeout(timeOut); // Set query time to 15 seconds
            String SQL_QUERY = "SELECT variable, value FROM variables";

            ResultSet queryResult = stmt.executeQuery(SQL_QUERY);
            result = new Hashtable<String, Integer>();

            while(queryResult.next())
            {
                String varName = queryResult.getString("variable");
                int val = queryResult.getInt("value");

                result.put(varName, val);

            }

            queryResult.close();
            stmt.close();
            conn.close();
        }
        catch(ClassNotFoundException exception)
        {
            System.out.println(TAG + ": "+ exception.getMessage());
        }
        catch(SQLException exception)
        {
            System.out.println(TAG + ": " + exception.getMessage());
        }
        return result;
    }




    /**Writes variable, value to the DB*/
    public int write(String variable, int value)
    {

        int success = 0;

        Connection conn = null;
        try
        {
            Class.forName(DB_DRIVER);
            conn = DriverManager.getConnection(DB_URL);

            Statement stmt = conn.createStatement();
            stmt.setQueryTimeout(timeOut); // Set query time to 15 seconds
            String SQL_QUERY = "SELECT value FROM variables WHERE variable = '" + variable +"'";

            ResultSet results = stmt.executeQuery(SQL_QUERY);

            if(results.next())
            {
                SQL_QUERY = "UPDATE variables SET value = "+ value + " WHERE variable = '"+variable + "'";

            }
            else
            {
                SQL_QUERY = "INSERT INTO variables VALUES ('"+variable+"', "+value+")";

            }
            success = stmt.executeUpdate(SQL_QUERY);

            results.close();
            stmt.close();
            conn.close();
        }
        catch(ClassNotFoundException exception)
        {
            System.out.println(TAG + ": "+ exception.getMessage());
        }
        catch(SQLException exception)
        {
            System.out.println(TAG + ": " + exception.getMessage());
        }

        print(printDB());

        return success;

    }

    /**Returns a string containing all the variables, values of the DB*/
    public String printDB()
    {

        StringBuilder result = new StringBuilder("\n\n");
        Connection conn = null;
        Hashtable<String, Integer> DBTable = new Hashtable<String, Integer>();

        try
        {
            Class.forName(DB_DRIVER);
            conn = DriverManager.getConnection(DB_URL);

            Statement stmt = conn.createStatement();
            stmt.setQueryTimeout(timeOut); // Set query time to 15 seconds

            /*Get all the values from the DB**/
            String SQL_QUERY = "SELECT * FROM variables"; // To get all items we run the SELECT * query
            ResultSet results = stmt.executeQuery(SQL_QUERY);

            /*Iterate through items in the DB and put it in the hashtable**/
            while(results.next())
            {
                String variableName = results.getString("variable");
                int value = results.getInt("value");
                DBTable.put(variableName,  value);
            }
            results.close();
            stmt.close();
            conn.close();
        }
        catch(ClassNotFoundException exception)
        {
            System.out.println(TAG + ": "+ exception.getMessage());
        }
        catch(SQLException exception)
        {
            System.out.println(TAG + ": " + exception.getMessage());
        }

        /*Iterate through items in the DB and put it in the TreeMap**/
        TreeMap<String, Integer> tm = new TreeMap<String, Integer>(DBTable);
        Iterator<String> it = tm.keySet().iterator();


        /*Iterate through items in the TreeMap and append it in the result variable
        * This makes the variable in sorted order
        * **/
        while(it.hasNext())
        {
            String varName = it.next();
            result.append("[" + varName + "  =  " + tm.get(varName) + "]\n");
        }

        return result.toString();
    }





}
