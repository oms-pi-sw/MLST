# MLST [![build status](http://minegrado.ovh/badges/build-passing-brightgreen.svg)](https://github.com/oms-pi-sw/MLST) [![license](http://minegrado.ovh/badges/license-MIT-blue.svg)](LICENSE) [![version](http://minegrado.ovh/badges/version-0.10.5-yellow.svg)](#) [![release](http://minegrado.ovh/badges/release-BETA-yellow.svg)](#)
## Minimum Labelling Spanning Tree:
Solver for MLST problem.

## Download binary:
### [REPOSITORY](http://minegrado.ovh/DWN/MLST)
**MLST-bin** *.tar.xz* or *.zip* is binary updated archive.
* [tar.xz](http://minegrado.ovh/DWN/MLST/MLST-bin.tar.xz)
* [zip](http://minegrado.ovh/DWN/MLST/MLST-bin.zip)

## Supported algorithm:
* TopDown: (Branch&Bound Top-Down)
    + Destructive algorithm.
    1. Start with complete graph.
    2. Remove recurively one label and check if is connected:
        1. If is connected check if the cost of found graph is less than last saved graph, if TRUE update saved graph.
    3. Check if at k step the graph is connected:
        1. If TRUE then call recursively and remove another label.
<br />

* BottomUp: (Branch&Bound Bottom-Up) [![algorithm](http://minegrado.ovh/badges/algorithm-exact-red.svg)](#) [![threads](http://minegrado.ovh/badges/threads-single-orange.svg)](#)<br />
    + Constructive algorithm.
    + Dual of BottomUp: start with empty graph and then add edges.
<br />

* BottomUpT: (Branch&Bound Bottom-Up with Threads) [![algorithm](http://minegrado.ovh/badges/algorithm-exact-red.svg)](#) [![threads](http://minegrado.ovh/badges/threads-multi-orange.svg)](#)<br />
    + Version of BottomUp with multithreading optimization.
<br />

* MVCA: [![algorithm](http://minegrado.ovh/badges/algorithm-heuristic-red.svg)](#) [![threads](http://minegrado.ovh/badges/threads-single-orange.svg)](#)<br />
    + Maximum Vertex Cover Algorithm. **Heuristic** *greedy* algorithm.
<br />

* MVCAO1: [![algorithm](http://minegrado.ovh/badges/algorithm-heuristic-red.svg)](#) [![threads](http://minegrado.ovh/badges/threads-single-orange.svg)](#)<br />
    + Variant of Maximum Vertex Cover Algorithm. **Heuristic** *greedy* algorithm.
<br />

* ERA: [![algorithm](http://minegrado.ovh/badges/algorithm-heuristic-red.svg)](#) [![threads](http://minegrado.ovh/badges/threads-single-orange.svg)](#)<br />
    + Edge Replacement Algorithm. **Heuristic** *local search* algorithm.
<br />

* TABU SEARCH: [![algorithm](http://minegrado.ovh/badges/algorithm-heuristic-red.svg)](#) [![threads](http://minegrado.ovh/badges/threads-multi-orange.svg)](#)<br />
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
 -a,--algorithm <algorithm>                         Choose algorithm, you
                                                    can select more
                                                    algorithm at same time
                                                    separating them with a
                                                    comma:
                                                    * topdown [EXACT]
                                                    * bottomup [EXACT]
                                                    * bottomupt [EXACT,
                                                    MULTITHREADED]
                                                    * mvca [HEURISTIC,
                                                    GREEDY]
                                                    * mvcao1 [HEURISTIC,
                                                    GREEDY]
                                                    * era [HEURISTIC]
                                                    * tabusearch
                                                    [HEURISTIC]
 -A,--intensification-learning                      [REQUIRE TABU SEARCH].
                                                    Use a "good moves"
                                                    list to learn best
                                                    moves and evaluate
                                                    after exploration of
                                                    neighborhood.
                                                    [INTENSIFICATION
                                                    OPTION]
 -B,--diversification-learning                      [REQUIRE TABU SEARCH].
                                                    Use a "frequently
                                                    moves" list to learn
                                                    too frequent moves and
                                                    reject after
                                                    exploration of
                                                    neighborhood.
                                                    [DIVERSIFICATION
                                                    OPTION]
 -g,--graph <graph>                                 Prefix of filenames
                                                    where save graphs.
 -h,--help                                          Print help.
 -I,--max-iter <max-iter>                           [REQUIRE TABU SEARCH].
                                                    Specify the max number
                                                    of iterations.
 -i,--input <input>                                 The input graph file.
 -L,--local-search-intensification                  [REQUIRE TABU SEARCH].
                                                    Use intensification
                                                    technique with local
                                                    search.
                                                    [INTENSIFICATION
                                                    OPTION]
 -M,--multistart <multistart>                       [REQUIRE TABU SEARCH].
                                                    Specify the number of
                                                    multistart: default
                                                    use greedy to find
                                                    start. You can specify
                                                    which starts ('g'
                                                    suffix) are greedy and
                                                    which are random ('r'
                                                    suffix): -M<x>g,<y>r.
                                                    [DIVERSIFICATION
                                                    OPTION]
    --max-equivalent-moves <max-equivalent-moves>   [REQUIRE TABU SEARCH
                                                    AND DIVERSIFICATION].
                                                    Set equivalent moves
                                                    number that define a
                                                    stall.
                                                    [DIVERSIFICATION
                                                    OPTION]
    --min-nonrandom-iter <min-nonrandom-iter>       [REQUIRE TABU SEARCH
                                                    AND DIVERSIFICATION].
                                                    Set minimum non random
                                                    iteration that follow
                                                    random iterations.
                                                    [DIVERSIFICATION
                                                    OPTION]
 -n,--nograph                                       Don't open graphic
                                                    interface for graph.
 -N,--no-improvement <no-improvement>               [REQUIRE TABU SEARCH].
                                                    Specify the max number
                                                    of iterations without
                                                    improvement.
 -o,--output <output>                               The output filename
                                                    where to save the
                                                    graph.
 -P,--path-relinking                                [REQUIRE TABU SEARCH].
                                                    Require to use path
                                                    relinking
                                                    intensification
                                                    heuristic after tabu
                                                    search collected all
                                                    elite results.
                                                    [INTENSIFICATION
                                                    OPTION]
    --path-relinking-desc                           [REQUIRE TABU SEARCH
                                                    AND PATH RELINKING].
                                                    Order elites in
                                                    descendent mode and
                                                    not in random default
                                                    mode. [INTENSIFICATION
                                                    OPTION]
    --path-relinking-min                            [REQUIRE TABU SEARCH
                                                    AND PATH RELINKING].
                                                    Always explore from
                                                    elite that cost more
                                                    to the cheaper.
                                                    [INTENSIFICATION
                                                    OPTION]
 -Q,--min-queue <min-queue>                         [REQUIRE TABU SEARCH].
                                                    Specify the max queue
                                                    of forbidden moves in
                                                    Tabu Search.
 -R,--move-diversification                          [REQUIRE TABU SEARCH].
                                                    Specify a stronger
                                                    policy for tabu queue
                                                    rejection in order to
                                                    diversify more.
                                                    [DIVERSIFICATION
                                                    OPTION]
 -r,--random                                        Generate random graph.
    --random-iter <random-iter>                     [REQUIRE TABU SEARCH
                                                    AND DIVERSIFICATION].
                                                    Set random iteration
                                                    during
                                                    diversification.
                                                    [DIVERSIFICATION
                                                    OPTION]
 -S,--path-relinking-alt                            [REQUIRE TABU SEARCH].
                                                    Require to use path
                                                    relinking alternative
                                                    algorithm (simplier
                                                    and faster)
                                                    intensification
                                                    heuristic after tabu
                                                    search collected all
                                                    elite results.
                                                    [INTENSIFICATION
                                                    OPTION]
 -t,--threads <threads>                             [REQUIRE
                                                    MULTITHREADING
                                                    ALGORITHM]. Specify
                                                    number of threads.
                                                    Works only for
                                                    multithreading
                                                    algorithms.
 -v,--version                                       Print version.
    --verbose                                       Enable verbose
                                                    modality.
 -X,--intensification                               [REQUIRE TABU SEARCH].
                                                    While you are choosing
                                                    "improvements moves"
                                                    abolish temporarily
                                                    tabu queue.
                                                    [INTENSIFICATION
                                                    OPTION]
 -Y,--diversification                               [REQUIRE TABU SEARCH].
                                                    Start to diversificate
                                                    when stall is
                                                    detected.
                                                    [DIVERSIFICATION
                                                    OPTION]
```

## UI: [![GraphStream](http://minegrado.ovh/badges/dependecy-GraphStream-blue.svg)](http://graphstream-project.org/)
Graphic interface use GraphStream library.

## WINDOWS: [![ansicon](http://minegrado.ovh/badges/dependecy-ansicon-blue.svg)](https://github.com/adoxa/ansicon)
For windows you need to run the console with a program like *ansicon*.

## Version:
v0.10.5 BETA

## TODO:
* Add a mixed exact algorith that use **Multithreaded Bottom-Up** *with an Heuristic Algorithm*, to find an *upper bound* for minimum graph cost.
