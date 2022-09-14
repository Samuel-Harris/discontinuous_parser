/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constbase;

import java.util.*;

public class ConstInternal extends ConstNode {

	protected String id;

	private String cat;

	private String label;

	private List<ConstNode> children;

	/**
	 * Index of child that is head. -1 if there are no children.
	 */
	private int headIndex;

	public ConstInternal(String id, String cat, String label, List<ConstNode> children, int headIndex) {
		this.id = id;
		this.cat = cat;
		this.label = label;
		this.children = new ArrayList<>(children);
		this.headIndex = headIndex;
	}

	public ConstInternal(String id, String cat, String label) {
		this(id, cat, label, new ArrayList<>(), -1);
	}

	public ConstInternal(String id, String cat) {
		this(id, cat, null);
	}

	public ConstInternal(String id) {
		this(id, "");
	}

	public ConstInternal(final ConstInternal old) {
		this(old.id, old.cat, old.label,
				new ArrayList<>(old.children), old.headIndex);
	}

	/**
	 * Constructor for top node.
	 */
	public ConstInternal() {
		this(null, null, null);
	}

	public boolean isTop() {
		return cat == null;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setCat(final String cat) {
		this.cat = cat;
	}

	public String getCat() {
		return cat;
	}

	public void setLabel(final String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public void clearChildren() {
		children.clear();
		headIndex = -1;
	}

	public void addChildLeft(ConstNode child) {
		children.add(0, child);
		headIndex++;
	}

	public void addChildRight(ConstNode child) {
		children.add(child);
		if (headIndex < 0)
			headIndex = 0;
	}

	public ConstNode[] getChildren() {
		return children.stream().toArray(ConstNode[]::new);
	}

	public ConstNode[] getLeftChildren() {
		return Arrays.asList(getChildren()).subList(0, getHeadIndex())
				.toArray(new ConstNode[0]);
	}

	public ConstNode[] getRightChildren() {
		return Arrays.asList(getChildren()).subList(getHeadIndex()+1, getChildren().length)
				.toArray(new ConstNode[0]);
	}

	/**
	 * Get the children that would have been attached before.
	 * If the given child is a left child, then all children would
	 * already be attached that come to the right of it.
	 * If the next child is a right child, then
	 * children between head and next child would already be attached.
	 *
	 * @param child  Next child to be attached.
	 * @param leftFirst Whether left dependents are to be attached first.
	 * @return The children that would have been attached before.
	 */
	public ConstNode[] getPreviousChildren(final ConstNode child,
												  final boolean leftFirst) {
		final SortedSet<ConstNode> previous = new TreeSet<>();
		int childIndex = children.indexOf(child);
		for (ConstNode c : children)
		if (leftFirst) {
			if (childIndex < headIndex) {
				previous.addAll(children.subList(childIndex+1, headIndex+1));
			} else {
				previous.addAll(children.subList(0, childIndex));
			}
		} else {
			if (childIndex < headIndex) {
				previous.addAll(children.subList(childIndex+1, children.size()));
			} else {
				previous.addAll(children.subList(headIndex, childIndex));
			}
		}
		return previous.toArray(new ConstNode[previous.size()]);
	}

	public void setHeadIndex(int headIndex) {
		this.headIndex = headIndex;
	}

	public int getHeadIndex() {
		return headIndex;
	}

	public ConstLeaf[] getLeaves() {
		Set<ConstLeaf> leaves = new TreeSet<>();
		for (ConstNode node : getChildren())
			leaves.addAll(Arrays.asList(node.getLeaves()));
		return leaves.toArray(new ConstLeaf[leaves.size()]);
	}

	public ConstNode getHeadChild() {
		if (headIndex < 0)
			return null;
		else
			return getChildren()[headIndex];
	}

	public ConstLeaf getHeadLeaf() {
		if (headIndex < 0)
			return null;
		else
			return getHeadChild().getHeadLeaf();
	}

	public void sortChildren() {
		ConstNode headChild = getHeadChild();
		Set<ConstNode> headedChildren = new TreeSet<>(children);
		children = new ArrayList<>(headedChildren);
		headIndex = children.indexOf(headChild);
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(id + " " + cat + " " + label);
		if (!children.isEmpty()) {
			buf.append("\n  ");
			for (ConstNode child : getLeftChildren())
				buf.append(child.getIdentification() + " ");
			buf.append("< " + getChildren()[getHeadIndex()].getIdentification()  + " > ");
			for (ConstNode child : getRightChildren())
				buf.append(child.getIdentification() + " ");
		}
		return buf.toString();
	}

	public String getIdentification() {
		if (cat == null)
			return "TOP";
		else
			return id;
	}
}
