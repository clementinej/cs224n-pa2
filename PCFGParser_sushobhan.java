package cs224n.assignment;

import cs224n.ling.Tree;
import java.util.*;

/**
 * The CKY PCFG Parser you will implement.
 */
public class PCFGParser implements Parser {
    private Grammar grammar;
    private Lexicon lexicon;
    private Map<String, Integer> nonterminals;

    private void buildNonterminalMap(){
	// Get all Tags from the grammar
	Set<String> allTags = lexicon.getAllTags();
	allTags.addAll(grammar.getAllTags());
	// for (BinaryRule r : grammar.binaryRulesByLeftChild().keySet()){
	//     allTags.add(r.getParent());
	//     allTags.add(r.getLeftChild());
	//     allTags.add(r.getRightChild());
	// }

	// for (UnaryRule u : grammar.unaryRulesByChild().keySet()) {
	//     allTags.add(u.getParent());
	//     allTags.add(u.getChild());
	// }

	int idx = 0;
	for(String s : allTags)
	    nonterminals.put(s,idx++);
    }



    public void train(List<Tree<String>> trainTrees) {
	List<Tree<String>> binarizedTrees = new ArrayList<Tree<String>>();
        // binarize the trees so that rules are at most binary
        for (Tree<String> tree : trainTrees)
            binarizedTrees.add(TreeAnnotations.annotateTree(tree));
        // System.out.println(trainTrees.toString());
        // lexicon contains the preterminals and the words
        lexicon = new Lexicon(binarizedTrees);
        grammar = new Grammar(binarizedTrees);
	buildNonterminalMap();
    }

    public Tree<String> getBestParse(List<String> sentence) {
        // TODO: implement this method
        return null;
    }

    public void handleUnaries(double[][][] score, Triplet[][][] back, int
			      i, int j) {
        // handle unaries, we already have probabilities in our cells
        boolean added = true;
        // keep applying unary rules until we stop discovering new constituents over a span
	// with better probabilities
	while (added) {
	    added = false;
	    // consider all unary rules in nonterminals
	    for (String child : nonterminals.keySet()) {
		List<UnaryRule> rules = grammar.getUnaryRulesByChild(child);
		for (UnaryRule r : rules) {
		    String parent = r.getParent();
		    if (((score[i][j][nonterminals.get(r.getChild())]) > 0)) {
			// returns a smoothed estimate of P(word|tag)
			double prob = r.getScore()*score[i][j][nonterminals.get(r.getChild())];
			// if it's a better probability, store it in
			//the back trace
			if (prob > score[i][j][nonterminals.get(r.getParent())]){
			    score[i][j][nonterminals.get(r.getParent())] = prob;
			    //TODO: back[i][j][r.parent] = B
			    // if we've done work, do another iteration
			    added = true;
			}
		    }
		}
	    }
	}
    }


    private Tree<String> cky(List<String> words) {
	int numWords = words.size();
	double[][][] score = new double[numWords+1][numWords+1][nonterminals.keySet().size()];
	//TODO: back[][][] implementation

	for(int i=0; i < numWords; i++){
	    for (String symbol : lexicon.getAllTags())
		score[i][i + 1][nonterminals.get(symbol)] =
		    lexicon.scoreTagging(words.get(i), symbol);
	    handleUnaries(score, back, i, i+1);
	}

	for (int span = 2; span <= numWords; span++) {
	    for (int begin = 0; begin <= numWords-span; begin++) {
		int end = begin+span;
		for (int split=begin+1; split <= end-1; split++){
		    Set<BinaryRule> binaryRules = new HashSet<BinaryRule>();
		    // loop over the possible left children
                    for (int i = 0; i < score[begin][split].length; i++)
                        if (score[begin][split][i] != 0) 
			    binaryRules.addAll(grammar.getBinaryRulesByLeftChild(nonterminals.get(i)));
		    for (BinaryRule r : binaryRules){
			double prob =
			    score[begin][split][nonterminals.get(r.leftChild)]
			    * score[split][end][r.rightChild] * r.getScore();
			if (prob > score[begin][end][nonterminals.get(r.parent)]){
			    score[begin][end][nonterminals.get(r.parent)]
				= prob;
			    // TODO: back pointer
			}
		    }
		}
		handleUnaries(score, back, begin, end);
	    }
	}
	return buildTree(score, back);
    }

