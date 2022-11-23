/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constbase.heads;

import standrews.constbase.*;

public abstract class HeadFinder {
    public void makeHeadedTreebank(ConstTreebank bank) {
        for (ConstTree tree : bank.getTrees()) {
            makeHeadedTree(tree);
        }
    }

    public void makeHeadedTree(ConstTree tree) {
        for (ConstNode root : tree.getRoots())
            makeHeadedNode(root);
    }

    protected void makeHeadedNode(ConstNode node) {
        if (node instanceof ConstLeaf)
            return;
        ConstInternal internal = (ConstInternal) node;
        for (ConstNode child : internal.getChildren())
            makeHeadedNode(child);
        internal.sortChildren();
        internal.setHeadIndex(getHeadIndex(internal));
    }

    protected abstract int getHeadIndex(ConstInternal node);

    protected int nLabel(ConstInternal node, String lab) {
        return nLabel(node, new String[]{lab});
    }

    protected int nLabel(ConstInternal node, String[] labs) {
        int n = 0;
        for (ConstNode child : node.getChildren()) {
            for (String lab : labs) {
                if (child.getLabel().equals(lab))
                    n++;
            }
        }
        return n;
    }

    protected int nCat(ConstInternal node, String cat) {
        return nCat(node, new String[]{cat});
    }

    protected int nCat(ConstInternal node, String[] cats) {
        int n = 0;
        for (ConstNode child : node.getChildren()) {
            for (String cat : cats) {
                if (child.getCat().equals(cat))
                    n++;
            }
        }
        return n;
    }

    protected int firstWithLabel(ConstInternal node, String lab) {
        return firstWithLabel(node, new String[]{lab});
    }

    protected int firstWithLabel(ConstInternal node, String[] labs) {
        int i = 0;
        for (ConstNode child : node.getChildren()) {
            for (String lab : labs) {
                if (child.getLabel().equals(lab))
                    return i;
            }
            i++;
        }
        return -1;
    }

    protected int firstWithCat(ConstInternal node, String cat) {
        return firstWithCat(node, new String[]{cat});
    }

    protected int firstWithCat(ConstInternal node, String[] cats) {
        int i = 0;
        for (ConstNode child : node.getChildren()) {
            for (String cat : cats) {
                if (child.getCat().equals(cat))
                    return i;
            }
            i++;
        }
        return -1;
    }

    protected void printLeaves(ConstLeaf[] leaves) {
        for (ConstLeaf leaf : leaves)
            System.err.print(leaf.getForm() + " ");
        System.err.println();
    }

}
