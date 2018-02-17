/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package engines;

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
public abstract class Algorithm<N extends Node, E extends Edge<N>> {

  protected final LabeledUndirectedGraph<N, E> graph;

  public Algorithm(LabeledUndirectedGraph<N, E> graph) throws NotConnectedGraphException {
    if (!graph.isConnected()) {
      throw new NotConnectedGraphException("Graph must be connected");
    }
    this.graph = graph;
  }

  public abstract void start();

  public abstract LabeledUndirectedGraph<N, E> getMinGraph();
}
