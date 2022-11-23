/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constbase;

import java.util.TreeMap;

/**
 * For convenience, tree with additional datastructures.
 */
public class ConstGraph extends ConstTree {

    protected ConstInternal[] leafParents;
    protected TreeMap<String, ConstInternal> internalParents;

    public ConstGraph(final ConstTree tree) {
        super(tree.getId());
        this.leaves = tree.leaves;
        this.roots = tree.roots;
        this.nodes = tree.nodes;
        analyseParents();
    }

    private void analyseParents() {
        leafParents = new ConstInternal[leaves.size()];
        internalParents = new TreeMap<>();
        for (String id : getInternalIds()) {
            ConstInternal parent = getInternal(id);
            for (ConstNode child : parent.getChildren()) {
                if (child instanceof ConstLeaf) {
                    ConstLeaf leafChild = (ConstLeaf) child;
                    leafParents[leafChild.getIndex()] = parent;
                } else {
                    ConstInternal internChild = (ConstInternal) child;
                    internalParents.put(internChild.getId(), parent);
                }
            }
        }
    }

    public ConstInternal getParent(ConstNode child) {
        if (child instanceof ConstLeaf) {
            ConstLeaf leafChild = (ConstLeaf) child;
            return leafParents[leafChild.getIndex()];
        } else {
            ConstInternal internChild = (ConstInternal) child;
            String id = internChild.getId();
            if (id == null)
                return null;
            else
                return internalParents.get(id);
        }
    }
}
