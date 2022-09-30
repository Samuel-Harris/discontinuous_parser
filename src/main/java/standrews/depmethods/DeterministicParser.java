/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depmethods;

import standrews.depautomata.DependencyGraph;
import standrews.depautomata.DependencyVertex;
import standrews.depautomata.SimpleConfig;
import standrews.depextract.SimpleExtractor;
import standrews.depbase.Token;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class DeterministicParser {
    /**
     * The tokens of the input.
     */
    protected final Token[] tokens;

    /**
     * The gold graph, used if doing training.
     */
    protected DependencyGraph graph;

    /**
     * Is gold tree guaranteed to be projective.
     */
    protected boolean isProjective = false;

    /**
     * The names of the actions. Subclasses to override.
     * @return
     */
    public abstract String[] actionNames();

    /**
     * The labels of prefix elements.
     * By default only empty string.
     */
    public String[] prefixLabels() {
        return new String[] {""};
    }

    /**
     * @param tokens Input string.
     */
    public DeterministicParser(final Token[] tokens) {
        this.tokens = tokens;
    }

    /**
     * Set projective.
     */
    public void setProjective(final boolean b) {
        isProjective = b;
    }

    /**
     * Is gold tree projective?
     */
    public boolean isProjective() {
        return isProjective;
    }

    /**
     * Make observations for parse, by default using static oracle.
     *
     * @param extractor  Extractor of features.
     */
    public void observe(final SimpleExtractor extractor) {
        prepareTraining();
        final SimpleConfig config = makeInitialConfig(tokens);
        while (!config.isFinal()) {
            final String[] action = getAction(config);
            if (action.length == 0) {
                fail("training", config);
                break;
            }
            extractor.extract(config, action);

            final String[] stepAction = getStepAction(config);
            if (stepAction.length == 0) {
                fail("training", config);
                break;
            }
            apply(config, stepAction);
        }
    }

    public double negLogProbability(final SimpleExtractor extractor) {
        prepareTraining();
        final SimpleConfig config = makeInitialConfig(tokens);
        double weight = 0;
        while (!config.isFinal()) {
            final String[] action = getAction(config);
            if (action.length == 0) {
                fail("training", config);
                break;
            }
            double p = extractor.actionProbability(config, action[0]);
            weight += -Math.log(p) / Math.log(2);
            apply(config, action);
        }
        return weight;
    }

    /**
     * Build data structures needed for determining parser actions.
     */
    protected void prepareTraining() {
        graph = new DependencyGraph(tokens);
    }

    protected boolean isEdge(Token t1, Token t0) {
        return graph.isDependentOf(t0.id, t1.id);
    }

    protected boolean isEdge(final DependencyVertex v1, final DependencyVertex v2) {
        return isEdge(v1.getToken(), v2.getToken());
    }

    protected boolean isLink(final DependencyVertex v1, final DependencyVertex v2) {
        return isEdge(v1, v2) || isEdge(v2, v1);
    }

    /**
     * Weight of dependency edge from t1 to t2.
     * @param t1
     * @param t2
     * @return
     */
    protected int edgeWeight(final Token t1, final Token t2) {
        return isEdge(t1, t2) ? 1 : 0;
    }

    protected int edgeWeight(final DependencyVertex v1, final DependencyVertex v2) {
        return edgeWeight(v1.getToken(), v2.getToken());
    }

    protected boolean areChildrenAttached(final DependencyVertex vParse) {
        final DependencyVertex vGold = goldVertex(vParse);
        return vGold.getChildren().length == vParse.getChildren().length;
    }

    protected boolean areRightChildrenAttached(final DependencyVertex vParse) {
        final DependencyVertex vGold = goldVertex(vParse);
        return vGold.getRightChildren().length == vParse.getRightChildren().length;
    }

    protected DependencyVertex goldVertex(final DependencyVertex vParse) {
        return graph.getVertex(vParse.getToken().id);
    }

    protected boolean isRightChild(final DependencyVertex v) {
        final DependencyVertex p = goldVertex(v).getParent();
        return p != null && p.compareTo(v) < 0;
    }

    protected String deprel(final Token t) {
        return graph.getVertex(t.id).getToken().deprel.toString();
    }

    protected String deprel(final DependencyVertex v) {
        return deprel(v.getToken());
    }

    /**
     * Parse sentence with trained classifier.
     *
     * @param extractor  Extractor of features.
     * @return Parse.
     */
    public Token[] parse(final SimpleExtractor extractor) {
        final SimpleConfig config = makeInitialConfig(tokens);
        while (!config.isFinal()) {
            final Iterator<String[]> actions = extractor.predict(config);
            if (!bestActionCompleted(config, actions)) {
                fail("parsing", config);
                break;
            }
        }
        return config.createParse();
    }

    /**
     * Report failure of action.
     *
     * @param failedAction The thing that failed.
     * @param config       The configuration in which failure occurs.
     */
    protected void fail(final String failedAction, final SimpleConfig config) {
        final Logger log = Logger.getLogger(getClass().getName());
        log.setParent(Logger.getGlobal());
        log.log(Level.WARNING, failedAction + " failed: " + config);
    }

    /**
     * Do best action.
     *
     * @param config  The configuration.
     * @param actions The ranked actions.
     * @return True if an action was performed.
     */
    private boolean bestActionCompleted(final SimpleConfig config,
                                        final Iterator<String[]> actions) {
        while (actions.hasNext()) {
            final String[] action = actions.next();
            if (applicable(config, action)) {
                apply(config, action);
                return true;
            }
        }
        return false;
    }

    protected String[] getStepAction(final SimpleConfig config) {
        return getAction(config);
    }

    protected abstract SimpleConfig makeInitialConfig(final Token[] tokens);

    protected abstract String[] getAction(final SimpleConfig config);

    protected abstract void apply(final SimpleConfig config, final String[] action);

    protected abstract boolean applicable(final SimpleConfig config, final String[] action);

    /**
     * In semiring, zero value.
     * @return
     */
    protected static int zero() {
        return Integer.MIN_VALUE;
    }

    protected static int one() {
        return 0;
    }

    protected static int times(final int a, final int b) {
        return a > zero() && b > zero() ? a+b : zero();
    }

    protected static int plus(final int a, final int b) {
        return Math.max(a, b);
    }

    protected TreeMap<String,Integer> scores() {
        final TreeMap<String, Integer> scores = new TreeMap<>();
        for (String actionName : actionNames())
            scores.put(actionName, zero());
        return scores;
    }

    /**
     * Get the set of actions with highest score.
     * @param scores
     * @return
     */
    protected Vector<String> getBestActions(final TreeMap<String,Integer> scores) {
        int max = zero();
        for (String actionName : actionNames())
            max = plus(max, scores.get(actionName));
        final Vector<String> actions = new Vector<>();
        for (String actionName : actionNames())
            if (scores.get(actionName) == max)
                actions.add(actionName);
        return actions;
    }

    protected boolean equalScores(final TreeMap<String,Integer> scores1,
                                   final TreeMap<String,Integer> scores2) {
        if (scores1.keySet().size() != scores2.keySet().size())
            return false;
        for (String action : scores1.keySet()) {
            if (!scores1.get(action).equals(scores2.get(action)))
                return false;
        }
        return true;
    }
    protected boolean equalActions(Vector<String> actions1, Vector<String> actions2) {
        if (actions1.size() != actions2.size())
            return false;
        for (int i = 0; i < actions1.size(); i++) {
            if (!actions1.get(i).equals(actions2.get(i)))
                return false;
        }
        return true;
    }

    /**
     * Get possible actions.
     */
    protected Vector<String> getPossibleActions(final TreeMap<String,Integer> scores) {
        final Vector<String> actions = new Vector<>();
        for (String actionName : actionNames())
        if (scores.get(actionName) > zero())
            actions.add(actionName);
        return actions;
    }

    /**
     * Normalize to make smallest value 0.
     * @return
     */
    protected void normalizeScores(final TreeMap<String,Integer> scores) {
        int min = Integer.MAX_VALUE;
        for (String actionName : actionNames()) {
            int val = scores.get(actionName);
            if (val != zero() && val < min)
                min = val;
        }
        if (min != Integer.MAX_VALUE) {
            for (String actionName : actionNames()) {
                int val = scores.get(actionName);
                if (val != zero())
                    scores.put(actionName, val - min);
            }
        }
    }

    /**
     * Scores of actions as string.
     */
    protected String toString(final TreeMap<String,Integer> scores) {
        String str = "";
        for (String actionName : actionNames()) {
            str += " " + actionName + "=" +
                    (scores.get(actionName) == zero() ? "-" :
                    scores.get(actionName));
        }
        return str;
    }

    /**
     * Print configuration, with only positions. For testing.
     */
    protected void printConfig(final SimpleConfig config) {
        System.out.println("prefix/suffix " + config.prefixLength() + "/" +
                config.suffixLength());
        final int m = config.prefixLength();
        final int n = config.suffixLength();
        for (int i = 0; i < m; i++) {
            System.out.print(config.getLabelLeft(i) + " ");
        }
        System.out.println();
        for (int i = 0; i < m+n; i++) {
            for (int j = 0; j < m+n; j++) {
                final Token ti = i < m ?
                        config.getPrefixLeft(i).getToken() :
                        config.getSuffixLeft(i-m).getToken();
                final Token tj = j < m ?
                        config.getPrefixLeft(j).getToken() :
                        config.getSuffixLeft(j-m).getToken();
                if (isEdge(ti, tj))
                    System.out.println("" + i + " " + j);
            }
        }
    }

    protected void printScores(final TreeMap<String,Integer> scores) {
        for (String a : scores.keySet()) {
            System.out.println(a + " " + scores.get(a));
        }
    }

    protected ArrayList<DependencyVertex> prunedSuffix(final ArrayList<DependencyVertex> suffixList) {
        if (suffixList.size() == 0)
            return suffixList;
        else {
            final DependencyVertex hd = suffixList.get(0);
            final ArrayList<DependencyVertex> pruned = new ArrayList<>();
            // System.out.println(graph.root);
            // System.out.println(goldVertex(hd));
            prunedSuffix(hd, goldVertex(hd), false, pruned);
            // System.out.println(pruned);
            return pruned;
        }
    }

    protected void prunedSuffix(final DependencyVertex hd, final DependencyVertex v, boolean missing,
                                final ArrayList<DependencyVertex> pruned) {
        DependencyVertex p = v.getParent();
        if (p != null) {
            if (hd.compareTo(v) <= 0) {
                final DependencyVertex[] left = v.getLeftChildren();
                missing = missing || left.length > 0 && left[0].compareTo(hd) < 0;
                if (missing || p.compareTo(hd) < 0)
                    pruned.add(v);
            }
            if (p.compareTo(hd) < 0) {
                DependencyVertex[] siblings = p.getRightChildren();
                for (int i = siblings.length - 1; i >= 0; i--) {
                    DependencyVertex s = siblings[i];
                    if (v.compareTo(s) < 0 && hd.compareTo(s) < 0)
                        pruned.add(s);
                    else
                        break;
                }
            }
            prunedSuffix(hd, p, missing, pruned);
        }
    }

}
