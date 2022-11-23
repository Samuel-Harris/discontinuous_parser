/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.depautomata;

import standrews.depbase.Deprel;
import standrews.depbase.Token;

import java.util.Arrays;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Vertex representing a token, its dependency and its dependants in
 * a dependency graph.
 */
public class DependencyVertex implements Comparable<DependencyVertex> {
    /**
     * The token that the vertex represents.
     */
    private Token token;
    /**
     * The parent of this vertex.
     */
    private DependencyVertex parent;
    /**
     * The vertex's children that have a lower token id than it does.
     */
    private final SortedSet<DependencyVertex> leftChildren;
    /**
     * The vertex's children that have a higher token id than it does.
     */
    private final SortedSet<DependencyVertex> rightChildren;

    /**
     * Construct an isolated vertex representing a consumed token.
     *
     * @param token The token that the vertex represents.
     */
    public DependencyVertex(final Token token) {
        this.token = token;
        parent = null;
        leftChildren = new TreeSet<>();
        rightChildren = new TreeSet<>();
    }

    /**
     * Deep copy.
     *
     * @return
     */
    public DependencyVertex(final DependencyVertex vertex) {
        this(vertex.token);
        for (DependencyVertex child : vertex.getChildren()) {
            DependencyVertex childCopy = new DependencyVertex(child);
            addChild(childCopy);
            childCopy.setParent(this, child.getToken().deprel);
        }
    }

    public Token getToken() {
        return token;
    }

    /**
     * Set the parent of this vertex to a given vertex.
     */
    public void setParent(final DependencyVertex parent) {
        this.parent = parent;
    }

    /**
     * Set the parent of this vertex to a given vertex and change head and deprel.
     *
     * @param parent The vertex that will be this vertex's parent.
     * @param deprel The new Deprel.
     */
    public void setParent(final DependencyVertex parent, Deprel deprel) {
        setParent(parent);
        token = token.getParented(parent.token.id, deprel);
    }

    /**
     * Get the vertex's parent vertex.
     *
     * @return The parent vertex of this vertex or null if it does not have one.
     */
    public DependencyVertex getParent() {
        return parent;
    }

    /**
     * Set the children of this vertex.
     *
     * @param children The array of children to give the vertex.
     */
    public void setChildren(final DependencyVertex[] children) {
        leftChildren.clear();
        rightChildren.clear();
        for (final DependencyVertex child : children) {
            addChild(child);
        }
    }

    /**
     * Add a vertex as a child to this vertex.
     *
     * @param child The child vertex to add.
     */
    public void addChild(final DependencyVertex child) {
        if (compareTo(child) > 0) {
            leftChildren.add(child);
        } else if (compareTo(child) < 0) {
            rightChildren.add(child);
        }
    }

    /**
     * Get the children that precede this vertex in sentence order.
     *
     * @return An array of the child vertices with a lower token id
     * than this vertex, in ascending order of token id.
     */
    public DependencyVertex[] getLeftChildren() {
        return leftChildren.stream().toArray(DependencyVertex[]::new);
    }

    /**
     * Get the children that follow this vertex in sentence order.
     *
     * @return An array of the child vertices with a higher token id
     * than this vertex, in ascending order of token id.
     */
    public DependencyVertex[] getRightChildren() {
        return rightChildren.stream().toArray(DependencyVertex[]::new);
    }

    /**
     * Get the vertex's children, in sentence order.
     *
     * @return An array of child vertices, in ascending order of token id.
     */
    public DependencyVertex[] getChildren() {
        return Stream.concat(leftChildren.stream(), rightChildren.stream())
                .toArray(DependencyVertex[]::new);
    }

    /**
     * Get the children that would have been attached before.
     * If the given child is a left child, then all children would
     * already be attached that come to the right of it.
     * If the next child is a right child, then
     * children between head and next child would already be attached.
     *
     * @param c         Next child to be attached.
     * @param leftFirst Whether left dependents are to be attached first.
     * @return The children that would have been attached before.
     */
    public DependencyVertex[] getPreviousChildren(final DependencyVertex c,
                                                  final boolean leftFirst) {
        final SortedSet<DependencyVertex> previous = new TreeSet<>();
        for (DependencyVertex prev : getChildren()) {
            if (leftFirst) {
                if (c.compareTo(this) < 0) {
                    if (c.compareTo(prev) < 0 && prev.compareTo(this) < 0)
                        previous.add(prev);
                } else {
                    if (prev.compareTo(c) < 0)
                        previous.add(prev);
                }
            } else {
                if (this.compareTo(c) < 0) {
                    if (this.compareTo(prev) < 0 && prev.compareTo(c) < 0)
                        previous.add(prev);
                } else {
                    if (c.compareTo(prev) < 0)
                        previous.add(prev);
                }
            }
        }
        return previous.toArray(new DependencyVertex[previous.size()]);
    }

    /**
     * Get vertex and children and descendants, in order.
     */
    public DependencyVertex[] getDescendants() {
        final SortedSet<DependencyVertex> descs = new TreeSet<>();
        descs.add(this);
        for (DependencyVertex child : getChildren())
            descs.addAll(Arrays.asList(child.getDescendants()));
        return descs.toArray(new DependencyVertex[descs.size()]);
    }

    /**
     * Get ancestors, in order.
     */
    public DependencyVertex[] getAncestors() {
        final SortedSet<DependencyVertex> ancs = new TreeSet<>();
        ancs.add(this);
        if (parent != null)
            ancs.addAll(Arrays.asList(getAncestors()));
        return ancs.toArray(new DependencyVertex[ancs.size()]);
    }


    /**
     * Get leftmost dependency relation in subtree.
     *
     * @return Dependency relation or null if there is none.
     */
    public Deprel getLeftmostDeprel() {
        if (getLeftChildren().length > 0) {
            DependencyVertex child = getLeftChildren()[0];
            Deprel recur = child.getLeftmostDeprel();
            return recur != null ? recur : child.getToken().deprel;
        } else
            return null;
    }

    /**
     * Get rightmost dependency relation in subtree.
     *
     * @return Dependency relation or null if there is none.
     */
    public Deprel getRightmostDeprel() {
        if (getRightChildren().length > 0) {
            DependencyVertex child = getRightChildren()[getRightChildren().length - 1];
            Deprel recur = child.getRightmostDeprel();
            return recur != null ? recur : child.getToken().deprel;
        } else
            return null;
    }

    /**
     * Does a path following only child edges exist between this node and the target node?
     *
     * @param target The target of the path from this vertex.
     * @return True if there is a path through only child edges from this node to the target node.
     */
    public boolean hasPathTo(final DependencyVertex target) {
        return target.equals(this) ||
                target.parent != null && hasPathTo(target.parent);
    }

    public int hashCode() {
        return Objects.hash(getToken().id.hashCode());
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof DependencyVertex && compareTo((DependencyVertex) other) == 0;
    }

    /**
     * Compare this dependency vertex to another by token.
     *
     * @param other The other dependency vertex to compare this one to.
     * @return The comparison value of the two vertices' tokens.
     */
    @Override
    public int compareTo(final DependencyVertex other) {
        return token.compareTo(other.token);
    }

    public String toString(String indent) {
        final DependencyVertex[] children = getChildren();
        final StringBuilder sb = new StringBuilder();
        sb.append(indent + token.id + " (" + token.form + ") [" + children.length + "]:");
        for (final DependencyVertex child : children) {
            sb.append("\n" + child.toString(indent + "\t"));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toString("");
    }

}