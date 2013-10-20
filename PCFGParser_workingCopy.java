package cs224n.assignment;

import cs224n.ling.Tree;
import cs224n.ling.Trees;
import java.util.*;

/**
 * The CKY PCFG Parser you will implement.
 */

// class Triple{
//     int split, left, right;
//     public Triple(){
// 	this.split = -1;
// 	this.left = -1;
// 	this.right = -1;
//     }

//     public Triple(int split, int left, int right){
// 	this.split = split;
// 	this.left = left;
// 	this.right = right;
//     }
// }

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
    // private Map<Integer, String> reverseNonterminals = new
    // 	HashMap<Integer, String>();

    private void buildNonterminalMap(){
	// Get all Tags from the grammar
	Set<String> allTags = new HashSet<String>();
	allTags.addAll(lexicon.getAllTags());
	allTags.addAll(grammar.getAllTags());
	for (String s : allTags) System.out.print(s + "\t");
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
	    //reverseNonterminals.put(idx, s);
	    idx++;
	}
	
	// for(String s : nonterminals.keySet()){
	//     System.out.println(s + " " + nonterminals.get(s));
	//     System.out.println(lexicon.scoreTagging("cat", s));
	// }
	
	// System.out.println(grammar);
    }



    public void train(List<Tree<String>> trainTrees) {
	List<Tree<String>> binarizedTrees = new ArrayList<Tree<String>>();
        // binarize the trees so that rules are at most binary
        for (Tree<String> tree : trainTrees){
	    // System.out.println(Trees.PennTreeRenderer.render(tree));
            binarizedTrees.add(TreeAnnotations.annotateTree(tree));
	}
	// for (Tree<String> tree : binarizedTrees){
	//     System.out.println(Trees.PennTreeRenderer.render(tree));
	//     // System.out.println(Trees.PennTreeRenderer.render(TreeAnnotations.unAnnotateTree(tree)));
	// }
        // System.out.println(trainTrees.toString());
        // lexicon contains the preterminals and the words
        lexicon = new Lexicon(binarizedTrees);
        grammar = new Grammar(binarizedTrees);
	buildNonterminalMap();
	// System.out.println(grammar);
    }

    public Tree<String> getBestParse(List<String> sentence) {
	return TreeAnnotations.unAnnotateTree(new Tree<String>("ROOT", Collections.singletonList(CKY(sentence))));
    }
    
    // private void printScoreBack(double[][][] score, Triple[][][] back, int
    // 				i, int j){
    // 	System.out.printf("Printing score and back at %d %d\n", i, j);
    // 	for (int k=0; k < score[i][j].length; k++){
    // 	    if(Double.isNaN(score[i][j][k])){
    // 		System.out.println("NAN Value: exiting.....");
    // 		System.exit(-1);
    // 	    }
    // 	    System.out.println(reverseNonterminals.get(k)+" "+score[i][j][k]);
    // 	}
    // 	for (int k=0; k < score[i][j].length; k++){
    // 	    if (back[i][j][k].split == -1 && back[i][j][k].left == -1)
    // 		System.out.println("back: "+ reverseNonterminals.get(k)+" -1 -1 -1");
    // 	    else if (back[i][j][k].split == -1)
    // 		System.out.println("back: "+ reverseNonterminals.get(k) +
    // 				   " " +
    // 				   reverseNonterminals.get(back[i][j][k].left));
    // 	    else
    // 		System.out.println("back: " + reverseNonterminals.get(k)
    // 				   + "->" +
    // 				reverseNonterminals.get(back[i][j][k].left)
    // 				   + " " +
    // 				reverseNonterminals.get(back[i][j][k].right));
    // 	}
    // }


	
    public void handleUnaries(double[][][] score, Triple[][][] back, int
			      i, int j) {
        // handle unaries, we already have probabilities in our cells
	// System.out.printf("Handling unaries at cell %d %d\n", i, j);
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
		    // System.out.println("At rule: " + r);
		    // System.out.println("score: " + score[i][j][nonterminals.get(child)]);
		    //if (score[i][j][nonterminals.get(child)] > 0) {
		    // System.out.println("Considering rule:" + r);
		    // returns a smoothed estimate of P(word|tag)
		    double prob = r.getScore()*score[i][j][nonterminals.get(child)];
		    // if it's a better probability, store it in
		    //the back trace			    
		    if (prob > score[i][j][nonterminals.get(parent)]){
			score[i][j][nonterminals.get(parent)] = prob;
			//back[i][j][nonterminals.get(parent)].left = nonterminals.get(child);
			back[i][j][nonterminals.get(parent)] = new
			    Triple(-1, child);
			// if we've done work, do another iteration
			added = true;
		    }		    
		}
	    }
	}
	// printScoreBack(score, back, i, j);
    }


    private Tree<String> CKY(List<String> words) {
	int numWords = words.size();
	double[][][] score = new double[numWords+1][numWords+1][nonterminals.keySet().size()];
	Triple[][][] back = new
	    Triple[numWords+1][numWords+1][nonterminals.keySet().size()];
	// for (int i = 0; i < numWords+1; i++)
	//     	for (int j = 0; j < numWords+1; j++)
	// 	    	for (int k = 0; k < nonterminals.keySet().size(); k++)
	// 		    back[i][j][k] = new Triple();
	
	for(int i=0; i < numWords; i++){
	    // System.out.println("At cell: "+ i +" " + (i+1));
	    for (String symbol : lexicon.getAllTags()){
		score[i][i + 1][nonterminals.get(symbol)] =
		    lexicon.scoreTagging(words.get(i), symbol);
		// System.out.println(symbol + " " + words.get(i)+ " " +
		// 		   lexicon.scoreTagging(words.get(i), symbol));
	    }
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
			if(score[begin][split][nonterminals.get(tag)] <= 0) continue;
			List<Grammar.BinaryRule> binaryRules =
			    grammar.getBinaryRulesByLeftChild(tag);
			//if (binaryRules == null) continue;
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
				// back[begin][end][nonterminals.get(r.getParent())]
				//     = new Triple(split,
				// 		 nonterminals.get(r.getLeftChild()), nonterminals.get(r.getRightChild())); 
				back[begin][end][nonterminals.get(r.getParent())]
				    = new Triple(split, r.getLeftChild(), r.getRightChild());
			    }
			}
		    }
		    //handleUnaries(score, back, begin, end);
		}
		handleUnaries(score, back, begin, end);
	    }
	}
	return buildParseTree(words, score, back, 0, score.length-1, "S");
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
	// if (S.split == -1){
	//     if (S.left == -1) //Pre-Terminal node
	// 	return new Tree<String>(tag, Collections.singletonList(new
	// 							       Tree<String>(words.get(i))));
	//     else //Unary rule
	// 	return new Tree<String>(tag,
	// 				Collections.singletonList(buildParseTree(words,
	// 									 score,
	// 									 back,
	// 									 i,
	// 									 j,
	// 									 reverseNonterminals.get(S.left))));
	// }else{ // Binary rule
	//     List<Tree<String>> children = new ArrayList<Tree<String>>();
	//     // Add left subtree first, then right
	//     children.add(buildParseTree(words, score, back, i, S.split,
	// 				reverseNonterminals.get(S.left)));
	//     children.add(buildParseTree(words, score, back, S.split, j,
	// 				reverseNonterminals.get(S.right)));
	//     return new Tree<String>(tag, children);
	// }
    }
}
