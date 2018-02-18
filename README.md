# MLST
Minimum Labelling Spanning Tree
Solver for MLST problem.
### Supported algorithm:
* Alg1:
    1. Start with complete graph.
    2. Remove recurively one label and check if is connected:
        1. If is connected check if the cost of found graph is less than last saved graph, if TRUE update saved graph.
    3. Check if at k step the graph is connected:
        1. If TRUE then call recursively and remove another label.
<br />

* RAlg1:<br />
    + Dual of Alg1: start with empty graph and then add edges.
