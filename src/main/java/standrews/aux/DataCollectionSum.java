/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.aux;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * Mapping from strings to parameters to averages. Keeping only sums.
 */
public class DataCollectionSum {

    public TreeMap<String, TreeMap<Integer, Double>> timings = new TreeMap<>();
    public TreeMap<String, TreeMap<Integer, Integer>> nObs = new TreeMap<>();

    public void clear() {
        timings = new TreeMap<>();
        nObs = new TreeMap<>();
    }

    public void add(final String id, final int param, final double t) {
        add(id, param, t, 1);
    }

    public void add(final String id, final int param, final double t, final int n) {
        if (timings.get(id) == null) {
            timings.put(id, new TreeMap<>());
            nObs.put(id, new TreeMap<>());
        }
        if (timings.get(id).get(param) == null) {
            timings.get(id).put(param, t);
            nObs.get(id).put(param, n);
        } else {
            timings.get(id).put(param, timings.get(id).get(param) + t);
            nObs.get(id).put(param, nObs.get(id).get(param) + n);
        }
    }

    public int nObservations(final String id, final int param) {
        if (nObs.get(id) == null)
            return 0;
        if (nObs.get(id).get(param) == null)
            return 0;
        return nObs.get(id).get(param);
    }

    public String observationStrings() {
        StringBuffer b = new StringBuffer();
        for (String id : timings.keySet())
            b.append(id + "\n" + nObservations(id, 0) + " times\n");
        return b.toString();
    }

    /**
     * List averages with number of decimal places.
     *
     * @param prec
     */
    public void list(final int prec) {
        for (Map.Entry<String, TreeMap<Integer, Double>> entry1 : timings.entrySet()) {
            String id = entry1.getKey();
            System.out.println(id);
            for (Map.Entry<Integer, Double> entry2 : entry1.getValue().entrySet()) {
                int param = entry2.getKey();
                double sum = entry2.getValue();
                int n = nObs.get(id).get(param);
                double av = sum / n;
                System.out.println(param + " " + averageStr(av, prec));
            }
        }
    }

    public double[] averages(final String id) {
        final TreeMap<Integer, Double> pairs = timings.get(id);
        if (pairs == null)
            return new double[0];
        else {
            double[] ar = new double[2 * pairs.size()];
            int index = 0;
            for (Map.Entry<Integer, Double> entry : pairs.entrySet()) {
                int param = entry.getKey();
                double sum = entry.getValue();
                int n = nObs.get(id).get(param);
                ar[index++] = param;
                ar[index++] = sum / n;
            }
            return ar;
        }
    }

    public String averagesString(final String id) {
        return averagesString(id, 1.0);
    }

    public String averagesString(final String id, final double scale) {
        double[] av = averages(id);
        String s = "";
        for (int i = 0; i < av.length; i += 2)
            s += "(" + av[i] + "," +
                    String.format("%5." + 5 + "f", av[i + 1] * scale) + ")";
        return s;
    }

    public String averagesStrings() {
        StringBuffer b = new StringBuffer();
        for (String id : timings.keySet())
            b.append(id + "\n" + averagesString(id) + "\n");
        return b.toString();
    }

    public static double average(final ArrayList<Double> ts) {
        return ts.stream().mapToDouble(a -> a).average().getAsDouble();
    }

    public static double geoAverage(final ArrayList<Double> ts) {
        return Math.exp(ts.stream().mapToDouble(a -> Math.log(a)).average().getAsDouble());
    }

    public static String averageStr(final ArrayList<Double> ts, final int prec) {
        return averageStr(average(ts), prec);
    }

    public static String averageStr(final double d, final int prec) {
        return String.format("%5." + prec + "f", d);
    }

    public void store(String fileName) {
        try {
            PrintWriter pw = new PrintWriter(new File(fileName));
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, TreeMap<Integer, Double>> entry1 : timings.entrySet()) {
                String id = entry1.getKey();
                for (Map.Entry<Integer, Double> entry2 : entry1.getValue().entrySet()) {
                    int param = entry2.getKey();
                    double sum = entry2.getValue();
                    int n = nObs.get(id).get(param);
                    sb.append(id);
                    sb.append(',');
                    sb.append(param);
                    sb.append(',');
                    sb.append(n);
                    sb.append(',');
                    sb.append(sum);
                    sb.append('\n');
                }
            }
            pw.write(sb.toString());
            pw.close();
        } catch (FileNotFoundException e) {
            System.err.println("Could not write: " + fileName);
        }
    }

    public void retrieve(String fileName) {
        try {
            String line = "";
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    String id = parts[0];
                    int param = Integer.parseInt(parts[1]);
                    int n = Integer.parseInt(parts[2]);
                    double sum = Double.parseDouble(parts[3]);
                    add(id, param, sum, n);
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Could not read: " + fileName);
        } catch (IOException e) {
            System.err.println("Could not read: " + fileName);
        }
    }

    public static DataCollectionSum globalCollection = new DataCollectionSum();
}
