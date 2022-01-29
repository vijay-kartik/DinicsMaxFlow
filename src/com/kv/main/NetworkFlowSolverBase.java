package com.kv.main;
import java.util.ArrayList;
import java.util.List;

abstract class NetworkFlowSolverBase {

  // To avoid overflow, set infinity to a value less than Long.MAX_VALUE;
  protected static final long INF = Long.MAX_VALUE / 2;

  public static class Edge {
    public int from, to;
    public String fromLabel, toLabel;
    public Edge residual;
    public long flow, cost;
    public final long capacity, originalCost;

    public Edge(int from, int to, long capacity) {
      this(from, to, capacity, 0 /* unused */);
    }

    public Edge(int from, int to, long capacity, long cost) {
      this.from = from;
      this.to = to;
      this.capacity = capacity;
      this.originalCost = this.cost = cost;
    }

    public boolean isResidual() {
      return capacity == 0;
    }

    public long remainingCapacity() {
      return capacity - flow;
    }

    public void augment(long bottleNeck) {
      flow += bottleNeck;
      residual.flow -= bottleNeck;
    }

    public String toString(int s, int t) {
      String u = (from == s) ? "s" : ((from == t) ? "t" : String.valueOf(from));
      String v = (to == s) ? "s" : ((to == t) ? "t" : String.valueOf(to));
      return String.format(
          "Edge %s -> %s | flow = %d | capacity = %d | is residual: %s",
          u, v, flow, capacity, isResidual());
    }
  }

  // Inputs: n = number of nodes, s = source, t = sink
  protected int n, s, t;

  protected long maxFlow;
  protected long minCost;

  protected boolean[] minCut;
  protected List<Edge>[] graph;
  protected String[] vertexLabels;
  protected List<Edge> edges;

  // 'visited' and 'visitedToken' are variables used for graph sub-routines to
  // track whether a node has been visited or not. In particular, node 'i' was
  // recently visited if visited[i] == visitedToken is true. This is handy
  // because to mark all nodes as unvisited simply increment the visitedToken.
  private int visitedToken = 1;
  private int[] visited;

  // Indicates whether the network flow algorithm has ran. We should not need to
  // run the solver multiple times, because it always yields the same result.
  protected boolean solved;

  /**
   * Creates an instance of a flow network solver. Use the {@link #addEdge} method to add edges to
   * the graph.
   *
   * @param n - The number of nodes in the graph including source and sink nodes.
   */
  public NetworkFlowSolverBase(int n, String[] vertexLabels) {
    this.n = n;
    initializeGraph();
    assignLabelsToVertices(vertexLabels);
    minCut = new boolean[n];
    visited = new int[n];
    edges = new ArrayList<>();
  }

  // Construct an empty graph with n nodes including the source and sink nodes.
  private void initializeGraph() {
    graph = new List[n];
    for (int i = 0; i < n; i++) graph[i] = new ArrayList<Edge>();
  }

  // Add labels to vertices in the graph.
  private void assignLabelsToVertices(String[] vertexLabels) {
    if(vertexLabels.length != n)
      throw new IllegalArgumentException(String.format("You must pass %s number of labels", n));
    this.vertexLabels = vertexLabels;
  }

  /**
   * Adds a list of directed edges (and residual edges) to the flow graph.
   *
   * @param edges - A list of all edges to be added to the flow graph.
   */
  public void addEdges(List<Edge> edges) {
    if (edges == null) throw new IllegalArgumentException("Edges cannot be null");
    for(Edge edge : edges) {
      addEdge(edge.from, edge.to, edge.capacity);
    }
  }

  /**
   * Adds a directed edge (and residual edge) to the flow graph.
   *
   * @param from - The index of the node the directed edge starts at.
   * @param to - The index of the node the directed edge ends at.
   * @param capacity - The capacity of the edge.
   */
  public void addEdge(int from, int to, long capacity) {
    if (capacity < 0) throw new IllegalArgumentException("Capacity < 0");
    Edge e1 = new Edge(from, to, capacity);
    Edge e2 = new Edge(to, from, 0);
    e1.residual = e2;
    e2.residual = e1;
    graph[from].add(e1);
    graph[to].add(e2);
    edges.add(e1);
  }

  /** Cost variant of {@link #addEdge(int, int, int)} for min-cost max-flow */
  public void addEdge(int from, int to, long capacity, long cost) {
    Edge e1 = new Edge(from, to, capacity, cost);
    Edge e2 = new Edge(to, from, 0, -cost);
    e1.residual = e2;
    e2.residual = e1;
    graph[from].add(e1);
    graph[to].add(e2);
    edges.add(e1);
  }

  // Marks node 'i' as visited.
  public void visit(int i) {
    visited[i] = visitedToken;
  }

  // Returns whether or not node 'i' has been visited.
  public boolean visited(int i) {
    return visited[i] == visitedToken;
  }

  // Resets all nodes as unvisited. This is especially useful to do
  // between iterations of finding augmenting paths, O(1)
  public void markAllNodesAsUnvisited() {
    visitedToken++;
  }

  /**
   * Returns the graph after the solver has been executed. This allow you to inspect the {@link
   * Edge#flow} compared to the {@link Edge#capacity} in each edge. This is useful if you want to
   * figure out which edges were used during the max flow.
   */
  public List<Edge>[] getGraph() {
    execute();
    return graph;
  }

  /**
   * Returns all edges in this flow network
   */
  public List<Edge> getEdges() {
    return edges;
  }

  // Returns the maximum flow from the source to the sink.
  public long getMaxFlow() {
    execute();
    return maxFlow;
  }

  // Returns the min cost from the source to the sink.
  // NOTE: This method only applies to min-cost max-flow algorithms.
  public long getMinCost() {
    execute();
    return minCost;
  }

  // Returns the min-cut of this flow network in which the nodes on the "left side"
  // of the cut with the source are marked as true and those on the "right side"
  // of the cut with the sink are marked as false.
  public boolean[] getMinCut() {
    execute();
    return minCut;
  }

  /**
   * Used to set the source for this flow network 
   */
  public void setSource(int s) {
    this.s = s;
  }

  /**
   * Used to set the sink for this flow network 
   */
  public void setSink(int t) {
    this.t = t;
  }

  /**
   * Get source for this flow network 
   */
  public int getSource() {
    return s;
  }

  /**
   * Get sink for this flow network 
   */
  public int getSink() {
    return t;
  }

  /**
   * Set 'solved' flag to false to force recomputation of subsequent flows.
   */
  public void recompute() {
    solved = false;
  }

  /**
   * Print all edges.
   */
  public void printEdges() {
    for(Edge edge : edges) {
      System.out.println(String.format("%s ----%s----> %s", vertexLabels[edge.from], edge.capacity, vertexLabels[edge.to]));
    }
  }

  // Wrapper method that ensures we only call solve() once
  private void execute() {
    if (solved) return;
    solved = true;
    solve();
  }

  // Method to implement which solves the network flow problem.
  public abstract void solve();
}
