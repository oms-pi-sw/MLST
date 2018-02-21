# MLST [![build status](http://minegrado.ovh/badges/build-passing-brightgreen.svg)](https://github.com/oms-pi-sw/MLST) [![license](http://minegrado.ovh/badges/license-MIT-blue.svg)](LICENSE)
## Minimum Labelling Spanning Tree:
Solver for MLST problem.

## Download binary:
### [REPOSITORY](http://minegrado.ovh/DWN/MLST)
**MLST-bin** *.tar.xz* or *.zip* is binary updated archive.
* [tar.xz](http://minegrado.ovh/DWN/MLST/MLST-bin.tar.xz)
* [zip](http://minegrado.ovh/DWN/MLST/MLST-bin.zip)

## Supported algorithm:
* BBTopDown: (Branch&Bound Top-Down)
    1. Start with complete graph.
    2. Remove recurively one label and check if is connected:
        1. If is connected check if the cost of found graph is less than last saved graph, if TRUE update saved graph.
    3. Check if at k step the graph is connected:
        1. If TRUE then call recursively and remove another label.
<br />

* BottomUp: (Branch&Bound Bottom-Up)<br />
    + Dual of Alg1: start with empty graph and then add edges.
<br />

* BottomUpT: (Branch&Bound Bottom-Up with Threads)<br />
    + Version of RAlg1 with multithreading optimization.
<br />

* MVCA:<br />
    + Maximum Vertex Cover Algorithm. **Heuristic**
<br />

* MVCAO1:<br />
    + Variant of Maximum Vertex Cover Algorithm. **Heuristic**
<br />

* ERA:<br />
    + Edge Replacement Algorithm. **Heuristic**
<br />

## Example:
### Random graph
```
MLST.run -r -abottomupt,mvca -ograph -gimage
```

Generate a random graph with **nnodes** nodes, *max* **nlabels** labels, **nedges** edges and then solve MLST problem with given algorithms.

### Input graph
```
MLST.run -iinput -abottomupt,mvca -ograph -gimage
```

Parse **input** file and create graph and then solve MLST problem with given algorithms.

## HELP:
```
usage: MLST
 -a,--algorithm <algorithm>   Choose algorithm, you can select more
                              algorithm at same time separating them with
                              a comma:
                              * topdown [EXACT]
                              * bottomup [EXACT]
                              * bottomupt [EXACT, MULTITHREADED]
                              * mvca [HEURISTIC, GREEDY]
                              * mvcao1 [HEURISTIC, GREEDY]
                              * era [HEURISTIC]
 -g,--graph <graph>           Prefix of filenames where save graphs.
 -h,--help                    Print help.
 -i,--input <input>           The input graph file.
 -n,--nograph                 Don't open graphic interface for graph.
 -o,--output <output>         The output filename where to save the graph.
 -r,--random                  Generate random graph.
 -t,--threads <threads>       Specify number of threads. Works only for
                              multithreading algorithms.
 -v,--version                 Print version.
    --verbose                 Enable verbose modality.
```

## UI: [![GraphStream](http://minegrado.ovh/badges/dependecy-GraphStream-blue.svg)](http://graphstream-project.org/)
Graphic interface use GraphStream library.

## WINDOWS: [![ansicon](http://minegrado.ovh/badges/dependecy-ansicon-blue.svg)](https://github.com/adoxa/ansicon)
For windows you need to run the console with a program like *ansicon*.

## TODO:
* Add a mixed exact algorith that use **Multithreaded Bottom-Up** *with an Heuristic Algorithm*, to find an *upper bound* for minimum graph cost.
* Tabu heuristic algorithm and Path Relinking.
