/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.classification;


import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Features {

    /**
     * Feature values are strings.
     */
    private final TreeMap<String, String> stringFeatures;
    /**
     * Feature values are sets of strings.
     */
    private final TreeMap<String, TreeSet<String>> setFeatures;

    /**
     * Feature values of arrays of doubles.
     */
    private final TreeMap<String, double[]> vectorFeatures;

    public Features() {
        stringFeatures = new TreeMap<>();
        setFeatures = new TreeMap<>();
        vectorFeatures = new TreeMap<>();
    }

    public void putString(final String key, final String val) {
        stringFeatures.put(key, val);
    }

    public void putSet(final String key, final TreeSet<String> set) {
        setFeatures.put(key, set);
    }

    public void putVector(final String key, final double[] vector) {
        vectorFeatures.put(key, vector);
    }

    public Set<String> stringKeys() {
        return stringFeatures.keySet();
    }

    public Set<String> setKeys() {
        return setFeatures.keySet();
    }

    public Set<String> vectorKeys() {
        return vectorFeatures.keySet();
    }

    public String stringVal(final String key) {
        return stringFeatures.get(key);
    }

    public Set<String> setVal(final String key) {
        return setFeatures.get(key);
    }

    public double[] vectorVal(final String key) {
        return vectorFeatures.get(key);
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        for (String key : stringKeys()) {
            buf.append(key + "->" + stringVal(key) + "\n");
        }
        for (String key : setKeys()) {
            buf.append(key + "->");
            for (String elem : setVal(key))
                buf.append(" " + elem);
            buf.append("\n");
        }
        for (String key : vectorKeys()) {
            buf.append(key + "->" + vectorVal(key) + "\n");
        }
        return buf.toString();
    }
}
