package com.kv.main;
import static java.lang.Math.min;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

class Dinics extends NetworkFlowSolverBase {

  private int[] level;

  /**
   * Creates an instance of a flow network solver. Use the {@link #addEdge} method to add edges to
   * the graph.
   *
   * @param n - The number of nodes in the graph including source and sink nodes.
   */
  public Dinics(int n, String[] vertexLabels) {
    super(n, vertexLabels);
    level = new int[n];
  }

  @Override
  public void solve() {
    // next[i] indicates the next unused edge index in the adjacency list for node i. This is part
    // of the Shimon Even and Alon Itai optimization of pruning deads ends as part of the DFS phase.
    int[] next = new int[n];

    while (bfs()) {
      Arrays.fill(next, 0);
      // Find max flow by adding all augmenting path flows.
      for (long f = dfs(s, next, INF); f != 0; f = dfs(s, next, INF)) {
        maxFlow += f;
      }
    }

    for (int i = 0; i < n; i++) if (level[i] != -1) minCut[i] = true;
  }

  // Do a BFS from source to sink and compute the depth/level of each node
  // which is the minimum number of edges from that node to the source.
  private boolean bfs() {
    Arrays.fill(level, -1);
    level[s] = 0;
    Deque<Integer> q = new ArrayDeque<>(n);
    q.offer(s);
    while (!q.isEmpty()) {
      int node = q.poll();
      for (Edge edge : graph[node]) {
        long cap = edge.remainingCapacity();
        if (cap > 0 && level[edge.to] == -1) {
          level[edge.to] = level[node] + 1;
          q.offer(edge.to);
        }
      }
    }
    return level[t] != -1;
  }

  private long dfs(int at, int[] next, long flow) {
    if (at == t) return flow;
    final int numEdges = graph[at].size();

    for (; next[at] < numEdges; next[at]++) {
      Edge edge = graph[at].get(next[at]);
      long cap = edge.remainingCapacity();
      if (cap > 0 && level[edge.to] == level[at] + 1) {

        long bottleNeck = dfs(edge.to, next, min(flow, cap));
        if (bottleNeck > 0) {
          edge.augment(bottleNeck);
          return bottleNeck;
        }
      }
    }
    return 0;
  }
}
