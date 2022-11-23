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

/**
 * CoNLL-U token MISC.
 */
public class Misc {
    /**
     * May be null for an undefined value.
     */
    public final String miscStr;

    public Misc(final String miscStr) {
        if (miscStr.equals("_")) {
            this.miscStr = null;
            // } else if (miscStr.indexOf(' ') >= 0) {
            // throw new IllegalArgumentException("Wrong MISC: " + miscStr);
        } else {
            this.miscStr = miscStr;
        }
    }

    public SortedMap<String, String> mapping() {
        return miscStr == null ? new TreeMap<>() :
                Utils.parseMapStringString(miscStr, "|", "=");
    }

    @Override
    public String toString() {
        return miscStr == null ? "_" : miscStr;
    }
}
