/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.lexical;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GloveMapping extends Embedding {

    private HashMap<String, double[]> map = new HashMap<>(1000);

    private int len = 0;

    private String unknownWord = "unk";

    public GloveMapping(final String path, final int len) {
        this.len = len;
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String fields[] = line.split(" ");
                String form = fields[0];
                double[] v = new double[fields.length - 1];
                for (int i = 1; i < fields.length; i++)
                    v[i - 1] = Double.parseDouble(fields[i]);
                if (len != fields.length - 1)
                    throw new IOException("Different-sized vectors in: " + path);
                map.put(form, v);
            }
        } catch (NumberFormatException e) {
            System.err.println("Cannot read double in file: " + path);
            System.exit(-1);
        } catch (IOException e) {
            System.err.println("Cannot read file: " + path);
            System.exit(-1);
        }
    }

    public int getLength() {
        return len;
    }

    public double[] get(final String word) {
        double[] v = map.get(word.toLowerCase());
        if (v == null) {
            double[] unk = map.get(unknownWord);
            if (unk == null)
                return new double[len];
            else
                return normalize ? normalizeUnit(unk) : unk;
        } else {
            return normalize ? normalizeUnit(v) : v;
        }
    }

    public double[] get() {
        return new double[len];
    }

    public String findNearestEuclidean(double[] v) {
        String bestString = "none";
        double bestDist = Double.MAX_VALUE;
        for (Map.Entry<String, double[]> entry : map.entrySet()) {
            String nextString = entry.getKey();
            double[] v2 = get(nextString);
            double nextDist = Embedding.distEuclidean(v, v2);
            if (nextDist < bestDist) {
                bestDist = nextDist;
                bestString = nextString;
            }
        }
        return bestString;
    }

    public String findNearestAngle(double[] v) {
        String bestString = "none";
        double bestDist = Double.MIN_VALUE;
        for (Map.Entry<String, double[]> entry : map.entrySet()) {
            String nextString = entry.getKey();
            double[] v2 = get(nextString);
            double nextDist = Embedding.distAngle(v, v2);
            if (nextDist > bestDist) {
                bestDist = nextDist;
                bestString = nextString;
            }
        }
        return bestString;
    }

    public static void main(String[] args) {
        GloveMapping m = new GloveMapping("/home/mjn/Data/GloVe/glove.6B.50d.txt", 50);
        // GloveMapping m = new GloveMapping("/home/mjn/Data/GloVe/glove.6B.100d.txt", 100);
        m.setNormalize(false);
        double[] king = m.get("king");
        double[] queen = m.get("queen");
        double[] actress = m.get("actress");
        double[] man = m.get("man");
        double[] woman = m.get("woman");
        double[] prince = m.get("prince");
        double[] princess = m.get("princess");
        double[] actorTarget = Embedding.add(Embedding.subtract(king, queen), actress);
        System.out.println("actor (eucl) " + m.findNearestEuclidean(actorTarget));
        System.out.println("actor (angle) " + m.findNearestAngle(actorTarget));
        double[] queenTarget = Embedding.add(Embedding.subtract(king, man), woman);
        System.out.println("queen " + m.findNearestEuclidean(queenTarget));
        double[] queenTarget1 = Embedding.add(Embedding.subtract(king, prince), princess);
        System.out.println("queen " + m.findNearestEuclidean(queenTarget1));
        double[] paris = m.get("Paris");
        double[] france = m.get("France");
        double[] italy = m.get("Italy");
        double[] rome = m.get("Rome");
        double[] romeTarget = Embedding.add(Embedding.subtract(paris, france), italy);
        System.out.println("rome (eucl) " + m.findNearestEuclidean(romeTarget));
        System.out.println("rome (angle) " + m.findNearestAngle(romeTarget));
        double[] china = m.get("China");
        double[] taiwan = m.get("Taiwan");
        double[] russia = m.get("Russia");
        double[] ukraine = m.get("Russia");
        double[] ukraineTarget = Embedding.add(Embedding.subtract(china, taiwan), russia);
        double[] russiaTarget = Embedding.add(Embedding.subtract(china, taiwan), ukraine);
        System.out.println("ukraine " + m.findNearestEuclidean(ukraineTarget));
        System.out.println("russia " + m.findNearestEuclidean(russiaTarget));
        double[] trump = m.get("Trump");
        double[] obama = m.get("Obama");
        double[] republican = m.get("Republican");
        double[] democratic = m.get("Democratic");
        double[] trumpTarget = Embedding.add(Embedding.subtract(republican, democratic), obama);
        System.out.println("trump (eucl) " + m.findNearestEuclidean(trumpTarget));
        System.out.println("trump (angle) " + m.findNearestAngle(trumpTarget));
    }

}
