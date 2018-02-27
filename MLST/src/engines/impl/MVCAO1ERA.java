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
public class MVCAO1ERA<N extends Node, E extends Edge<N>> extends Algorithm<N, E> {

  public MVCAO1ERA(LabeledUndirectedGraph<N, E> graph) throws NotConnectedGraphException {
    super(graph);
  }

  @Override
  protected void start() throws Exception {
    MVCAO1<N, E> mvcao1 = new MVCAO1<>(minGraph);
    mvcao1.run();
    ERA<N, E> era = new ERA<>(mvcao1.getMinGraph());
    minGraph = era.getMinGraph();
  }

}
