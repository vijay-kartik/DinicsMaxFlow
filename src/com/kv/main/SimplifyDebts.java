package com.kv.main;
import java.util.*;
import static java.lang.Math.min;

/**
 * Implementation of algorithm to simplify debts using Dinic's network flow algorithm. The algorithm picks edges one at a time and
 * runs the network flow algorithm to generates the residual graph, which is then again fed back to the network flow algorithm
 * until there are no more non visited edges.
 *
 * <p>Time Complexity: O(E²V²)
 *
 * @author Mithun Mohan K, mithunmk93@gmail.com
 */
public class SimplifyDebts {
  private static final long OFFSET = 1000000000L;
  private static Set<Long> visitedEdges;

  public static void main(String[] args) {
    createGraphForDebts();
  }

  /**
   * This example graph is taken from my Medium blog post.
   * Here Alice, Bob, Charlie, David, Ema, Fred and Gabe are represented by vertices from 0 to 6 respectively.
   */
  private static void createGraphForDebts() {
    //  List of all people in the group
    String[] person = { "Alice", "Bob", "Charlie", "David", "Ema", "Fred", "Gabe"};
    int n = person.length;
    //  Creating a graph with n vertices
    Dinics solver = new Dinics(n, person);
    //  Adding edges to the graph
    solver = addAllTransactions(solver);

    System.out.println();
    System.out.println("Simplifying Debts...");
    System.out.println("--------------------");
    System.out.println();

    //  Map to keep track of visited edges
    visitedEdges = new HashSet<>();
    Integer edgePos;

    while((edgePos = getNonVisitedEdge(solver.getEdges())) != null) {
      //  Force recomputation of subsequent flows in the graph
      solver.recompute();
      //  Set source and sink in the flow graph
      Dinics.Edge firstEdge = solver.getEdges().get(edgePos);
      solver.setSource(firstEdge.from);
      solver.setSink(firstEdge.to);
      //  Initialize the residual graph to be same as the given graph
      List<Dinics.Edge>[] residualGraph = solver.getGraph();
      List<Dinics.Edge> newEdges = new ArrayList<>();

      for(List<Dinics.Edge> allEdges : residualGraph) {
        for(Dinics.Edge edge : allEdges) {
          long remainingFlow = ((edge.flow < 0) ? edge.capacity : (edge.capacity - edge.flow));
          //  If there is capacity remaining in the graph, then add the remaining capacity as an edge
          //  so that it can be used for optimizing other debts within the graph
          if(remainingFlow > 0) {
            newEdges.add(new Dinics.Edge(edge.from, edge.to, remainingFlow));
          }
        }
      }

      //  Get the maximum flow between the source and sink
      long maxFlow = solver.getMaxFlow();
      //  Mark the edge from source to sink as visited
      int source = solver.getSource();
      int sink = solver.getSink();
      visitedEdges.add(getHashKeyForEdge(source, sink));
      //  Create a new graph
      solver = new Dinics(n, person);
      //  Add edges having remaining capacity
      solver.addEdges(newEdges);
      //  Add an edge from source to sink in the new graph with obtained maximum flow as it's weight
      solver.addEdge(source, sink, maxFlow);
    }
    //  Print the edges in the graph
    solver.printEdges();
    System.out.println();
  }

  private static Dinics addAllTransactions(Dinics solver) {
    //  Transactions made by Bob
    solver.addEdge(1, 2, 40);
    //  Transactions made by Charlie
    solver.addEdge(2, 3, 20);
    //  Transactions made by David
    solver.addEdge(3, 4, 50);
    //  Transactions made by Fred
    solver.addEdge(5, 1, 10);
    solver.addEdge(5, 2, 30);
    solver.addEdge(5, 3, 10);
    solver.addEdge(5, 4, 10);
    //  Transactions made by Gabe
    solver.addEdge(6, 1, 30);
    solver.addEdge(6, 3, 10);
    return solver;
  }

  /**
  * Get any non visited edge in the graph
  * @param edges list of all edges in the graph
  * @return index of a non visited edge
  */
  private static Integer getNonVisitedEdge(List<Dinics.Edge> edges) {
    Integer edgePos = null;
    int curEdge = 0;
    for(Dinics.Edge edge : edges) {
      if(!visitedEdges.contains(getHashKeyForEdge(edge.from, edge.to))) {
        edgePos = curEdge;
      }
      curEdge++;
    }
    return edgePos;
  }

  /**
  * Get a unique hash key for a given edge
  * @param u the starting vertex in the edge
  * @param v the ending vertex in the edge
  * @return a unique hash key
  */
  private static Long getHashKeyForEdge(int u, int v) {
    return u * OFFSET + v;
  }
}


