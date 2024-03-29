package cs224n.assignment;

import cs224n.ling.Tree;
import java.util.*;

/**
 * The CKY PCFG Parser you will implement.
 */

class Triple{
    int split, left, right;
    public Triple(){
	this.split = -1;
	this.left = -1;
	this.right = -1;
    }

    public Triple(int split, int left, int right){
	this.split = split;
	this.left = left;
	this.right = right;
    }

}

public class PCFGParser implements Parser {
    private Grammar grammar;
    private Lexicon lexicon;
    private Map<String, Integer> nonterminals;
    private Map<Integer, String> reverseNonterminals;

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
	for(String s : allTags){
	    nonterminals.put(s,idx);
	    reverseNonterminals.put(idx, s);
	}
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
	return new Tree<String>("ROOT", Collections.singletonList(CKY(sentence)));
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
		List<UnaryRule> rules = grammar.getUnaryRulesByChild(child);
		for (UnaryRule r : rules) {
		    String parent = r.getParent();
		    if (score[i][j][nonterminals.get(child)] > 0) {
			// returns a smoothed estimate of P(word|tag)
			double prob = r.getScore()*score[i][j][nonterminals.get(child)];
			// if it's a better probability, store it in
			//the back trace
			if (prob > score[i][j][nonterminals.get(parent)]){
			    score[i][j][nonterminals.get(parent)] = prob;
			    back[i][j][nonterminals.get(parent)].left = nonterminals.get(child);
			    // if we've done work, do another iteration
			    added = true;
			}
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
	for (int i = 0; i < numWords+1; i++)
	    	for (int j = 0; j < numWords+1; j++)
		    	for (int k = 0; k < nonterminals.keySet().size(); k++)
			    back[i][j][k] = new Triple();
	
	for(int i=0; i < numWords; i++){
	    for (String symbol : lexicon.getAllTags())
		score[i][i + 1][nonterminals.get(symbol)] =
		    lexicon.scoreTagging(words.get(i), symbol);
	    handleUnaries(score, back, i, i+1);
	}

	for (int span = 2; span <= numWords; span++) {
	    for (int begin = 0; begin <= numWords-span; begin++) {
		int end = begin+span;
		for (int split = begin+1; split <= end-1; split++){
		    // Set<BinaryRule> binaryRules = new HashSet<BinaryRule>();
		    // // loop over the possible left children
                    // for (int i = 0; i < score[begin][split].length; i++)
                    //     if (score[begin][split][i] != 0) 
		    // 	    binaryRules.addAll(grammar.getBinaryRulesByLeftChild(nonterminals.get(i)));
		    for (String tag : nonterminals.keySet()){
			List<BinaryRule> binaryRules =
			    grammar.getBinaryRulesByLeftChild(tag);
			if (binaryRules == null) continue;
			for (BinaryRule r : binaryRules){
			    double prob =
				score[begin][split][nonterminals.get(r.getLeftChild())]
				* score[split][end][r.getRightChild()] * r.getScore();
			    if (prob > score[begin][end][nonterminals.get(r.getParent())]){
				score[begin][end][nonterminals.get(r.getParent())]
				    = prob;
				back[begin][end][nonterminals.get(r.getParent())]
				    = new Triple(split,
						 nonterminals.get(r.getLeftChild()), nonterminals.get(r.getRightChild())); 
			    }
			}
		    }
		}
		handleUnaries(score, back, begin, end);
	    }
	}
	return buildParseTree(words, score, back, 0, back.length-1, "S");
    }

    private Tree<String> buildParseTree(List<String> words, int[][][]
					score, Triple[][][] back, int i,
					int j, String tag){
	Triple S = back[i][j][nonterminals.get(tag)];
	if (S.split == -1){
	    if (S.left == -1) //Pre-Terminal node
		return new Tree<String>(tag, Collections.singletonList(new
								       Tree<String>(words.get(i))));
	    else //Unary rule
		return new Tree<String>(tag,
					Collections.singletonList(buildParseTree(words,
										 score,
										 back,
										 i,
										 j,
										 reverseNonterminals.get(S.left))));
	}else{ // Binary rule
	    List<Tree<String>> children = new ArrayList<Tree<String>>();
	    // Add left subtree first, then right
	    children.add(buildParseTree(words, score, back, i, S.split,
					reverseNonterminals.get(S.left)));
	    children.add(buildParseTree(words, score, back, S.split, j,
					reverseNonterminals.get(S.right)));
	    return new Tree<String>(tag, children);
	}
    }
}
