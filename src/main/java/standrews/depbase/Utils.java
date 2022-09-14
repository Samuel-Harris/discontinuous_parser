/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.depbase;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * CoNLL-U treebank utilities.
 */
public final class Utils {
	/**
	 * Is the line a comment?
	 *
	 * @param line The line.
	 * @return True if the line is a comment.
	 */
	public static boolean isCommentLine(final String line) {
		return line.startsWith("#");
	}

	/**
	 * Parse a map from Ks to Vs, with a key-value pair separator and key-value assignment
	 * operator, and parsers for keys and values.
	 *
	 * @param <K>        The map's key type.
	 * @param <V>        The map's value type.
	 * @param value      The value to parse to a map.
	 * @param separator  The separator between map entries.
	 * @param assignment The assignment operator within a map entry.
	 * @param keyParser  A function to parse keys.
	 * @param valParser  A function to parse values.
	 * @return A sorted map consumed from the passed string.
	 */
	private static <K extends Comparable<K>, V> SortedMap<K, V> parseMap(
			final String value,
			final String separator,
			final String assignment,
			final Function<String, K> keyParser,
			final Function<String, V> valParser) {
		final SortedMap<K, V> result = new TreeMap<>();
		if (!value.equals("_")) {
			for (final String pair : value.split(Pattern.quote(separator))) {
				final String[] keyVal = pair.split(Pattern.quote(assignment), 2);
				if (keyVal.length == 2) {
					result.put(keyParser.apply(keyVal[0]), valParser.apply(keyVal[1]));
				} else {
					throw new IllegalArgumentException("Wrong pair: " + pair);
				}
			}
		}
		return result;
	}

	/**
	 * Print a map from Ks to Vs, with a key-value pair separator and key-value assignment
	 * symbol, and printers for keys and values.
	 *
	 * @param <K>        The map's key type.
	 * @param <V>        The map's value type.
	 * @param map        The map to print.
	 * @param separator  The separator between map entries.
	 * @param assignment The assignment operator within a map entry.
	 * @param keyPrinter A function to print keys.
	 * @param valPrinter A function to print values.
	 * @return A string representation of the map.
	 */
	private static <K extends Comparable<K>, V> String printMap(
			final SortedMap<K, V> map,
			final String separator,
			final String assignment,
			final Function<K, String> keyPrinter,
			final Function<V, String> valPrinter) {
		if (map.isEmpty()) {
			return "_";
		}
		final StringBuilder sb = new StringBuilder();
		for (final SortedMap.Entry<K, V> entry : map.entrySet()) {
			if (sb.length() != 0)
				sb.append(separator);
			sb.append(keyPrinter.apply(entry.getKey()));
			sb.append(assignment);
			sb.append(valPrinter.apply(entry.getValue()));
		}
		return sb.toString();
	}

	/**
	 * Parse a map from strings to strings, using the given key-value pair separator and key-value assignment symbol.
	 *
	 * @param value      The string from which to parse the map.
	 * @param separator  The separator between map entries.
	 * @param assignment The assignment operator within a map entry.
	 * @return A sorted map consumed from the passed string.
	 */
	public static SortedMap<String, String> parseMapStringString(final String value,
																 final String separator, final String assignment) {
		return parseMap(value, separator, assignment, Function.identity(), Function.identity());
	}

	/**
	 * Parse a map from token IDs to Deprels, using the given key-value pair separator and key-value assignment operator.
	 *
	 * @param value      The string from which to parse the map.
	 * @param separator  The separator between map entries.
	 * @param assignment The assignment operator within a map entry.
	 * @return A sorted map consumed from the passed string.
	 */
	public static SortedMap<Id, Deprel> parseMapIdDeprel(final String value,
														 final String separator, final String assignment) {
		return parseMap(value, separator, assignment, Id::new, Deprel::new);
	}

	/**
	 * Print a map from strings to strings, using the given key-value pair separator and key-value assignment operator.
	 *
	 * @param map        The map to print.
	 * @param separator  The separator between map entries.
	 * @param assignment The assignment operator within a map entry.
	 * @return A string representation of the map.
	 */
	public static String printMapStringString(final SortedMap<String, String> map,
											  final String separator, final String assignment) {
		return printMap(map, separator, assignment, Function.identity(), Function.identity());
	}

	/**
	 * Print a map from token identifiers to strings, using the given key-value pair separator and key-value assignment symbol.
	 *
	 * @param map        The map to print.
	 * @param separator  The separator between map entries.
	 * @param assignment The assignment operator within a map entry.
	 * @return A string representation of the map.
	 */
	public static String printMapIdDeprel(final SortedMap<Id, Deprel> map,
											   final String separator, final String assignment) {
		return printMap(map, separator, assignment, Id::toString, Deprel::toString);
	}
}
