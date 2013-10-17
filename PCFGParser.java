package cs224n.assignment;

import cs224n.ling.Tree;

import java.util.*;

import cs224n.util.*;
import cs224n.assignment.Grammar.BinaryRule;
import cs224n.assignment.Grammar.UnaryRule;

/**
 * The CKY PCFG Parser you will implement.
 */
public class PCFGParser implements Parser {
    private Grammar grammar;
    private Lexicon lexicon;
    Map<String, Integer> nonterminals;

    public void getSymbols(Grammar grammar, Map<String, Integer> nonterminals, Lexicon lexicon) {

        // get a map of all nonterminals of the form (nonterminal symbol, integer)
        for (BinaryRule r : grammar.binaryRulesByLeftChild().keySet()) {
            String parent = r.parent;
            String left_child = r.leftChild;
            String right_child = r.rightChild;

            if (!nonterminals.containsKey(parent)) {
                nonterminals.put(parent, nonterminals.keySet().size());
            }
            if (!nonterminals.containsKey(left_child)) {
                nonterminals.put(left_child, nonterminals.keySet().size());
            }
            if (!nonterminals.containsKey(right_child)) {
                nonterminals.put(right_child, nonterminals.keySet().size());
            }
        }
        for (UnaryRule u : grammar.unaryRulesByChild().keySet()) {
            String parent = u.parent;
            String child = u.child;
            if (!nonterminals.containsKey(parent)) {
                Set<String> words = lexicon.wordCounter.keySet();
                if (!words.contains(parent)) {
                    nonterminals.put(parent, nonterminals.keySet().size());
                }
            }
            if (!nonterminals.containsKey(child)) {
                Set<String> words = lexicon.wordCounter.keySet();
                if (!words.contains(child)) {
                    nonterminals.put(child, nonterminals.keySet().size());
                }
            }
        }
    }
    // build the grammar and its probabilities
    public void train(List<Tree<String>> trainTrees) {
        List<Tree<String>> binarizedTrees = new ArrayList<Tree<String>>();
        // binarize the trees so that rules are at most binary
        for (Tree<String> tree : trainTrees) {
            binarizedTrees.add(TreeAnnotations.annotateTree(tree));
        }
        System.out.println(trainTrees.toString());
        lexicon = new Lexicon(binarizedTrees);
        grammar = new Grammar(binarizedTrees);
    }

    public Tree<String> getBestParse(List<String> sentence) {
        // TODO what should getBestParse do? Trace through the back pointers?
        Tree<String> bestParse = cky(sentence);
        return bestParse;
    }

    public void handleUnaries(double[][][] score, double[][][] back, int i) {
        //handle unaries, we already have probabilities in our cells
        boolean added = true;
        // keep applying unary rules until we stop discovering new constituents over a span
        // with better probabilities
        while (added) {
            added = false;
            // consider all unary rules in nonterminals
            for (String parent : nonterminals.keySet()) {
                List<UnaryRule> rules = grammar.getUnaryRulesByChild(parent);
                for (UnaryRule r : rules) {
                    String child = r.child;
                    if (((score[i][i + 1][nonterminals.get(r.child)]) > 0)) {
                        // returns a smoothed estimate of P(word|tag)
                        double prob = lexicon.scoreTagging(parent, child);
                        // if it's a better probability, store it in the back trace
                        if (prob > score[i][i + 1][nonterminals.get(r.parent)]) {
                            score[i][i + 1][nonterminals.get(r.parent)] = prob;
                            // TODO not totally sure how the back pointers should be constructed
                            back[i][i + 1][nonterminals.get(r.parent)] = prob;
                            // if we've done work, do another iteration
                            added = true;
                        }
                    }
                }
            }
        }
    }

    public Tree<String> cky(List<String> words) {

        int num_words = words.size();
        // store probabilities of things that we can build
        // each score is the probability of building a constituent over a given span
        double[][][] score = new double[num_words + 1][num_words + 1][nonterminals.keySet().size()];
        // stores back pointers for each nonterminal over each span
        // represents the best way of building that probability over that span
        // TODO: pseudo code suggests using a Triple?
        double[][][] back = new double[num_words + 1][num_words + 1] [nonterminals.keySet().size()];

        // lexicon part - filling in nonterminals that words can rewrite as
        for (int i = 0; i < num_words; i++) {
            // loop through POS tags that correspond to word i
            for (String symbols : lexicon.getAllTags()) {
                // if we've got a rule for a given nonterminal,
                // e.g. if (symbol -> words[i] in grammar)
                List<UnaryRule> rules = grammar.getUnaryRulesByChild(words.get(i));
                for(UnaryRule r : rules) {
                    double prob = r.getScore();
                    // put it's score into that cell of the chart
                    score[i][i+1][nonterminals.get(r.parent)] = prob;

                }
            }
            handleUnaries(score, back, i);
        }
        // build the chart, ordering by the size of the constituent span
        for (int span = 2; span < num_words; span++) {
            // go accross the words from left to rightmost position
            for (int begin = 0; begin < num_words; begin++) {
                int end = begin + span;
                // try out all possible split points
                for (int split = begin + span; split < end - 1; split++) {

                    // variable to hold the rules that are possible for a given split
                    BinaryRule rules_by_left;
                    BinaryRule rules_by_right;
                    // variable to hold the highest probability for a given parent
                    Double max_q;

                    // loop over the possible left children
                    for (int i = 0; i < score[begin][split].length; i++) {
                        if (score[begin][split].length != 0) {
                            String symbol = nonterminals.get(i);
                            rules_by_left = grammar.getBinaryRulesByLeftChild(symbol);
                        }
                    }
                    // loop over the possible right children
                    for (int i = 0; i < score[begin][split].length; i++) {
                        if (score[begin][split].length != 0) {
                            String symbol = nonterminals.get(i);
                            rules_by_right = grammar.getBinaryRulesByRightChild(symbol);
                        }
                    }
                    for (BinaryRule rule : rules_by_left) {
                        max_q = score[begin][split][nonterminals.get(rule.leftChild)]
                                * score[split][end][nonterminals.get(rule.rightChild)] * rule.getScore();
                        if (max_q < score[begin][end][nonterminals.get(rule.parent)]) {
                            score[begin][end][nonterminals.get(rule.parent)] = max_q;
                        }
                    }
                    for (BinaryRule rule : rules_by_right) {
                        max_q = score[begin][split][nonterminals.get(rule.rightChild)]
                                * score[split][end][nonterminals.get(rule.leftChild)]
                                * rule.getScore();
                        if (max_q < score[begin][end][nonterminals.get(rule.parent)]) {
                            score[begin][end][nonterminals.get(rule.parent)] = max_q;
                        }
                    }
                }
            }
            // TODO: not sure exactly what to pass to handleUnaries
            handleUnaries(score, back, span);
        }
        return buildTree(score, back);
    }

    public Tree buildTree(double[][][] score, double[][][] back) {
        // follow back ptrs
        return null;
    }
}

