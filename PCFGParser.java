package cs224n.assignment;

import cs224n.ling.Tree;
import cs224n.ling.Trees;
import java.util.*;

/**
 * The CKY PCFG Parser you will implement.
 */

class Triple{
    int split;
    String leftChild;
    String rightChild;
    
    public Triple(){
	this.split = -1;
	this.leftChild = "";
	this.rightChild = "";
    }

    public Triple(int split, String leftChild){
	this.split = split;
	this.leftChild = leftChild;
	this.rightChild = "";
    }

    public Triple(int split, String leftChild, String rightChild){
	this.split = split;
	this.leftChild = leftChild;
	this.rightChild = rightChild;
    }
}

public class PCFGParser implements Parser {
    private Grammar grammar;
    private Lexicon lexicon;
    private Map<String, Integer> nonterminals = new HashMap<String, Integer>();

    private void buildNonterminalMap(){
	// Get all Tags from the grammar
	Set<String> allTags = new HashSet<String>();
	allTags.addAll(lexicon.getAllTags());
	allTags.addAll(grammar.getAllTags());

	int idx = 0;
	for(String s : allTags){
	    nonterminals.put(s,idx);
	    idx++;
	}
    }



    public void train(List<Tree<String>> trainTrees) {
	List<Tree<String>> binarizedTrees = new ArrayList<Tree<String>>();
        // binarize the trees so that rules are at most binary
        for (Tree<String> tree : trainTrees){
            binarizedTrees.add(TreeAnnotations.annotateTree(tree));
	}
        lexicon = new Lexicon(binarizedTrees);
        grammar = new Grammar(binarizedTrees);
	buildNonterminalMap();
    }

    public Tree<String> getBestParse(List<String> sentence) {
	return TreeAnnotations.unAnnotateTree(CKY(sentence));
    }
    

	
    public void handleUnaries(double[][][] score, Triple[][][] back, int
			      i, int j) {
        // handle unaries, we already have probabilities in our cells
        boolean added = true;
        // keep applying unary rules until we stop discovering new constituents over a span
	// with better probabilities
	while (added) {
	    added = false;
	    // consider all unary rules in nonterminals
	    for (String child : nonterminals.keySet()) {
		if(score[i][j][nonterminals.get(child)] <= 0)
		    continue;
		List<Grammar.UnaryRule> rules =
			      grammar.getUnaryRulesByChild(child);
		for (Grammar.UnaryRule r : rules) {
		    String parent = r.getParent();
		    double prob = r.getScore()*score[i][j][nonterminals.get(child)];
		    // if it's a better probability, store it in
		    //the back trace			    
		    if (prob > score[i][j][nonterminals.get(parent)]){
			score[i][j][nonterminals.get(parent)] = prob;
			back[i][j][nonterminals.get(parent)] = new
			    Triple(-1, child);
			// if we've done work, do another iteration
			added = true;
		    }		    
		}
	    }
	}
    }


    private Tree<String> CKY(List<String> words) {
	int numWords = words.size();
	double[][][] score = new double[numWords+1][numWords+1][nonterminals.keySet().size()];
	Triple[][][] back = new
	    Triple[numWords+1][numWords+1][nonterminals.keySet().size()];
	
	for(int i=0; i < numWords; i++){
	    for (String symbol : lexicon.getAllTags()){
		score[i][i + 1][nonterminals.get(symbol)] =
		    lexicon.scoreTagging(words.get(i), symbol);
	    }
	    handleUnaries(score, back, i, i+1);
	}

	for (int span = 2; span <= numWords; span++) {
	    for (int begin = 0; begin <= numWords-span; begin++) {
		int end = begin+span;
		for (int split = begin+1; split <= end-1; split++){
		    for (String tag : nonterminals.keySet()){
			if(score[begin][split][nonterminals.get(tag)] <= 0) continue;
			List<Grammar.BinaryRule> binaryRules =
			    grammar.getBinaryRulesByLeftChild(tag);
			for (Grammar.BinaryRule r : binaryRules){
			    if(Double.isNaN(score[begin][split][nonterminals.get(r.getLeftChild())])   ||
			       Double.isNaN(score[split][end][nonterminals.get(r.getRightChild())]))
				System.exit(-1);
			    double prob =
				score[begin][split][nonterminals.get(r.getLeftChild())]
				* score[split][end][nonterminals.get(r.getRightChild())] * r.getScore();
			    if (prob > score[begin][end][nonterminals.get(r.getParent())]){
				score[begin][end][nonterminals.get(r.getParent())]
				    = prob;
				back[begin][end][nonterminals.get(r.getParent())]
				    = new Triple(split, r.getLeftChild(), r.getRightChild());
			    }
			}
		    }
		}
		handleUnaries(score, back, begin, end);
	    }
	}
	return buildParseTree(words, score, back, 0, score.length-1, "ROOT");
    }

    private Tree<String> buildParseTree(List<String> words, double[][][]
					score, Triple[][][] back, int i,
					int j, String tag){
	Triple S = back[i][j][nonterminals.get(tag)];
	if (S == null) // Preterminal Node
	    return new Tree<String>(tag, Collections.singletonList(new
								   Tree<String>(words.get(i))));
	if (S.split == -1) // Unary rule
	    return new Tree<String>(tag,
					Collections.singletonList(buildParseTree(words,
										 score,
										 back,
										 i,
										 j,
										 S.leftChild)));
	// Binary rule
	List<Tree<String>> children = new ArrayList<Tree<String>>();
	children.add(buildParseTree(words, score, back, i, S.split,
				    S.leftChild));
	children.add(buildParseTree(words, score, back, S.split, j,
				    S.rightChild));
	return new Tree<String>(tag, children);
    }
}
