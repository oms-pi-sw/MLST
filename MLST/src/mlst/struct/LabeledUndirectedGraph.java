/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mlst.struct;

import ansiTTY.ansi.Ansi;
import static ansiTTY.ansi.Ansi.println;
import ansiTTY.ansi.format.AnsiColor;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.file.FileSinkImages;
import org.graphstream.stream.file.FileSinkImages.LayoutPolicy;
import org.graphstream.stream.file.FileSinkImages.OutputType;
import org.graphstream.stream.file.FileSinkImages.Quality;
import org.graphstream.stream.file.FileSinkImages.RendererType;
import org.graphstream.stream.file.FileSinkImages.Resolutions;

/**
 *
 * @author Niccolò Ferrari
 * @param <N>
 * @param <E>
 */
public final class LabeledUndirectedGraph<N extends Node, E extends Edge<N>> implements Cloneable {

  private final Set<N> nodes;
  private final Set<E> edges;
  private final Set<String> labels;

  private final Set<E> spanningEdges = new HashSet<>();
  private final Set<E> removedEdges;
  private final Set<String> removedLabels;

  public LabeledUndirectedGraph() {
    this.nodes = new HashSet<>();
    this.edges = new HashSet<>();
    this.labels = new HashSet<>();
    this.removedEdges = new HashSet<>();
    this.removedLabels = new HashSet<>();
  }

  public LabeledUndirectedGraph(Collection<N> nodes) {
    this();
    this.nodes.addAll(nodes);
  }

  public LabeledUndirectedGraph(LabeledUndirectedGraph<N, E> graph) {
    this.nodes = new HashSet<>(graph.nodes);
    this.edges = new HashSet<>(graph.edges);
    this.labels = new HashSet<>(graph.labels);
    this.removedEdges = new HashSet<>(graph.removedEdges);
    this.removedLabels = new HashSet<>(graph.removedLabels);
  }

  public boolean addNode(N node) {
    if (!nodes.contains(node)) {
      nodes.add(node);
      return true;
    }
    return false;
  }

  public boolean addEdge(E edge) {
    if (nodes.contains(edge.getNode1()) && nodes.contains(edge.getNode2())) {
      edges.add(edge);
      removedEdges.remove(edge);
      labels.add(edge.getLabel());
      removedLabels.remove(edge.getLabel());
      return true;
    }
    return false;
  }

  public void removeEdge(String label, N node1, N node2) {
    for (E edge : edges) {
      if (edge.getLabel().equals(label)
              && (edge.getNode1().equals(node1) && edge.getNode2().equals(node2)
              || edge.getNode1().equals(node2) && edge.getNode2().equals(node1))) {
        removeEdge(edge);
        break;
      }
    }
  }

  public void removeEdge(E edge) {
    if (edges.remove(edge)) {
      recalculateLabels();
      removedEdges.add(edge);
    }
  }

  private void recalculateLabels() {
    removedLabels.addAll(labels);
    labels.clear();
    edges.forEach(edge -> labels.add(edge.getLabel()));
    removedLabels.removeAll(labels);
  }

  public void removeNode(N node) {
    List<E> _edges = new ArrayList<>(edges);
    if (nodes.remove(node)) {
      _edges.forEach(edge -> {
        if (edge.has(node)) {
          removeEdge(edge);
        }
      });
    }
  }

  public void removeLabel(String label) {
    List<E> _edges = new ArrayList<>(edges);
    _edges.forEach(edge -> {
      if (edge.getLabel().equals(label)) {
        removeEdge(edge);
      }
    });
  }

  public boolean isConnected() {
    return getSubGraphsNodes().size() == 1;
  }

  public Set<Set<N>> getSubGraphsNodes() {
    if (!nodes.isEmpty()) {
      Set<N> unvisited = new HashSet<>(nodes);

      Set<Set<N>> components = new HashSet<>();

      nodes.forEach(node -> {
        if (unvisited.contains(node)) {
          Queue<N> queue = new LinkedList<>();
          queue.add(node);

          Set<N> component = new HashSet<>();
          components.add(component);

          while (!queue.isEmpty()) {
            N _n = queue.poll();
            unvisited.remove(_n);

            Set<N> neigh = getNeighbors(_n).stream()
                    .filter(_node -> unvisited.contains(_node)).collect(Collectors.toSet());
            unvisited.removeAll(neigh);
            queue.addAll(neigh);

            component.add(_n);
          }
        }
      });

      return components;
    } else {
      return new HashSet<>();
    }
  }

  public Set<N> getNeighbors(N node) {
    if (nodes.contains(node)) {
      Set<N> neigh = new HashSet<>();
      edges.forEach(e -> {
        if (e.has(node)) {
          neigh.add(e.other(node));
        }
      });
      return neigh;
    } else {
      return null;
    }
  }

  public Set<N> getNodes() {
    return new HashSet<>(nodes);
  }

  public Set<E> getEdges() {
    return new HashSet<>(edges);
  }

  public Set<String> getLabels() {
    return new HashSet<>(labels);
  }

  public Set<E> getSpanningEdges() {
    return new HashSet<>(spanningEdges);
  }

  public Set<E> getRemovedEdges() {
    return new HashSet<>(removedEdges);
  }

  public Set<String> getRemovedLabels() {
    return new HashSet<>(removedLabels);
  }

  @Override
  public LabeledUndirectedGraph clone() {
    return new LabeledUndirectedGraph(this);
  }

  public int calculateCost() {
    return labels.size();
  }

  public void calculateSpanningTree() {
    Queue<N> queue = new LinkedList<>();
    List<N> ns = new ArrayList<>(getNodes());
    List<E> sedges = new ArrayList<>();
    List<E> ledges = new ArrayList<>(getEdges());

    Collections.shuffle(ns);

    N start = ns.remove(0);
    queue.add(start);

    while (!queue.isEmpty()) {
      N n = queue.poll();

      ledges.forEach(edge -> {
        N o = edge.other(n);
        if (edge.has(n) && ns.contains(o)) {
          ns.remove(o);
          queue.add(o);
          sedges.add(edge);
        }
      });
    }

    spanningEdges.addAll(sedges);
  }

  public void plot(String filename) throws IOException {
    plot(filename, true, true);
  }

  public void plot(String filename, boolean showGraph, boolean saveGraph) throws IOException {
    Graph graph = new MultiGraph("MLST");
    graph.addAttribute("ui.quality");
    graph.addAttribute("ui.antialias");

    nodes.forEach(node -> {
      String name = node.getName();
      graph.addNode(name);
      graph.getNode(name).addAttribute("ui.style", "shape:circle; fill-color: yellow; size: 30px; text-alignment: center; text-color: green; text-style: bold; text-size: 15;");
      graph.getNode(name).setAttribute("ui.label", name);
    });

    long id = 0;
    for (E edge : edges) {
      String label = edge.getLabel();
      String node1 = edge.getNode1().getName(), node2 = edge.getNode2().getName(), name = node1 + node2 + "E" + id;
      id++;
      graph.addEdge(name, node1, node2);
      if (spanningEdges.contains(edge)) {
        graph.getEdge(name).addAttribute("ui.style", "fill-color: red; size: 5px; text-alignment: center; text-color: green; text-style: bold; text-size: 12;");
      } else {
        graph.getEdge(name).addAttribute("ui.style", "fill-color: blue; size: 2px; text-alignment: center; text-color: green; text-size: 12;");
      }
      graph.getEdge(name).setAttribute("ui.label", label);
    }

    for (E edge : removedEdges) {
      String label = edge.getLabel();
      String node1 = edge.getNode1().getName(), node2 = edge.getNode2().getName(), name = node1 + node2 + "E" + id;
      id++;
      graph.addEdge(name, node1, node2);
      graph.getEdge(name).addAttribute("ui.style", "fill-color: black; size: 1px; text-alignment: center; text-color: green; text-size: 10; stroke-mode: dashes;");
      graph.getEdge(name).setAttribute("ui.label", label);
    }

    FileSinkImages pic = new FileSinkImages(OutputType.png, Resolutions.HD1080);
    pic.setLayoutPolicy(LayoutPolicy.COMPUTED_FULLY_AT_NEW_IMAGE);
    pic.setQuality(Quality.HIGH);
    pic.setRenderer(RendererType.SCALA);

    if (showGraph) {
      graph.display();
    }

    if (saveGraph) {
      pic.writeAll(graph, filename);
    }
  }

  public void save(String filename) throws IOException {
    try (PrintWriter printWriter = new PrintWriter(new FileWriter(filename))) {
      boolean f = true;
      for (N node : nodes) {
        printWriter.print((f ? "" : " ") + node.getName());
        if (f) {
          f = false;
        }
      }
      printWriter.println();
      edges.forEach(edge -> printWriter.println(edge.getLabel() + " " + edge.getNode1().getName() + " " + edge.getNode2().getName()));
    } catch (IOException ex) {
      throw ex;
    }
  }

  public void saveData(String filename) throws IOException {
    try (PrintWriter printWriter = new PrintWriter(new FileWriter(filename))) {
      printWriter.println("Nodes:");
      printWriter.println(nodes);
      printWriter.println();

      printWriter.println("Edges:");
      printWriter.println(edges);
      printWriter.println();

      printWriter.println("Labels:");
      printWriter.println(labels);
      printWriter.println();

      printWriter.println("Cost:");
      printWriter.println(labels.size());
      printWriter.println();

      printWriter.println("Spanning tree:");
      printWriter.println(spanningEdges);
      printWriter.println();

      printWriter.println("Removed edges:");
      printWriter.println(removedEdges);
      println();

      printWriter.println("Removed labels:");
      printWriter.println(removedLabels);
    } catch (IOException ex) {
      throw ex;
    }
  }

  public void printData() throws IOException {
    println(Ansi.ansi().format().fg(AnsiColor.YELLOW).format().bg(AnsiColor.BLUE).a("Nodes:").format().reset());
    println(nodes.toString());
    println();

    println(Ansi.ansi().format().fg(AnsiColor.YELLOW).format().bg(AnsiColor.BLUE).a("Edges:").format().reset());
    println(edges.toString());
    println();

    println(Ansi.ansi().format().fg(AnsiColor.YELLOW).format().bg(AnsiColor.BLUE).a("Labels:").format().reset());
    println(labels.toString());
    println();

    println(Ansi.ansi().format().fg(AnsiColor.YELLOW).format().bg(AnsiColor.BLUE).a("Cost:").format().reset());
    println(Ansi.ansi().format().boldOn().format().fg(AnsiColor.GREEN).a(labels.size()).format().reset());
    println();

    println(Ansi.ansi().format().fg(AnsiColor.YELLOW).format().bg(AnsiColor.BLUE).a("Spanning tree:").format().reset());
    println(spanningEdges.toString());
    println();

    println(Ansi.ansi().format().fg(AnsiColor.YELLOW).format().bg(AnsiColor.BLUE).a("Removed edges:").format().reset());
    println(removedEdges.toString());
    println();

    println(Ansi.ansi().format().fg(AnsiColor.YELLOW).format().bg(AnsiColor.BLUE).a("Removed labels:").format().reset());
    println(removedLabels.toString());
  }

  @Override
  public String toString() {
    return "Nodes: " + nodes + "; Edges: " + edges + "; Labels: " + labels + "; Spanning tree: " + spanningEdges;
  }
}
