/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constbase;

public abstract class ConstNode implements Comparable<ConstNode> {

    public abstract boolean isTop();

    public ConstNode copy() {
        if (this instanceof ConstLeaf)
            return new ConstLeaf((ConstLeaf) this);
        else
            return new ConstInternal((ConstInternal) this);
    }

    public abstract String getCat();

    public abstract String getLabel();

    public abstract ConstLeaf[] getLeaves();

    public abstract ConstLeaf getHeadLeaf();

    public int getHeadLeafIndex() {
        ConstLeaf leaf = getHeadLeaf();
        return leaf == null ? -1 : leaf.getIndex();
    }

    public int compareTo(final ConstNode other) {
        return Integer.compare(getHeadLeafIndex(), other.getHeadLeafIndex());
    }

    public abstract String getIdentification();
}
