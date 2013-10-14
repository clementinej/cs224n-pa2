package cs224n.assignment;

import cs224n.ling.Tree;

import java.util.*;
import cs224n.util.*;

/**
 * The CKY PCFG Parser you will implement.
 */
public class PCFGParser implements Parser {
    private Grammar grammar;
    private Lexicon lexicon;

    // build the grammar and its probabilities
    public void train(List<Tree<String>> trainTrees) {
        List<Tree<String>> binarizedTrees = new ArrayList<Tree<String>>();
        // binarize the trees so that rules are at most binary
        for(Tree<String> tree : trainTrees) {
            binarizedTrees.add(TreeAnnotations.annotateTree(tree));
        }
        System.out.println(trainTrees.toString());
        lexicon = new Lexicon(binarizedTrees);
        grammar = new Grammar(binarizedTrees);
    }
   /* public Tree<String> getBestParse(List<String> sentence) {
           return null;
    }*/

    public Tree<String> getBestParse(List<String> sentence) {
        // TODO what should getBestParse do? Trace through the back pointers?
        Tree<String> bestGuess = cky(sentence);
        return bestGuess;
    }

    public void handleUnaries(double[][][] score, Triplet back, int i) {
        //handle unaries, we already have probabilities in our cells
        boolean added = true;
        // keep applying unary rules until we stop discovering new constituents over a span
        // with better probabilities
        while (added) {
            added = false;
            // consider all of our unary rules
            //for (String A, B in non_terms){
            for(String A : grammar.unaryRulesByChild.keySet()) {
                for(UnaryRule B : grammar.getUnaryRulesByChild(A)) {
                    // work out what probability they assign
                    if (((score[i][i + 1][B]) > 0) && B.getChild().equals(A)){
                        // prob = P(A-> B);
                        // returns a smoothed estimate of P(word|tag)
                        prob = lexicon.scoreTagging(A, B);
                        // if it's a better probability, store it in the back trace
                        if (prob > score[i][i + 1][A]) {
                            score[i][i + 1][A] = prob;
                            back[i][i + 1][A] = B;
                            // if we've done work, do another iteration
                            added = true;
                        }
                    }
                }
            }
        }
    }

    // TODO, returns [most_probable_parse, prob]
    public Tree<String> cky(List<String> words) {

        int num_words = words.size();
        // TODO, is this the actual number I want?
        int num_non_terms = lexicon.totalWordTypes();

        // store probabilities of things that we can build, probability of building a constituent over that span
        double[][][] score = new double[num_words + 1][num_words + 1][num_non_terms];
        // pointers, for each nonterminal over each span, what was the best way of building that probability?
        Triplet back = new Triplet(num_words + 1, num_words + 1, num_non_terms);

        // lexicon part - filling in nonterminals that words can rewrite as
        for (int i = 0; i < num_words; i++) {
            // loop through POS tags that correspond to word i
            // for (String A : non_terms) {
            for (String A : lexicon.getAllTags()) {
                // if we've got a rule for a nonterminal
                // TODO, how to check if a rules is in the grammar?
                //if (A-> words[i] in grammar){
                if (lexicon.scoreTagging(words[i], A) > 0) {
                    // put it's score into that cell of the chart
                    // TODO, where do I get the probability for a rule?
                    // int score[i][i + 1][A]=P(A-> words[i]);
                    score[i][i + 1][A]=lexicon.scoreTagging(words[i], A);
                }
            }
            handleUnaries(score, back, i);
        }
        // build the chart
        // ordering by the size of the constituent span
        for (int span = 2; span < num_words; span++) {
            // go accross the words from left to rightmost position
            for (int begin = 0; begin < num_words; begin++) {
                int end = begin + span;
                // for every possible split
                // try out different split points
                for (int split = begin + span; split < end - 1; split++) {
                    // consider all Triplets of nonterminals
                    // for (for Triplet A->B,C in nonterms){
                    for (String A : lexicon.getAllTags()) {
                        // ?????
                        int left_prob = grammar.getBinaryRulesByLeftChild(B);
                        int right_prob = grammar.getBinaryRulesByRightChild(C);
                        int prob = score[begin][split][B] * left_prob * right_prob;
                        // probability of building A over this span
                        // where B is the left hand side and C
                        // is the right hand side
                        if (prob > score[begin][end][A]) {
                            score[begin][end][A] = prob;
                            back[begin][end][A] = B;
                        }
                    }
                }
                handleUnaries(score, grammar, back);
            }
        }
        return buildTree(score, back);
    }
}

