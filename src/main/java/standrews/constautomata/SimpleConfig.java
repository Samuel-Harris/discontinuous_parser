/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constautomata;

import standrews.constbase.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimpleConfig {
    protected final String id;
    protected final List<ConstNode> stack;
    protected final List<String> states;
    protected final List<EnhancedConstLeaf> input;

    /**
     * Gold tree, for debugging.
     */
    public ConstTree goldTree = null;

    public SimpleConfig(final String id, final EnhancedConstLeaf[] input, final String topState) {
        this.id = id;
        this.stack = Stream.of(new ConstInternal())
                .collect(Collectors.toCollection(ArrayList::new));
        this.states = Stream.of(topState)
                .map(String::new)
                .collect(Collectors.toCollection(ArrayList::new));
        this.input = Arrays.stream(input)
                .map(EnhancedConstLeaf::new)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public SimpleConfig(final String id, final ConstLeaf[] input, double[][] embeddings) {
        this.id = id;
        this.stack = Stream.of(new ConstInternal())
                .collect(Collectors.toCollection(ArrayList::new));
        this.states = Stream.of("")
                .map(String::new)
                .collect(Collectors.toCollection(ArrayList::new));

        this.input = new ArrayList<>();
        for (int i = 0; i < input.length; i++) {
            this.input.add(new EnhancedConstLeaf(input[i], embeddings[i]));
        }
    }

    /**
     * Deep copy.
     */
    public SimpleConfig(final SimpleConfig config) {
        this.id = config.id;
        this.stack = config.stack.stream()
                .map(ConstNode::copy)
                .collect(Collectors.toCollection(ArrayList::new));
        this.states = config.states.stream()
                .map(String::new)
                .collect(Collectors.toCollection(ArrayList::new));
        this.input = config.input.stream()
                .map(EnhancedConstLeaf::new)
                .collect(Collectors.toCollection(ArrayList::new));
        this.goldTree = config.goldTree;
    }

    /**
     * Is this a final configuration?
     *
     * @return True if it is a final configuration.
     */
    public boolean isFinal() {
        return (stack.size() == 1 && input.isEmpty());
    }

    /**
     * Get length of stack.
     *
     * @return The length of the stack.
     */
    public int stackLength() {
        return stack.size();
    }

    /**
     * Get element from stack, indexed from left to right.
     *
     * @param i Index.
     * @return The element.
     */
    public ConstNode getStackLeft(final int i) {
        return stack.get(i);
    }

    /**
     * Get statte belonging to element from prefix.
     */
    public String getStateLeft(final int i) {
        return states.get(i);
    }

    /**
     * Get element from prefix, indexed from right to left.
     *
     * @param i Index.
     * @return The element.
     */
    public ConstNode getStackRight(final int i) {
        return stack.get(stack.size() - 1 - i);
    }

    /**
     * Get label belonging to element from prefix.
     */
    public String getStateRight(final int i) {
        return states.get(stack.size() - 1 - i);
    }

    public String getId() {
        return id;
    }

    public void setStateRight(final int i, final String state) {
        states.set(stack.size() - 1 - i, state);
    }

    public void addStackRight(final ConstNode node) {
        addStackRight(node, "");
    }

    public void addStackRight(final ConstNode node, final String state) {
        stack.add(node);
        states.add(state);
    }

    public void addStackRight(final ConstNode node,
                              final int index) {
        addStackRight(node, "", index);
    }

    public void addStackRight(final ConstNode node, final String state,
                              final int index) {
        stack.add(index, node);
        states.add(index, state);
    }

    /**
     * Remove and return element from stack, indexed from left to right.
     *
     * @param i Index.
     * @return The removed element.
     */
    public ConstNode removeStackLeft(final int i) {
        states.remove(i);
        return stack.remove(i);
    }

    /**
     * Remove and return element from prefix, indexed from right to left.
     *
     * @param i Index.
     * @return The removed element.
     */
    public ConstNode removeStackRight(final int i) {
        final int index = stack.size() - 1 - i;
        states.remove(index);
        return stack.remove(index);
    }

    public ArrayList<ConstNode> stackList() {
        final ArrayList<ConstNode> list = new ArrayList<>();
        for (int i = 0; i < stackLength(); i++)
            list.add(getStackLeft(i));
        return list;
    }

    public ArrayList<String> stateList() {
        final ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < stackLength(); i++)
            list.add(getStateLeft(i));
        return list;
    }

    /**
     * Get length of input.
     *
     * @return The length of the input.
     */
    public int inputLength() {
        return input.size();
    }

    /**
     * Get element from input, indexed from left to right.
     *
     * @param i Index.
     * @return The element.
     */
    public EnhancedConstLeaf getInputLeft(final int i) {
        return input.get(i);
    }

    /**
     * Remove and return leftmost element from input.
     *
     * @return The element.
     */
    public ConstLeaf removeInputLeft() {
        return input.remove(0);
    }

    public void addInputLeft(EnhancedConstLeaf vertex) {
        input.add(0, vertex);
    }

    public List<EnhancedConstLeaf> inputList() {
        return input;
    }

    public int totalLength() {
        return stackLength() + inputLength();
    }

    /**
     * Create a parse tree from configuration.
     *
     * @return The parse tree.
     */
    public ConstTree createParse() {
        ConstTree tree = new ConstTree(id);
        Set<ConstLeaf> leaves = new TreeSet<>();
        for (int i = 0; i < stackLength(); i++)
            for (ConstLeaf leaf : getStackLeft(i).getLeaves())
                leaves.add(leaf);
        for (int i = 0; i < inputLength(); i++)
            leaves.add(getInputLeft(i));
        for (ConstLeaf leaf : leaves) {
            tree.addLeaf(leaf.getForm(), leaf.getCat(), leaf.getLabel());
        }
        int idNum = 500;
        idNum = createSubParseTop(tree, getStackLeft(0), idNum);
        for (int i = 1; i < stackLength(); i++)
            idNum = createSubParse(tree, getStackLeft(i), null, idNum);
        return tree;
    }

    private int createSubParseTop(ConstTree tree, ConstNode top, int idNum) {
        ConstInternal internal = (ConstInternal) top;
        for (ConstNode child : internal.getChildren()) {
            idNum = createSubParse(tree, child, null, idNum);
        }
        return idNum;
    }

    private int createSubParse(ConstTree tree, ConstNode node, String parentId, int idNum) {
        ConstNode nodeCopy;
        if (node instanceof ConstLeaf) {
            ConstLeaf leaf = (ConstLeaf) node;
            nodeCopy = tree.getLeaf(leaf.getIndex());
        } else {
            ConstInternal internal = (ConstInternal) node;
            String id = "" + (idNum++);
            nodeCopy = tree.addInternal(id, internal.getCat(), internal.getLabel());
            for (ConstNode child : internal.getChildren()) {
                idNum = createSubParse(tree, child, id, idNum);
            }
        }
        if (parentId == null)
            tree.addRoot(nodeCopy);
        else
            tree.addParent(nodeCopy, parentId);
        return idNum;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < stack.size(); i++) {
            ConstNode node = stack.get(i);
            String state = states.get(i);
            if (!state.equals(""))
                buf.append(state + " ");
            buf.append("" + node + "\n");
        }
        buf.append("-------\n");
        for (ConstLeaf leaf : input)
            buf.append("" + leaf + "\n");
        return buf.toString();
    }
}
