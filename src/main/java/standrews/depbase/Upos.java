/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.depbase;

import java.util.Arrays;

/**
 * CoNLL-U token UPOS.
 */
public class Upos {
    /**
     * May be null for an undefined value.
     */
    public final String upos;

    /**
     * Allowable values.
     */
    public static final String[] UPOSS = new String[]{
            "ADJ", "ADP", "ADV", "AUX", "CCONJ", "DET", "INTJ", "NOUN",
            "NUM", "PART", "PRON", "PROPN", "PUNCT", "SCONJ", "SYM", "VERB", "X"};

    /**
     * Parse a UPOS.
     *
     * @param uposStr The string to parse to a UPOS.
     * @throws IllegalArgumentException if the string is not a valid UPOS.
     */
    public Upos(final String uposStr) {
        if (uposStr.equals("_"))
            this.upos = null;
        else if (Arrays.asList(UPOSS).contains(uposStr))
            this.upos = uposStr;
        else
            throw new IllegalArgumentException("Wrong UPOS: " + uposStr);
    }

    @Override
    public String toString() {
        return upos == null ? "_" : upos;
    }
}
