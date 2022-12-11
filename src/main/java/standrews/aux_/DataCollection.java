/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.aux_;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * Mapping from strings to parameters to series of values.
 */
public class DataCollection {

    public final TreeMap<String, TreeMap<Integer, ArrayList<Double>>> timings = new TreeMap<>();

    public void add(final String id, final int param, final double t) {
        if (timings.get(id) == null)
            timings.put(id, new TreeMap<Integer, ArrayList<Double>>());
        if (timings.get(id).get(param) == null)
            timings.get(id).put(param, new ArrayList<Double>());
        timings.get(id).get(param).add(t);
    }

    public int nObservations(final String id, final int param) {
        if (timings.get(id) == null)
            return 0;
        if (timings.get(id).get(param) == null)
            return 0;
        return timings.get(id).get(param).size();
    }

    /**
     * List averages with number of decimal places.
     *
     * @param prec
     */
    public void list(final int prec) {
        for (Map.Entry<String, TreeMap<Integer, ArrayList<Double>>> entry1 : timings.entrySet()) {
            System.out.println(entry1.getKey());
            for (Map.Entry<Integer, ArrayList<Double>> entry2 : entry1.getValue().entrySet()) {
                System.out.println(entry2.getKey() + " " + averageStr(entry2.getValue(), prec));
            }
        }
    }

    public double[] averages(final String id) {
        final TreeMap<Integer, ArrayList<Double>> pairs = timings.get(id);
        if (pairs == null)
            return new double[0];
        else {
            double[] ar = new double[2 * pairs.size()];
            int index = 0;
            for (Map.Entry<Integer, ArrayList<Double>> entry : pairs.entrySet()) {
                ar[index++] = entry.getKey();
                ar[index++] = average(entry.getValue());
            }
            return ar;
        }
    }

    public double[] geoAverages(final String id) {
        final TreeMap<Integer, ArrayList<Double>> pairs = timings.get(id);
        if (pairs == null)
            return new double[0];
        else {
            double[] ar = new double[2 * pairs.size()];
            int index = 0;
            for (Map.Entry<Integer, ArrayList<Double>> entry : pairs.entrySet()) {
                ar[index++] = entry.getKey();
                ar[index++] = geoAverage(entry.getValue());
            }
            return ar;
        }
    }

    public String averagesString(final String id, final double scale) {
        double[] av = averages(id);
        String s = "";
        for (int i = 0; i < av.length; i += 2)
            s += "(" + av[i] + "," +
                    String.format("%5." + 3 + "f", av[i + 1] * scale) + ")";
        return s;
    }

    public String geoAveragesString(final String id, final double scale) {
        double[] av = geoAverages(id);
        String s = "";
        for (int i = 0; i < av.length; i += 2)
            s += "(" + av[i] + "," +
                    String.format("%5." + 3 + "f", av[i + 1] * scale) + ")";
        return s;
    }

    public static double average(final ArrayList<Double> ts) {
        return ts.stream().mapToDouble(a -> a).average().getAsDouble();
    }

    public static double geoAverage(final ArrayList<Double> ts) {
        return Math.exp(ts.stream().mapToDouble(a -> Math.log(a)).average().getAsDouble());
    }

    public static String averageStr(final ArrayList<Double> ts, final int prec) {
        double d = average(ts);
        return String.format("%5." + prec + "f", d);
    }

    public static String geoAverageStr(final ArrayList<Double> ts, final int prec) {
        double d = geoAverage(ts);
        return String.format("%5." + prec + "f", d);
    }
}
