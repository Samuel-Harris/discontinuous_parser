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
 * CoNLL-U token DEPREL.
 */
public class Deprel {
    /**
     * May be null for an undefined value.
     */
    public final String deprel;

    /**
     * Universal dependency relations.
     */
    public static final String[] DEPRELS = new String[]{
            "acl", "advcl", "advmod", "amod", "appos", "aux", "case", "cc", "ccomp", "clf",
            "compound", "conj", "cop", "csubj", "dep", "det", "discourse", "dislocated", "expl",
            "fixed", "flat", "goeswith", "iobj", "list", "mark", "nmod", "nsubj", "nummod", "obj",
            "obl", "orphan", "parataxis", "punct", "reparandum", "root", "vocative", "xcomp"};

    /**
     * Parse a dependency relation from a string or throw an exception if its
     * prefix does not match a universal dependency relation.
     *
     * @param deprelStr The string to parse to a Deprel.
     * @throws IllegalArgumentException if the string is not a valid Deprel.
     */
    public Deprel(final String deprelStr) {
        if (deprelStr.equals("_")) {
            deprel = null;
        } else if (deprelStr.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("Wrong DEPREL: " + deprelStr);
        } else {
            String[] parts = split(deprelStr);
            if (parts.length < 1 || !Arrays.asList(DEPRELS).contains(parts[0]))
                throw new IllegalArgumentException("Wrong DEPREL: " + deprelStr);
            deprel = deprelStr;
        }
    }

    private static String[] split(final String s) {
        return s.split(":");
    }

    /**
     * Are Deprels equal?
     */
    public boolean equals(final Object other) {
        return other instanceof Deprel && toString().equals(((Deprel) other).toString());
    }

    /**
     * Universal part of a Deprel.
     *
     * @return String representation of universal Deprel.
     */
    public String uniPart() {
        return deprel == null ? "_" : split(deprel)[0];
    }

    @Override
    public String toString() {
        return deprel == null ? "_" : deprel;
    }
}
