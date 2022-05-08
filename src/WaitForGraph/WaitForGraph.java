package WaitForGraph;

import DataStruct.pair;
import Transaction.Transaction;

import java.util.ArrayList;

public class WaitForGraph
{

    public static String TAG = WaitForGraph.class.getName();
    private Graph graph;

    public WaitForGraph()
    {
        graph = new Graph();
    }
    public String toString()
    {
        return this.graph.toString();
    }
    public boolean addDependency(Integer transaction1, Integer transaction2)
    {
        Vertex t1 = graph.getVertex(transaction1);
        Vertex t2 = graph.getVertex(transaction2);
        if(t1 == null)
        {
            t1 = new Vertex(transaction1);
            graph.addVertex(t1);
        }
        if(t2 == null)
        {
            t2 = new Vertex(transaction2);
            graph.addVertex(t2);
        }
        graph.addEdge(t1, t2);
        return true;
    }
    public pair<Integer, Integer> getSize()
    {

        return graph.getSize();
    }
    public boolean hasTransaction(int transactionID)
    {
        return graph.containsVertex(transactionID);
    }

    public pair<Vertex, Vertex> getCycle()
    {

        if(graph == null || graph.getCycles().size() == 0)
        {
            return null;
        }

        ArrayList<pair<Vertex, Vertex>> cycles = graph.getCycles();
        pair<Vertex, Vertex> cycleEdge = cycles.get(0);
        return cycleEdge;
    }


}
