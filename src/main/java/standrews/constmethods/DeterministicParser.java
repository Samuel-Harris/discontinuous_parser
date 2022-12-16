/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constmethods;

import standrews.constautomata.HatConfig;
import standrews.constautomata.SimpleConfig;
import standrews.constbase.*;
import standrews.constextract.HatExtractor;
import standrews.constextract.SimpleExtractor;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class DeterministicParser {
    /**
     * The gold tree, used if doing training.
     */
    protected ConstGraph goldTree;

    /**
     * Is gold tree guaranteed to be projective.
     */
    protected boolean isProjective = false;

    /**
     * The names of the actions. Subclasses to override.
     *
     * @return
     */
    public abstract String[] actionNames();

    /**
     * @param goldTree The gold tree.
     */
    public DeterministicParser(final ConstTree goldTree) {
        this.goldTree = new ConstGraph(goldTree);
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
     * @param extractor Extractor of features.
     */
    public void observe(final HatExtractor extractor, double[][] embeddings) {
        prepareTraining();
        final HatConfig config = makeInitialConfig(goldTree, embeddings);
        while (!config.isFinal()) {
            final String[] action = getAction(config);
            if (action.length == 0) {
                fail("training", config);
                break;
            } else if (!applicable(config, action)) {
                fail("training inapplicable action " +
                        actionString(action), config);
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

    /**
     * Build data structures needed for determining parser actions.
     */
    protected void prepareTraining() {
        // not used now
    }

    protected ConstInternal goldParent(ConstNode n) {
        return goldTree.getParent(n);
    }

    protected boolean isHead(final ConstNode node) {
        ConstNode goldNode = goldNode(node);
        ConstInternal goldParent = goldTree.getParent(goldNode);
        return goldParent != null && goldParent.getHeadChild() == goldNode;
    }

    protected boolean longChain(ConstNode node) {
        Set<String> cats = new TreeSet<>();
        while (node instanceof ConstInternal) {
            ConstInternal internal = (ConstInternal) node;
            if (internal.getChildren().length != 1)
                return false;
            if (cats.contains(internal.getCat()))
                return true;
            cats.add(internal.getCat());
            node = internal.getChildren()[0];
        }
        return false;
    }

    protected boolean areChildrenAttached(final ConstNode node) {
        if (node instanceof ConstLeaf)
            return true;
        if (node.isTop())
            return false;
        ConstInternal internal = (ConstInternal) node;
        ConstInternal goldInternal = goldInternal(internal);
        return internal.getChildren().length == goldInternal.getChildren().length;
    }

    protected boolean areRightChildrenAttached(final ConstNode node) {
        if (node instanceof ConstLeaf)
            return true;
        ConstInternal internal = (ConstInternal) node;
        ConstInternal goldInternal = goldInternal(internal);
        return internal.getRightChildren().length == goldInternal.getRightChildren().length;
    }

    protected boolean hasChildGold(final ConstNode n1, final ConstNode n2) {
        if (n2.isTop())
            return false;
        final ConstNode ng2 = goldNode(n2);
        if (n1.isTop())
            return goldTree.getRoots().contains(ng2);
        if (n1 instanceof ConstLeaf)
            return false;
        final ConstInternal ng1 = goldInternal((ConstInternal) n1);
        return Arrays.asList(ng1.getChildren()).contains(ng2);
    }

    protected ConstLeaf goldLeaf(ConstLeaf n) {
        return goldTree.getLeaves()[n.getIndex()];
    }

    protected ConstInternal goldInternal(ConstInternal n) {
        return goldTree.getInternal(n.getId());
    }

    protected ConstNode goldNode(ConstNode n) {
        if (n instanceof ConstLeaf)
            return goldLeaf((ConstLeaf) n);
        else
            return goldInternal((ConstInternal) n);
    }

    protected ConstNode[] goldRoots() {
        return goldTree.getRoots().toArray(new ConstNode[0]);
    }

    protected int[] withParent(ConstInternal parent, List<ConstNode> elems) {
        Set<Integer> children = new TreeSet<>();
        for (int i = 0; i < elems.size(); i++) {
            if (goldParent(elems.get(i)) == parent)
                children.add(i);
        }
        return children.stream().mapToInt(n -> n).toArray();
    }

    protected int[] withRootParent(List<ConstNode> elems) {
        Set<Integer> children = new TreeSet<>();
        for (int i = 1; i < elems.size(); i++) {
            if (goldParent(elems.get(i)) == null)
                children.add(i);
        }
        return children.stream().mapToInt(n -> n).toArray();
    }

    /**
     * Parse sentence with trained classifier.
     *
     * @param extractor Extractor of features.
     * @return Parse.
     */
    public ConstTree parse(final SimpleExtractor extractor, double[][] embeddings) {
        final HatConfig config = makeInitialConfig(goldTree, embeddings);
        while (!config.isFinal()) {
            final Iterator<String[]> actions = extractor.predict(config);
            if (!bestActionCompleted(config, actions)) {
                listActions(extractor.predict(config));
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
    protected void fail(final String failedAction,
                        final SimpleConfig config) {
        final Logger log = Logger.getLogger(getClass().getName());
        log.setParent(Logger.getGlobal());
        log.log(Level.WARNING, failedAction + " failed: " + config + " " + goldTree);
        System.exit(-1);
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

    private void listActions(final Iterator<String[]> actions) {
        int i = 0;
        while (actions.hasNext()) {
            final String[] action = actions.next();
            System.err.println(actionString(action));
            i++;
        }
        System.err.println("All of " + i + " actions did not apply");

    }

    public static String actionString(final String[] action) {
        if (action.length == 0)
            return "";
        String s = action[0];
        for (int i = 1; i < action.length; i++)
            s += " " + action[i];
        return s;
    }

    protected String[] getStepAction(final SimpleConfig config) {
        return getAction(config);
    }

    protected abstract HatConfig makeInitialConfig(final ConstTree tree, double[][] embeddings);

    protected abstract String[] getAction(final SimpleConfig config);

    protected abstract void apply(final SimpleConfig config, final String[] action);

    protected abstract boolean applicable(final SimpleConfig config, final String[] action);

    /**
     * In semiring, zero value.
     *
     * @return
     */
    protected static int zero() {
        return Integer.MIN_VALUE;
    }

    protected static int one() {
        return 0;
    }

    protected static int times(final int a, final int b) {
        return a > zero() && b > zero() ? a + b : zero();
    }

    protected static int plus(final int a, final int b) {
        return Math.max(a, b);
    }

    protected TreeMap<String, Integer> scores() {
        final TreeMap<String, Integer> scores = new TreeMap<>();
        for (String actionName : actionNames())
            scores.put(actionName, zero());
        return scores;
    }

    /**
     * Get the set of actions with highest score.
     *
     * @param scores
     * @return
     */
    protected Vector<String> getBestActions(final TreeMap<String, Integer> scores) {
        int max = zero();
        for (String actionName : actionNames())
            max = plus(max, scores.get(actionName));
        final Vector<String> actions = new Vector<>();
        for (String actionName : actionNames())
            if (scores.get(actionName) == max)
                actions.add(actionName);
        return actions;
    }

    /**
     * Get possible actions.
     */
    protected Vector<String> getPossibleActions(final TreeMap<String, Integer> scores) {
        final Vector<String> actions = new Vector<>();
        for (String actionName : actionNames())
            if (scores.get(actionName) > zero())
                actions.add(actionName);
        return actions;
    }

    /**
     * Normalize to make smallest value 0.
     *
     * @return
     */
    protected void normalizeScores(final TreeMap<String, Integer> scores) {
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
    protected String toString(final TreeMap<String, Integer> scores) {
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
        System.out.println("stack/input " + config.stackLength() + "/" +
                config.inputLength());
        final int m = config.stackLength();
        final int n = config.inputLength();
        for (int i = 0; i < m; i++) {
            final ConstNode node = config.getStackLeft(i);
            System.out.print(node.getIdentification() + " ");
        }
        for (int i = 0; i < n; i++) {
            final ConstLeaf node = config.getInputLeft(i);
            System.out.print(node.getForm() + " ");
        }
        System.out.println();
        for (int i = 0; i < m + n; i++) {
            for (int j = 0; j < m + n; j++) {
                final ConstNode ni = i < m ?
                        config.getStackLeft(i) :
                        config.getInputLeft(i - m);
                final ConstNode nj = j < m ?
                        config.getStackLeft(j) :
                        config.getInputLeft(j - m);
                if (hasChildGold(ni, nj))
                    System.out.println("" +
                            ni.getIdentification() + " " +
                            nj.getIdentification());

            }
        }
    }

}
