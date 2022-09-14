/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.depautomata;

import standrews.depbase.Id;
import standrews.depbase.Token;

import java.util.Map;
import java.util.TreeMap;

/**
 * Rooted, connected dependency graph of tokens.
 */
public class DependencyGraph {
	/**
	 * The set of vertices in the graph, indexed by token ID.
	 */
	private final Map<Id, DependencyVertex> vertices;
	/**
	 * The root vertex of the graph.
	 */
	public final DependencyVertex root;

	/**
	 * Construct a dependency graph from a list of tokens.
	 *
	 * @param tokens The tokens from which to construct the graph.
	 */
	public DependencyGraph(final Token[] tokens) {
		vertices = new TreeMap<>();
		for (final Token token : tokens) {
			vertices.put(token.id, new DependencyVertex(token));
		}
		root = new DependencyVertex(Token.ROOT);
		vertices.put(Token.ROOT.id, root);
		connectVertices();
	}

	/**
	 * Get the vertex representing the token with the given ID.
	 *
	 * @param id The token ID of the vertex to fetch.
	 * @return The dependency vertex for the given token ID.
	 */
	public DependencyVertex getVertex(final Id id) {
		return vertices.get(id);
	}

	/**
	 * Get the token with the given ID.
	 *
	 * @param id The token ID of the token to fetch.
	 * @return The token with the given ID.
	 */
	public Token getToken(final Id id) {
		return getVertex(id).getToken();
	}

	/**
	 * Connect the vertices of the graph to each other.
	 */
	private void connectVertices() {
		for (final DependencyVertex vertex : vertices.values()) {
			if (vertex != root) {
				final DependencyVertex parent = vertices.get(vertex.getToken().head);
				if (parent == null)
					throw new IllegalArgumentException("Strange vertex: " + vertex);
				vertex.setParent(parent);
				parent.addChild(vertex);
			}
		}
	}

	/**
	 * Is one token the dependent of another in the graph?
	 *
	 * @param dep  Token ID that is dependant.
	 * @param head Token ID that is head.
	 * @return True if the first token is the head of the second.
	 */
	public boolean isDependentOf(final Id dep, final Id head) {
		DependencyVertex vDep = getVertex(dep);
		DependencyVertex vHead = getVertex(head);
		return vDep.getParent() == vHead;
	}

	/**
	 * Get the vertices that come between the two bounds (non-inclusive) in the sentence order.
	 *
	 * @param from One end of the vertex range.
	 * @param to   The other end of the vertex range.
	 * @return Array of vertices with token IDs between the two vertices at the ends of the range,
	 * in order of IDs.
	 */
	private DependencyVertex[] verticesBetween(final DependencyVertex from, final DependencyVertex to) {
		return vertices.values().stream()
				.filter(x ->
						(from.compareTo(x) <= 0 && x.compareTo(to) <= 0) ||
								(to.compareTo(x) <= 0 && x.compareTo(from) <= 0))
				.toArray(DependencyVertex[]::new);
	}

	/**
	 * Is the tree projective? That is, do all vertices have continuous span?
	 *
	 * @return True if the tree is projective.
	 */
	public boolean isProjective() {
		return isProjective(root);
	}

	/**
	 * Is subtree below vertex projective?
	 */
	private boolean isProjective(final DependencyVertex head) {
		for (final DependencyVertex v : head.getDescendants())
			if (!isContinuous(v))
				return false;
		return true;
	}

	/**
	 * Is the list of descendants of the vertex free of gaps?
	 *
	 * @param v Vertex.
	 * @return Whether the list of descendants is continuous.
	 */
	public boolean isContinuous(final DependencyVertex v) {
		final DependencyVertex[] descs = v.getDescendants();
		final DependencyVertex[] betweens = verticesBetween(descs[0], descs[descs.length - 1]);
		return descs.length == betweens.length;
	}

	/**
	 * Compute a mapping from vertices to the top-most vertex in a maximal projective component.
	 */
	public Map<Id, Id> componentMap() {
		Map<Id, Id> vertexToTop = new TreeMap<>();
		for (DependencyVertex head : vertices.values())
			if (isProjective(head)) {
				if (head.getParent() == null || !isProjective(head.getParent()))
					for (DependencyVertex desc : head.getDescendants())
						vertexToTop.put(desc.getToken().id, head.getToken().id);
			} else {
				vertexToTop.put(head.getToken().id, head.getToken().id);
			}
		return vertexToTop;
	}

	/**
	 * Compute mapping from vertices to indices in projectivized structure,
	 * by inorder traversal.
	 */
	public Map<Id, Integer> projectiveOrder() {
		Map<Id, Integer> vertexToIndex = new TreeMap<>();
		projectiveOrder(root, 0, vertexToIndex);
		return vertexToIndex;
	}

	/**
	 * Compute mapping from vertices to indices in projectivized structure,
	 * by inorder traversal.
	 */
	private int projectiveOrder(final DependencyVertex v, int next, final Map<Id, Integer> map) {
		for (DependencyVertex child : v.getLeftChildren())
			next = projectiveOrder(child, next, map);
		map.put(v.getToken().id, next++);
		for (DependencyVertex child : v.getRightChildren())
			next = projectiveOrder(child, next, map);
		return next;
	}

	@Override
	public String toString() {
		return root.toString();
	}
}
