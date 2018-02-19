/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package engines.impl;

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
public class BBBottomUp<N extends Node, E extends Edge<N>> extends Algorithm<N, E> {

  public BBBottomUp(LabeledUndirectedGraph<N, E> graph) throws NotConnectedGraphException {
    super(graph);
    minGraph = new LabeledUndirectedGraph<>(graph);
  }

  protected void compute(LabeledUndirectedGraph<N, E> g) {
    g.getRemovedLabels().forEach(label -> {
      LabeledUndirectedGraph<N, E> cg = new LabeledUndirectedGraph<>(g);
      cg.getRemovedEdges().forEach(redge -> {
        if (redge.getLabel().equals(label)) {
          cg.addEdge(redge);
        }
      });
      if (cg.calculateCost() < minGraph.calculateCost()) {
        if (cg.isConnected()) {
          minGraph = cg;
        } else {
          compute(cg);
        }
      }
    });
  }

  @Override
  public void start() throws Exception {
    LabeledUndirectedGraph<N, E> zero = new LabeledUndirectedGraph<>(minGraph);
    zero.getEdges().forEach(edge -> zero.removeEdge(edge));
    compute(zero);
  }

}
