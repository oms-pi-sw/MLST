/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package engines.impl;

import engines.Algorithm;
import engines.exceptions.NotConnectedGraphException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import mlst.struct.Edge;
import mlst.struct.LabeledUndirectedGraph;
import mlst.struct.Node;

/**
 *
 * @author Niccolò Ferrari
 * @param <N>
 * @param <E>
 */
public class MVCA<N extends Node, E extends Edge<N>> extends Algorithm<N, E> {

  public MVCA(LabeledUndirectedGraph<N, E> graph) throws NotConnectedGraphException {
    super(graph);
  }

  private static <T, E> List<T> getKeysByValue(Map<T, E> map, E value) {
    List<T> keys = new ArrayList<>();
    map.entrySet().stream().filter((entry) -> (Objects.equals(value, entry.getValue()))).forEachOrdered((entry) -> {
      keys.add(entry.getKey());
    });
    return keys;
  }

  @Override
  public void start() {
    LabeledUndirectedGraph<N, E> g = new LabeledUndirectedGraph<>(minGraph);
    g.getEdges().forEach(edge -> g.removeEdge(edge));
    Random rand = new Random();
    do {
      Map<String, Integer> cover = new HashMap<>();
      g.getRemovedLabels().forEach(rlabel -> {
        Set<Node> covered_nodes = new HashSet<>();
        g.getRemovedEdges().stream()
                .filter(redge -> redge.getLabel().equals(rlabel))
                .forEachOrdered(redge -> {
                  covered_nodes.add(redge.getNode1());
                  covered_nodes.add(redge.getNode2());
                });
        cover.put(rlabel, covered_nodes.size());
      });
      List<Integer> cover_list = new ArrayList<>(cover.values());
      int max = Collections.max(cover_list);
      List<String> mlabels = getKeysByValue(cover, max);
      String mlabel = mlabels.get(rand.nextInt(mlabels.size()));

      g.getRemovedEdges().stream()
              .filter(redge -> redge.getLabel().equals(mlabel))
              .forEachOrdered(redge -> g.addEdge(redge));
    } while (!g.isConnected());
    minGraph = g;
  }

}
