package WaitForGraph;

import DataStruct.pair;
import ParticipantSite.ParticipantSite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class Graph
{
    // Vertices
    HashSet<Vertex> vertices;
    HashMap<Vertex, LinkedList<Vertex>> adjList;
    ArrayList<pair<Vertex, Vertex>> cycles;

    int size;

    public Graph()
    {
        size = 0;
        vertices = new HashSet<Vertex>();
        adjList = new HashMap<Vertex, LinkedList<Vertex>>();
    }

    public void addVertex(Vertex v)
    {
        vertices.add(v);
        size++;
    }

    public void addEdge(Vertex from, Vertex to)
    {
        addVertex(from);
        addVertex(to);

        if(adjList.containsKey(from) == false)
        {
            adjList.put(from, new LinkedList<Vertex>());

        }

        adjList.get(from).add(to);
    }

    public boolean containsVertex(int transactionID)
    {
        for(Vertex v: vertices)
        {
            if(v.getValue() == transactionID)
                return true;
        }
        return false;
    }
    public pair<Integer, Integer> getSize()
    {
        return new pair<Integer, Integer>(vertices.size(), adjList.size());
    }

    public Vertex getVertex(int transactionID)
    {
        if(vertices.size() == 0)
            return null;

        for(Vertex v: vertices)
        {
            if(v.getValue() == transactionID)
                return v;
        }
        return null;
    }


    public void resetGraph()
    {
        for(Vertex v: vertices)
        {
            v.reset();
        }
        cycles = new ArrayList<pair<Vertex, Vertex>>();
    }

    public boolean containsCycle()
    {
        if(cycles.size() > 0)
            return true;
        return false;
    }

    public ArrayList<pair<Vertex, Vertex>> getCycles()
    {
        DFS();
        return this.cycles;

    }

    public void DFS()
    {
        resetGraph();
        for(Vertex v: vertices)
        {
            if(cycles.contains(v) == false)
                visit(v, cycles);
        }


    }
    public void visit(Vertex u, ArrayList<pair<Vertex, Vertex>> cycles)
    {

        u.setColor(Vertex.GREY);
        if(adjList.containsKey(u) && adjList.get(u).size() > 0)
        {
            for (Vertex v : adjList.get(u)) {
                if (v.getColor() == Vertex.GREY) {
                    cycles.add(new pair<Vertex, Vertex>(u, v));
                } else if (v.getColor() == Vertex.WHITE) {
                    visit(v, cycles);
                }
            }
        }
        u.setColor(Vertex.BLACK);
    }


    @Override
    public String toString()
    {

//        System.out.println("In graph to string " + vertices.size() + " " + adjList.size());
        StringBuilder result = new StringBuilder("Graph{vertices= [");

        if(vertices != null & vertices.size() > 0)
        {
            for (Vertex v : vertices)
            {
                result.append("\n[" + v.toString());
                result.append("Edge:[");
                if (adjList.containsKey(v) && adjList.get(v).size() > 0) {
                    for (Vertex u : adjList.get(v)) {
                        result.append(v.getName() + "->" + u.getName() + ",");
                    }
                }
                result.append("],");
            }
        }
        result.append("]};");
        return result.toString();
    }
}



