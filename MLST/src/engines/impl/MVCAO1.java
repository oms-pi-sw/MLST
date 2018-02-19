/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package engines.impl;

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
public class MVCAO1<N extends Node, E extends Edge<N>> extends MVCA<N, E> {

  public MVCAO1(LabeledUndirectedGraph<N, E> graph) throws NotConnectedGraphException {
    super(graph);
  }

  @Override
  public void start() {
    compute(getZeroGraph(minGraph));
  }

}
