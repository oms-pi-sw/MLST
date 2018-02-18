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
public class Alg1<N extends Node, E extends Edge<N>> extends Algorithm<N, E> {

  public Alg1(LabeledUndirectedGraph<N, E> graph) throws NotConnectedGraphException {
    super(graph);
  }

  protected void compute(LabeledUndirectedGraph<N, E> g) {
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

  @Override
  public void start() throws Exception {
    compute(graph);
  }
}
