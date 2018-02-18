# MLST
Minimum Labelling Spanning Tree
Solver for MLST problem: actual supported algorithm:
* Alg1:
    Start with complete graph.
    Remove recurively one label and check if is connected:
        If is connected check if the cost of found graph is less than last saved graph, if TRUE update saved graph.
    Check if at k step the graph is connected:
        If TRUE then call recursively and remove another label.
* RAlg1:
	Dual of Alg1: start with empty graph and then add edges.
