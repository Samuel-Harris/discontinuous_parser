/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.depbase;

import java.util.*;

/**
 * CoNLL-U dependency structure.
 */
public class DependencyStructure {
    /**
     * Comments preceding the tokens.
     */
    public final List<String> comments;
    /**
     * Map of tokens indexed by their ID.
     */
    private final SortedMap<Id, Token> idToToken;

    /**
     * Construct a structure from the tokens that make it up.
     *
     * @param comments Lines preceding structure,
     * @param tokens   The tokens that make up the sentence.
     */
    public DependencyStructure(final String[] comments, final Token[] tokens) {
        this.comments = Arrays.asList(comments);
        idToToken = constructMap(tokens);
    }

    public DependencyStructure(final Token[] tokens) {
        this(new String[0], tokens);
    }

    /**
     * Parse a sentence from the lines representing it in the treebank.
     *
     * @param comments   The comment lines.
     * @param tokenLines The token lines.
     */
    public DependencyStructure(final String[] comments, final String[] tokenLines) {
        this.comments = Arrays.asList(comments);
        final Token[] tokens = Arrays.stream(tokenLines)
                .map(line -> new Token(line.split("\\t")))
                .toArray(Token[]::new);
        idToToken = constructMap(tokens);
    }

    /**
     * Construct a map from token IDs to tokens from an array of tokens.
     *
     * @param tokens The tokens from which to construct the map.
     * @return A map from token IDs to tokens.
     */
    private static SortedMap<Id, Token> constructMap(final Token[] tokens) {
        final SortedMap<Id, Token> index = new TreeMap<>();
        for (final Token token : tokens) {
            index.put(token.id, token);
        }
        return index;
    }

    /**
     * Get only major tokens, no ranges or empty nodes.
     * Only take universal deprels.
     *
     * @return The major tokens, with universal deprels.
     */
    public Token[] getNormalTokens() {
        return idToToken.values().stream()
                .filter(t -> t.id.isMajor())
                .map(t -> t.getUniDeprel())
                .toArray(Token[]::new);
    }

    /**
     * Get sequence of word forms.
     */
    public Form[] getForms() {
        return Arrays.stream(getNormalTokens())
                .map(t -> t.form)
                .toArray(Form[]::new);
    }

    /**
     * Get sequence of word form strings.
     */
    public String[] getFormStrings() {
        return Arrays.stream(getForms())
                .map(f -> f.toString())
                .toArray(String[]::new);
    }

    /**
     * Get sequence of Upos.
     */
    public Upos[] getUposs() {
        return Arrays.stream(getNormalTokens())
                .map(t -> t.upos)
                .toArray(Upos[]::new);
    }

    /**
     * Get sequence of Upos strings.
     */
    public String[] getUposStrings() {
        return Arrays.stream(getUposs())
                .map(u -> u.toString())
                .toArray(String[]::new);
    }

    /**
     * Fetch the token in the sentence with the given ID.
     *
     * @param id The ID.
     * @return The token.
     */
    public Token getToken(final Id id) {
        return idToToken.get(id);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (String comment : comments) {
            sb.append(comment + "\n");
        }
        for (Token token : idToToken.values()) {
            sb.append(token.toString() + "\n");
        }
        return sb.toString();
    }
}
