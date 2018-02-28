/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package engines.impl;

import engines.Algorithm;
import engines.exceptions.NotConnectedGraphException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import mlst.struct.Edge;
import mlst.struct.LabeledUndirectedGraph;
import mlst.struct.Node;

/**
 *
 * @author Niccolò Ferrari
 * @param <N>
 * @param <E>
 */
public class ERA<N extends Node, E extends Edge<N>> extends Algorithm<N, E> {

  public ERA(LabeledUndirectedGraph<N, E> graph) throws NotConnectedGraphException {
    super(graph);
  }

  private Set<E> getCycleEdges(LabeledUndirectedGraph<N, E> g) {
    final LabeledUndirectedGraph<N, E> cg = new LabeledUndirectedGraph<>(g);
    final Set<E> cedges = new HashSet<>();
    cg.getEdges().forEach(edge -> {
      cg.removeEdge(edge);
      if (cg.isConnected()) {
        cedges.add(edge);
      }
      cg.addEdge(edge);
    });
    return cedges;
  }

  @Override
  public void start() throws Exception {
    final LabeledUndirectedGraph<N, E> g = new LabeledUndirectedGraph<>(minGraph);

    //1
    final LabeledUndirectedGraph<N, E> spanning = getSpanningTree();

    //2
    Set<E> allEdges = g.getEdges();
    allEdges.addAll(g.getRemovedEdges());
    allEdges.stream().filter(edge -> !spanning.getEdges().contains(edge)).forEachOrdered(edge -> {
      String label = edge.getLabel();
      //3
      if (spanning.getLabels().contains(label)) {
        //4
        spanning.addEdge(edge);
        Set<E> cycle = getCycleEdges(spanning);

        //5
        long current_label_count = cycle.stream().filter(_edge -> _edge.getLabel().equals(label)).count();

        //Get all lables in cycle
        Set<String> clabels = new HashSet<>();
        cycle.forEach(_edge -> clabels.add(_edge.getLabel()));

        String minimum_label = Collections.min(clabels, (String l1, String l2) -> {
          long o1 = cycle.stream().filter(_edge -> _edge.getLabel().equals(l1)).count();
          long o2 = cycle.stream().filter(_edge -> _edge.getLabel().equals(l2)).count();
          return (int) (o1 - o2);
        });

        long minimum_count = cycle.stream().filter(_edge -> _edge.getLabel().equals(minimum_label)).count();

        //6
        Random rand = new Random(System.currentTimeMillis());
        if (current_label_count > minimum_count && !label.equals(minimum_label)) {
          List<E> min_edges = cycle.stream()
                  .filter(_edge -> _edge.getLabel().equals(minimum_label))
                  .collect(Collectors.toList());
          E _redge = min_edges.get(rand.nextInt(min_edges.size()));
          spanning.removeEdge(_redge);
        } else {
          spanning.removeEdge(edge);
        }
      }
    });
    minGraph = spanning;
  }

}
