/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.depautomata;

import standrews.depbase.Token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimpleConfig {
	protected final List<DependencyVertex> prefix;
	protected final List<String> labels;
	protected final List<DependencyVertex> suffix;

	/**
	 * Only used for certain special parsing strategies, e.g. arc-eager parsing
	 * with the unshift.
	 */
	protected String status = "normal";

	/**
	 * Construct initial configuration for given sentence.
	 *
	 * @param tokens    Tokenized sentence.
	 */
	public SimpleConfig(final Token[] tokens) {
		this(tokens, "");
	}
	public SimpleConfig(final Token[] tokens, final String rootLabel) {
		prefix = Stream.of(Token.ROOT)
				.map(DependencyVertex::new)
				.collect(Collectors.toCollection(ArrayList::new));
		labels = Stream.of(rootLabel)
				.map(String::new)
				.collect(Collectors.toCollection(ArrayList::new));
		suffix = Arrays.stream(tokens)
				.map(Token::getOrphaned)
				.map(DependencyVertex::new)
				.collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Deep copy.
	 */
	public SimpleConfig(final SimpleConfig config) {
		prefix = config.prefix.stream()
				.map(DependencyVertex::new)
				.collect(Collectors.toCollection(ArrayList::new));
		labels = config.labels.stream()
				.map(String::new)
				.collect(Collectors.toCollection(ArrayList::new));
		suffix = config.suffix.stream()
				.map(DependencyVertex::new)
				.collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Is this a final configuration?
	 *
	 * @return True if it is a final configuration.
	 */
	public boolean isFinal() {
		return (prefix.size() == 1 && suffix.isEmpty());
	}

	/**
	 * Get length of prefix.
	 *
	 * @return The length of the prefix.
	 */
	public int prefixLength() {
		return prefix.size();
	}

	/**
	 * Get element from prefix, indexed from left to right.
	 *
	 * @param i Index.
	 * @return The element.
	 */
	public DependencyVertex getPrefixLeft(final int i) {
		return prefix.get(i);
	}

	/**
	 * Get label belonging to element from prefix.
	 */
	public String getLabelLeft(final int i) {
		return labels.get(i);
	}

	public void setLabelLeft(final int i, final String label) {
		labels.set(i, label);
	}

	/**
	 * Get element from prefix, indexed from right to left.
	 *
	 * @param i Index.
	 * @return The element.
	 */
	public DependencyVertex getPrefixRight(final int i) {
		return prefix.get(prefix.size() - 1 - i);
	}

	/**
	 * Get label belonging to element from prefix.
	 */
	public String getLabelRight(final int i) {
		return labels.get(prefix.size() - 1 - i);
	}

	public void setLabelRight(final int i, final String label) {
		labels.set(prefix.size() - 1 - i, label);
	}

	public void addPrefixRight(final DependencyVertex vertex) {
		addPrefixRight(vertex, "");
	}

	public void addPrefixRight(final DependencyVertex vertex, final String label) {
		prefix.add(vertex);
		labels.add(label);
	}

	/**
	 * Remove and return element from prefix, indexed from left to right.
	 *
	 * @param i Index.
	 * @return The removed element.
	 */
	public DependencyVertex removePrefixLeft(final int i) {
		labels.remove(i);
		return prefix.remove(i);
	}

	/**
	 * Remove and return element from prefix, indexed from right to left.
	 *
	 * @param i Index.
	 * @return The removed element.
	 */
	public DependencyVertex removePrefixRight(final int i) {
		final int index = prefix.size() - 1 - i;
		labels.remove(index);
		return prefix.remove(index);
	}

	public ArrayList<DependencyVertex> prefixList() {
		final ArrayList<DependencyVertex> list = new ArrayList<>();
		for (int i = 0; i < prefixLength(); i++)
			list.add(getPrefixLeft(i));
		return list;
	}

	public ArrayList<String> labelList() {
		final ArrayList<String> list = new ArrayList<>();
		for (int i = 0; i < prefixLength(); i++)
			list.add(getLabelLeft(i));
		return list;
	}

	/**
	 * Get length of suffix.
	 *
	 * @return The length of the suffix.
	 */
	public int suffixLength() {
		return suffix.size();
	}

	/**
	 * Get element from suffix, indexed from left to right.
	 *
	 * @param i Index.
	 * @return The element.
	 */
	public DependencyVertex getSuffixLeft(final int i) {
		return suffix.get(i);
	}

	/**
	 * Remove and return leftmost element from suffix.
	 *
	 * @return The element.
	 */
	public DependencyVertex removeSuffixLeft() {
		return suffix.remove(0);
	}

	public void addSuffixLeft(DependencyVertex vertex) {
		suffix.add(0, vertex);
	}

	public ArrayList<DependencyVertex> suffixList() {
		final ArrayList<DependencyVertex> list = new ArrayList<>();
		for (int i = 0; i < suffixLength(); i++)
			list.add(getSuffixLeft(i));
		return list;
	}

	public int totalLength() {
		return prefixLength() + suffixLength();
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String s) {
		status = s;
	}

	/**
	 * Create a dependency structure from configuration.
	 *
	 * @return The dependency structure.
	 */
	public Token[] createParse() {
		return Stream.concat(prefix.stream(), suffix.stream())
				.map(v-> v.getDescendants())
				.flatMap(Arrays::stream)
				.map(v -> v.getToken())
				.filter(t -> !t.equals(Token.ROOT))
				.toArray(Token[]::new);
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < prefix.size(); i++) {
			DependencyVertex vertex = prefix.get(i);
			String label = labels.get(i);
			if (!label.equals(""))
				buf.append(label + " ");
			buf.append(vertex.getToken() + "\n");
		}
		buf.append("-------\n");
		for (DependencyVertex vertex : suffix)
			buf.append(vertex.getToken() + "\n");
		return buf.toString();
	}

}
