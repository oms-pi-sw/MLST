/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package engines.alg1;

import engines.Algorithm;
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
public class Alg1<N extends Node, E extends Edge<N>> extends Algorithm<N, E> {

  private LabeledUndirectedGraph<N, E> minGraph;

  public Alg1(LabeledUndirectedGraph<N, E> graph) throws NotConnectedGraphException {
    super(graph);
    minGraph = new LabeledUndirectedGraph<>(graph);
  }

  private void compute(LabeledUndirectedGraph<N, E> g) {
    g.getLabels().forEach(label -> {
      LabeledUndirectedGraph<N, E> cg = new LabeledUndirectedGraph<>(g);
      cg.removeLabel(label);
      if (cg.isConnected()) {
        if (minGraph.calculateCost() > cg.calculateCost()) {
          minGraph = cg;
        }
        compute(cg);
      }
    });
  }

  public LabeledUndirectedGraph<N, E> getGraph() {
    return graph;
  }

  @Override
  public LabeledUndirectedGraph<N, E> getMinGraph() {
    return minGraph;
  }

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

  @Override
  public void start() {
    compute(graph);
  }
}
