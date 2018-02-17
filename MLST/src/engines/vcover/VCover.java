/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package engines.vcover;

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
public class VCover<N extends Node, E extends Edge<N>> extends Algorithm<N, E> {

  public VCover(LabeledUndirectedGraph<N, E> graph) throws NotConnectedGraphException {
    super(graph);
    throw new UnsupportedOperationException("VCover algorithm not supported yet.");
  }

  @Override
  public void start() {
    throw new UnsupportedOperationException("VCover algorithm not supported yet.");
  }

}
