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
    + Maximum Vertex Cover Algorithm. **Heuristic** *greedy* algorithm.
<br />

* MVCAO1:<br />
    + Variant of Maximum Vertex Cover Algorithm. **Heuristic** *greedy* algorithm.
<br />

* ERA:<br />
    + Edge Replacement Algorithm. **Heuristic** *local search* algorithm.
<br />

* TABU SEARCH:<br />
    + Tabu Search Algorithm. **Heuristic**. Use **MVCAO1** *greedy* to generate an initial solution then try to improve it with *Tabu Search*.<br />**TODO**: use *Path Relinking* as intensification method and *Multi Start* as difersification method.

## Taboo Search:
It's an *heuristic* algorithm that start from a feasible solution and, in a loop, search for another solution in the defined neighborhood.<br />

### Intensification strategies
* A first intensification strategy is to start a *local search* with **ERA** algorithm to improve the solution just found.
* Another intensification is to keep a *best moves* list and evaluate next moves using this list.
* The last proposed way is run a *Path Relinking* algorithm at the end of all *Tabu Search* starts.

### Diversification strategies
* A first simple diversification strategy is achieved starting Tabu Search many times using different initial graph.
* Another difersification technique is to *harden* moves comparison criteria, to reject more moves.
* The last proposed diversification technique is to keep a long memory moves list.  

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
 -a,--algorithm <algorithm>             Choose algorithm, you can select
                                        more algorithm at same time
                                        separating them with a comma:
                                        * topdown [EXACT]
                                        * bottomup [EXACT]
                                        * bottomupt [EXACT, MULTITHREADED]
                                        * mvca [HEURISTIC, GREEDY]
                                        * mvcao1 [HEURISTIC, GREEDY]
                                        * era [HEURISTIC]
                                        * tabusearch [HEURISTIC]
 -A,--intensification                   To USE WITH Tabu Search heuristic
                                        algorithm. Use a "good moves" list
                                        to learn best moves and evaluate
                                        after exploration of neighborhood.
                                        [INTENSIFICATION OPTION]
 -B,--diversification                   To USE WITH Tabu Search heuristic
                                        algorithm. Use a "frequently
                                        moves" list to learn too frequent
                                        moves and reject after exploration
                                        of neighborhood. [DIVERSIFICATION
                                        OPTION]
 -g,--graph <graph>                     Prefix of filenames where save
                                        graphs.
 -h,--help                              Print help.
 -I,--max-iter <max-iter>               To USE WITH Tabu Search heuristic
                                        algorithm. Specify the max number
                                        of iterations.
 -i,--input <input>                     The input graph file.
 -L,--local-search-intensification      To USE WITH Tabu Search heuristic
                                        algorithm. Require to use path
                                        relinking intensification
                                        heuristic after tabu search
                                        collected all elite results.
                                        [INTENSIFICATION OPTION]
 -m,--min-queue <min-queue>             To USE WITH Tabu Search heuristic
                                        algorithm. Specify the max queue
                                        of forbidden moves in Tabu Search.
 -M,--multistart <multistart>           To USE WITH Tabu Search heuristic
                                        algorithm. Specify the number of
                                        multistart. [DIVERSIFICATION
                                        OPTION]
 -n,--nograph                           Don't open graphic interface for
                                        graph.
 -N,--no-improvement <no-improvement>   To USE WITH Tabu Search heuristic
                                        algorithm. Specify the max number
                                        of iterations without improvement.
 -o,--output <output>                   The output filename where to save
                                        the graph.
 -P,--path-relinking                    To USE WITH Tabu Search heuristic
                                        algorithm. Require to use path
                                        relinking intensification
                                        heuristic after tabu search
                                        collected all elite results.
                                        [INTENSIFICATION OPTION]
    --path-relinking-min                To USE WITH Tabu Search heuristic
                                        algorithm. Require to use path
                                        relinking alternative algorithm
                                        (simplier and faster)
                                        intensification heuristic after
                                        tabu search collected all elite
                                        results. [INTENSIFICATION OPTION]
 -R,--move-diversification              To USE WITH Tabu Search heuristic
                                        algorithm. Specify a stronger
                                        policy for tabu queue rejection in
                                        order to diversify more.
                                        [DIVERSIFICATION OPTION]
 -r,--random                            Generate random graph.
 -S,--path-relinking-alt                To USE WITH Tabu Search heuristic
                                        algorithm. Require to use path
                                        relinking alternative algorithm
                                        (simplier and faster)
                                        intensification heuristic after
                                        tabu search collected all elite
                                        results. [INTENSIFICATION OPTION]
 -t,--threads <threads>                 To USE WITH Tabu Search heuristic
                                        algorithm or Bottom-Up
                                        MultiThreaded algorithm. Specify
                                        number of threads. Works only for
                                        multithreading algorithms.
 -v,--version                           Print version.
    --verbose                           Enable verbose modality.
```

## UI: [![GraphStream](http://minegrado.ovh/badges/dependecy-GraphStream-blue.svg)](http://graphstream-project.org/)
Graphic interface use GraphStream library.

## WINDOWS: [![ansicon](http://minegrado.ovh/badges/dependecy-ansicon-blue.svg)](https://github.com/adoxa/ansicon)
For windows you need to run the console with a program like *ansicon*.

## TODO:
* Add a mixed exact algorith that use **Multithreaded Bottom-Up** *with an Heuristic Algorithm*, to find an *upper bound* for minimum graph cost.
* Tabu heuristic algorithm and Path Relinking. *Already work in progress*.
