/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.lexical;

public abstract class Embedding {

    protected boolean normalize = false;

    public void setNormalize(boolean b) {
        normalize = b;
    }

    public abstract int getLength();

    public abstract double[] get(final String word);

    public abstract double[] get();

    protected static double[] normalizeUnit(double[] v) {
        if (v.length == 0)
            return v;
        double dist = 0;
        for (int i = 0; i < v.length; i++) {
            dist += v[i] * v[i];
        }
        dist = Math.sqrt(dist);
        double[] w = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            w[i] = v[i] / dist;
        }
        return w;
    }

    protected static double[] normalizeAffine(double[] v, double diff, double fact) {
        double[] w = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            w[i] = (v[i] + diff) * fact;
        }
        return w;
    }

    public static double[] add(double[] v1, double[] v2) {
        double[] v3 = new double[v1.length];
        for (int i = 0; i < v1.length; i++)
            v3[i] = v1[i] + v2[i];
        return v3;
    }

    public static double[] subtract(double[] v1, double[] v2) {
        return add(v1, neg(v2));
    }

    public static double[] neg(double[] v1) {
        double[] v2 = new double[v1.length];
        for (int i = 0; i < v1.length; i++)
            v2[i] = -v1[i];
        return v2;
    }

    public static double distEuclidean(double[] v1, double[] v2) {
        double sum = 0;
        for (int i = 0; i < v1.length; i++) {
            sum += (v1[i] - v2[i]) * (v1[i] - v2[i]);
        }
        return Math.sqrt(sum);
    }

    public static double distAngle(double[] v1, double[] v2) {
        v1 = Embedding.normalizeUnit(v1);
        v2 = Embedding.normalizeUnit(v2);
        double sum = 0;
        for (int i = 0; i < v1.length; i++) {
            sum += v1[i] * v2[i];
        }
        return sum;
    }

}
