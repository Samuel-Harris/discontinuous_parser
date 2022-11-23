/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.depbase;

/**
 * CoNLL-U token ID.
 */
public final class Id implements Comparable<Id> {
    /**
     * Invalid index, used for missing values.
     */
    public static final int INVALID = -1;
    /**
     * Default root token ID.
     */
    public static final Id ROOT = new Id(0, 0, 0);
    /**
     * The major index.
     */
    public final int major;
    /**
     * The minor index.
     */
    public final int minor;
    /**
     * The end index of range.
     */
    public final int end;

    /**
     * Create a token id with given major, minor and end.
     *
     * @param major Major index.
     * @param minor Minor index.
     * @param end   End index.
     */
    private Id(final int major, final int minor, final int end) {
        this.major = major;
        this.minor = minor;
        this.end = end;
    }

    /**
     * Parse a token ID from a string.
     *
     * @param idStr The string to parse.
     * @throws IllegalArgumentException if the indices are wrong.
     */
    public Id(final String idStr) {
        // Missing value
        if (idStr.equals("_")) {
            major = INVALID;
            minor = INVALID;
            end = INVALID;
            return;
        }
        // Major and end
        final String[] rangeParts = idStr.split("-");
        if (rangeParts.length == 2) {
            major = Integer.parseInt(rangeParts[0]);
            minor = 0;
            end = Integer.parseInt(rangeParts[1]);
            if (end <= major)
                throw new IllegalArgumentException("Wrong ID: " + idStr);
            return;
        }
        // Major and minor
        final String[] subParts = idStr.split("\\.");
        if (subParts.length == 2) {
            major = Integer.parseInt(subParts[0]);
            minor = Integer.parseInt(subParts[1]);
            if (minor < 1)
                throw new IllegalArgumentException("Wrong ID: " + idStr);
            end = 0;
            return;
        }
        // Major
        major = Integer.parseInt(subParts[0]);
        minor = 0;
        end = 0;
    }

    /**
     * Is a token ID the root?
     *
     * @return True if it is the root ID.
     */
    public boolean isRoot() {
        return equals(ROOT);
    }

    /**
     * Is this missing value?
     */
    public boolean isMissing() {
        return major < 0;
    }

    /**
     * Is the token ID only a major value?
     *
     * @return True if it has no minor index and no end index.
     */
    public boolean isMajor() {
        return major >= 0 && minor == 0 && end == 0;
    }

    /**
     * Does the token ID have a minor index?
     *
     * @return True if it has a minor index.
     */
    public boolean hasMinor() {
        return minor > 0;
    }

    /**
     * Is the token ID a range of tokens?
     *
     * @return True if it is a range of tokens.
     */
    public boolean isRange() {
        return end > major;
    }

    @Override
    public int hashCode() {
        return major * 10000 + minor * 100 + end;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof Id && compareTo((Id) other) == 0;
    }

    @Override
    public int compareTo(final Id other) {
        final int comparedMajor = Integer.compare(major, other.major);
        if (comparedMajor != 0) {
            return comparedMajor;
        }
        final int comparedMinor = Integer.compare(minor, other.minor);
        if (comparedMinor != 0) {
            return comparedMinor;
        }
        return -Integer.compare(end, other.end);
    }

    @Override
    public String toString() {
        if (isMissing()) {
            return "_";
        } else if (hasMinor()) {
            return Integer.toString(major) + "." + Integer.toString(minor);
        } else if (isRange()) {
            return Integer.toString(major) + "-" + Integer.toString(end);
        } else
            return Integer.toString(major);
    }
}
