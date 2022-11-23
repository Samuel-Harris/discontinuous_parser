/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constbase;

import java.util.*;

public class ConstTree {

    protected String id;

    protected List<ConstLeaf> leaves;

    protected List<ConstNode> roots;

    protected Map<String, ConstInternal> nodes;

    public ConstTree(String id) {
        this.id = id;
        this.leaves = new ArrayList<>();
        this.roots = new ArrayList<>();
        this.nodes = new TreeMap<>();
    }

    public String getId() {
        return id;
    }

    public ConstLeaf addLeaf(String form, String pos, String label) {
        ConstLeaf leaf = new ConstLeaf(leaves.size(), form, pos, label);
        leaves.add(leaf);
        return leaf;
    }

    public ConstLeaf getLeaf(int i) {
        return leaves.get(i);
    }

    public ConstLeaf[] getLeaves() {
        return leaves.toArray(new ConstLeaf[leaves.size()]);
    }

    public void addRoot(ConstNode node) {
        roots.add(node);
    }

    public List<ConstNode> getRoots() {
        return roots;
    }

    public ConstInternal addInternal(String id, String cat, String label) {
        ConstInternal node = getInternal(id);
        node.setCat(cat);
        node.setLabel(label);
        return node;
    }

    public ConstInternal getInternal(String id) {
        if (nodes.containsKey(id))
            return nodes.get(id);
        else {
            ConstInternal node = new ConstInternal(id);
            nodes.put(id, node);
            return node;
        }
    }

    public Set<String> getInternalIds() {
        return nodes.keySet();
    }

    public void addParent(ConstNode node, String parentId) {
        ConstInternal parent = getInternal(parentId);
        parent.addChildRight(node);
    }

    public boolean isProjective() {
        for (String id : getInternalIds())
            if (!isProjective(getInternal(id)))
                return false;
        return true;
    }

    public boolean isProjective(final ConstNode node) {
        ConstLeaf[] leaves = node.getLeaves();
        return leaves.length == 0 ||
                leaves[leaves.length - 1].getIndex() - leaves[0].getIndex() + 1
                        == leaves.length;
    }

    public boolean isSymbolConsistent(
            final Set<String> poss, final Set<String> cats, final Set<String> labels) {
        for (ConstLeaf leaf : leaves) {
            if (!poss.contains(leaf.getCat()))
                return false;
            if (!labels.contains(leaf.getLabel()))
                return false;
        }
        for (String id : getInternalIds()) {
            ConstInternal internal = getInternal(id);
            if (!cats.contains(internal.getCat()))
                return false;
            if (!labels.contains(internal.getLabel()))
                return false;
        }
        return true;
    }

    public void gatherSymbols(
            final Set<String> poss, final Set<String> cats, final Set<String> labels) {
        for (ConstLeaf leaf : leaves) {
            poss.add(leaf.getCat());
            labels.add(leaf.getLabel());
        }
        for (String id : getInternalIds()) {
            ConstInternal internal = getInternal(id);
            cats.add(internal.getCat());
            String label = internal.getLabel();
            if (label != null) labels.add(label);
        }
    }

    public int length() {
        return leaves.size();
    }

    public boolean hasCycle() {
        for (String id : getInternalIds()) {
            ConstInternal internal = getInternal(id);
            String cat = internal.getCat();
            while (internal.getChildren().length == 1 &&
                    internal.getChildren()[0] instanceof ConstInternal) {
                internal = (ConstInternal) internal.getChildren()[0];
                if (internal.getCat().equals(cat)) {
                    System.out.println(id);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean removeOneCycle() {
        ConstInternal node = null;
        ConstInternal descendant = null;
        for (String id : getInternalIds()) {
            ConstInternal internal = getInternal(id);
            String cat = internal.getCat();
            descendant = internal;
            while (descendant.getChildren().length == 1 &&
                    descendant.getChildren()[0] instanceof ConstInternal) {
                descendant = (ConstInternal) descendant.getChildren()[0];
                if (descendant.getCat().equals(cat)) {
                    node = internal;
                    break;
                }
            }
            if (node != null)
                break;
        }
        if (node != null) {
            Set<ConstInternal> links = new TreeSet<>();
            ConstInternal link = node;
            while (link != descendant) {
                ConstInternal child = (ConstInternal) link.getChildren()[0];
                links.add(child);
                link = child;
            }
            node.clearChildren();
            for (ConstNode child : descendant.getChildren()) {
                node.addChildRight(child);
            }
            node.setHeadIndex(descendant.getHeadIndex());
            for (ConstInternal l : links) {
                nodes.remove(l.getId());
            }
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return NegraTreebank.treeToString(this);
    }

}
