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
 * CoNLL-U token FEATS.
 */
public class Feats {
    /**
     * May be null for an undefined value.
     */
    public final String featsStr;

    public Feats(final String featsStr) {
        if (featsStr.equals("_")) {
            this.featsStr = null;
        } else if (featsStr.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("Wrong FEATS: " + featsStr);
        } else {
            this.featsStr = featsStr;
        }
    }

    public SortedMap<String, String> mapping() {
        return featsStr == null ? new TreeMap<>() :
                Utils.parseMapStringString(featsStr, "|", "=");
    }

    @Override
    public String toString() {
        return featsStr == null ? "_" : featsStr;
    }
}
