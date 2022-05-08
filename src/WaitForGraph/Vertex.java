package WaitForGraph;

import java.util.HashSet;

public class Vertex {

    public static String TAG = Vertex.class.getName();

    private int value; // Transaction ID
    private String name;
    private HashSet<Vertex> adjacentVertices;
    private int color;
    public static int WHITE = 0;
    public static int GREY = 1;
    public static int BLACK = 2;

    private Vertex parent;

    public Vertex(int transactionID)
    {
        this.value = transactionID;
        this.adjacentVertices= new HashSet<Vertex>();
        this.parent = null;
        this.color = 0;
        this.name = String.valueOf(transactionID);
    }

    public String getName()
    {
        return this.name;
    }

    @Override
    public String toString()
    {
        String Color = "White";
        if(color == GREY) Color = "Grey";
        else if(color == BLACK) Color = "Black";
        return "Vertex[" +
                "value=" + value +
                ", name='" + name + '\'' +
                ", color=" + Color +
                ']';
    }

    public Vertex(int transactionID, HashSet<Vertex> adjacentVertices)
    {
        this.value = transactionID;
        this.name = String.valueOf(transactionID);
        this.adjacentVertices= adjacentVertices;
        this.parent = null;
        this.color = 0;
    }

    public int getColor(){return this.color;}

    public void setColor(int color){ this.color = color;}
    public void setParent(Vertex v){
        this.parent = v;
    }
    public int getValue()
    {
        return this.value;
    }

    public void mark()
    {
        this.color++;
    }
    public void reset()
    {
        this.color = Vertex.WHITE;
        this.parent = null;
    }
    public void setValue(int value){this.value = value;}

    public HashSet<Vertex> getAdjacentVertices()
    {
        return this.adjacentVertices;
    }

    public void setAdjacentVertices(HashSet<Vertex> adjacentVertices)
    {this.adjacentVertices = adjacentVertices;}

    public void addVertex(Vertex v)
    {
        this.adjacentVertices.add(v);
    }


}
