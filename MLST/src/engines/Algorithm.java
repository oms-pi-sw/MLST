/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package engines;

import engines.exceptions.NotConnectedGraphException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import mlst.struct.Edge;
import mlst.struct.LabeledUndirectedGraph;
import mlst.struct.Node;

/**
 *
 * @author Niccolò Ferrari
 * @param <N>
 * @param <E>
 */
public abstract class Algorithm<N extends Node, E extends Edge<N>> {

  protected final LabeledUndirectedGraph<N, E> graph;

  protected volatile LabeledUndirectedGraph<N, E> minGraph;

  public Algorithm(LabeledUndirectedGraph<N, E> graph) throws NotConnectedGraphException {
    if (!graph.isConnected()) {
      throw new NotConnectedGraphException("Graph must be connected");
    }
    this.graph = new LabeledUndirectedGraph<>(graph);
    minGraph = new LabeledUndirectedGraph<>(graph);
  }

  public abstract void start() throws Exception;

  public LabeledUndirectedGraph<N, E> getSpanningTree() {
    Queue<N> queue = new LinkedList<>();
    List<N> ns = new ArrayList<>(minGraph.getNodes());
    List<E> sedges = new ArrayList<>();
    List<E> edges = new ArrayList<>(minGraph.getEdges());

    Collections.shuffle(ns);

    N start = ns.remove(0);
    queue.add(start);

    while (!queue.isEmpty()) {
      N n = queue.poll();

      edges.forEach(edge -> {
        N o = edge.other(n);
        if (edge.has(n) && ns.contains(o)) {
          ns.remove(o);
          queue.add(o);
          sedges.add(edge);
        }
      });
    }

    LabeledUndirectedGraph<N, E> spanningTree = new LabeledUndirectedGraph<>(minGraph.getNodes());
    sedges.forEach(edge -> spanningTree.addEdge(edge));
    return spanningTree;
  }

  public LabeledUndirectedGraph<N, E> getGraph() {
    return graph;
  }

  public LabeledUndirectedGraph<N, E> getMinGraph() {
    return minGraph;
  }
}
