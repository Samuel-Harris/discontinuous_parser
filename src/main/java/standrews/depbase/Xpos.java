/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.depbase;

/**
 * CoNLL-U token XPOS.
 */
public class Xpos {
    /**
     * May be null for an undefined value.
     */
    public final String xpos;

    /**
     * Parse a XPOS.
     *
     * @param xposStr The string to parse to a XPOS.
     * @throws IllegalArgumentException if the string is not a valid XPOS.
     */
    public Xpos(final String xposStr) {
        if (xposStr.equals("_"))
            this.xpos = null;
        else if (xposStr.indexOf(' ') >= 0)
            throw new IllegalArgumentException("Wrong XPOS: " + xposStr);
        else
            this.xpos = xposStr;
    }

    @Override
    public String toString() {
        return xpos == null ? "_" : xpos;
    }
}
