/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package engines;

import engines.exceptions.NotConnectedGraphException;
import engines.impl.BottomUp;
import engines.impl.BottomUpT;
import engines.impl.ERA;
import engines.impl.MVCA;
import engines.impl.MVCAO1;
import engines.impl.TabuSearch;
import engines.impl.TopDown;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import mlst.struct.Edge;
import mlst.struct.LabeledUndirectedGraph;
import mlst.struct.Node;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author Niccolò Ferrari
 * @param <N>
 * @param <E>
 */
public abstract class Algorithm<N extends Node, E extends Edge<N>> {

  public static enum Algorithms {
    E_TOPDOWN(TopDown.class, "[EXACT]", "td"),
    E_BOTTOMUP(BottomUp.class, "[EXACT]", "bu"),
    E_MULTITHREADS_BOTTOMUP(BottomUpT.class, "[EXACT, MULTITHREADED]", "threaded_bu"),
    H_GREDDY_MVCA(MVCA.class, "[HEURISTIC, GREEDY]", "greedy"),
    H_GREEDY_MVCAO1_GREEDY(MVCAO1.class, "[HEURISTIC, GREEDY]", "greedy_opt1"),
    H_ERA(ERA.class, "[HEURISTIC]"),
    H_TABU(TabuSearch.class, "[HEURISTIC]");

    private final Class<? extends Algorithm> aclass;
    private final List<String> aliases = new ArrayList<>();
    private final String desc;

    private Algorithms(Class<? extends Algorithm> aclass, String desc, String... aliases) {
      this.aclass = aclass;
      this.desc = desc;
      if (aliases != null) {
        this.aliases.addAll(Arrays.asList(aliases));
      }
    }

    @Override
    public String toString() {
      return aclass.getSimpleName().trim().toLowerCase();
    }

    public Class<? extends Algorithm> getAclass() {
      return aclass;
    }

    public List<String> getAliases() {
      return aliases;
    }

    public String getDesc() {
      return desc;
    }

    public static <N extends Node, E extends Edge<N>> List<Algorithm<N, E>> getAlgorithmsInstances(String name, LabeledUndirectedGraph<N, E> graph) throws Exception {
      List<Algorithm<N, E>> algs_i = new ArrayList<>();
      List<Algorithms> algs = getAlgorithms(name);
      if (algs != null && !algs.isEmpty()) {
        for (Algorithms _alg : algs) {
          algs_i.add(getAlgorithmInstance(_alg, graph));
        }
      }
      return algs_i;
    }

    public static List<Algorithms> getAlgorithms(String name) throws Exception {
      List<Algorithms> algorithms = new ArrayList<>();
      for (Algorithms alg : values()) {
        List<String> names = new ArrayList<>();
        names.add(alg.getAclass().getSimpleName().trim().toLowerCase());
        names.addAll(alg.getAliases());
        if (names.contains(name)) {
          algorithms.add(alg);
        }
      }
      return algorithms;
    }

    public static <N extends Node, E extends Edge<N>> Algorithm<N, E> getAlgorithmInstance(Algorithms alg, LabeledUndirectedGraph<N, E> graph) throws Exception {
      if (alg != null && graph != null) {
        Class<? extends Algorithm> aclass = alg.getAclass();
        Constructor<? extends Algorithm> constructor = aclass.getConstructor(graph.getClass());
        constructor.setAccessible(true);
        Algorithm<N, E> algorithm = (Algorithm<N, E>) constructor.newInstance(graph);
        return algorithm;
      } else {
        return null;
      }
    }

    public static List<Algorithms> getValues() {
      return Arrays.asList(values());
    }
  }

  protected final LabeledUndirectedGraph<N, E> graph;

  protected volatile LabeledUndirectedGraph<N, E> minGraph;

  protected Integer maxThreads = null;

  public Algorithm(LabeledUndirectedGraph<N, E> graph) throws NotConnectedGraphException {
    if (!graph.isConnected()) {
      NotConnectedGraphException ex = new NotConnectedGraphException("Graph must be connected");
      LogManager.getLogger().fatal("NotConnectedGraphException: " + ex.getMessage(), ex);
      throw ex;
    }
    this.graph = new LabeledUndirectedGraph<>(graph);
    minGraph = new LabeledUndirectedGraph<>(graph);
  }

  public final void run() throws Exception {
    minGraph = new LabeledUndirectedGraph<>(graph);
    start();
  }

  protected abstract void start() throws Exception;

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

    LabeledUndirectedGraph<N, E> spanningTree = new LabeledUndirectedGraph<>(graph.getNodes());
    graph.getEdges().forEach(edge -> {
      spanningTree.addEdge(edge);
      if (!sedges.contains(edge)) {
        spanningTree.removeEdge(edge);
      }
    });
    return spanningTree;
  }

  protected LabeledUndirectedGraph<N, E> getZeroGraph(LabeledUndirectedGraph<N, E> g) {
    LabeledUndirectedGraph<N, E> test = new LabeledUndirectedGraph<>(g);
    Set<String> nlabels = new HashSet<>();
    test.getLabels().forEach(label -> {
      Set<E> ledges = test.getEdges().stream().filter(edge -> edge.getLabel().equals(label)).collect(Collectors.toSet());
      for (E edge : ledges) {
        test.removeEdge(edge);
        if (!test.isConnected()) {
          nlabels.add(label);
          test.addEdge(edge);
          break;
        }
        test.addEdge(edge);
      }
    });
    LabeledUndirectedGraph<N, E> zero = new LabeledUndirectedGraph<>(g);
    zero.getEdges().forEach(edge -> {
      if (!nlabels.contains(edge.getLabel())) {
        zero.removeEdge(edge);
      }
    });
    return zero;
  }

  public LabeledUndirectedGraph<N, E> getGraph() {
    return graph;
  }

  public LabeledUndirectedGraph<N, E> getMinGraph() {
    return minGraph;
  }

  public Integer getMaxThreads() {
    return maxThreads;
  }

  public void setMaxThreads(Integer maxThreads) {
    this.maxThreads = maxThreads;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!getClass().equals(obj.getClass())) {
      return false;
    }
    return Objects.hashCode(this) == Objects.hashCode(obj);
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 13 * hash + Objects.hashCode(getClass().getName());
    return hash;
  }

  protected static <T, E> List<T> getKeysByValue(Map<T, E> map, E value) {
    List<T> keys = new ArrayList<>();
    map.entrySet().stream().filter((entry) -> (Objects.equals(value, entry.getValue()))).forEachOrdered((entry) -> {
      keys.add(entry.getKey());
    });
    return keys;
  }

}
