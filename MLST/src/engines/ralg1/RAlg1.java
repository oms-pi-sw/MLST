/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package engines.ralg1;

import engines.Algorithm;
import engines.exceptions.NotConnectedGraphException;
import mlst.struct.Edge;
import mlst.struct.LabeledUndirectedGraph;
import mlst.struct.Node;

/**
 *
 * @author Niccolò Ferrari
 * @param <N>
 * @param <E>
 */
public class RAlg1<N extends Node, E extends Edge<N>> extends Algorithm<N, E> {

  public RAlg1(LabeledUndirectedGraph<N, E> graph) throws NotConnectedGraphException {
    super(graph);
    minGraph = new LabeledUndirectedGraph<>(graph);
  }

  private void compute(LabeledUndirectedGraph<N, E> g) {
    g.getRemovedLabels().forEach(label -> {
      LabeledUndirectedGraph<N, E> cg = new LabeledUndirectedGraph<>(g);
      cg.getRemovedEdges().forEach(redge -> {
        if (redge.getLabel().equals(label)) {
          cg.addEdge(redge);
        }
      });
      if (cg.isConnected()) {
        if (minGraph.calculateCost() > cg.calculateCost()) {
          minGraph = cg;
        }
      } else {
        if (cg.calculateCost() < minGraph.calculateCost()) {
          compute(cg);
        }
      }
    });
  }

  @Override
  public void start() {
    LabeledUndirectedGraph<N, E> zero = new LabeledUndirectedGraph<>(minGraph);
    zero.getEdges().forEach(edge -> zero.removeEdge(edge));
    compute(zero);
  }

}
