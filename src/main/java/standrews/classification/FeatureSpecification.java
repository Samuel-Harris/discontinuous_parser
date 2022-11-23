/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.classification;

import standrews.lexical.*;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public class FeatureSpecification {

    private TreeMap<String,Integer> intFeatures = new TreeMap<>();

    private TreeMap<String,List<Integer>> intsFeatures = new TreeMap<>();

    private Embedding formVec = new EmptyEmbedding();

    private Embedding lemmaVec = new EmptyEmbedding();

    private boolean goldPos = true;

    private PosTagger posTagger = null;

    public void setIntFeature(final String name, final int val) {
        intFeatures.put(name, val);
    }

    public int getIntFeature(final String name, final int def) {
        if (intFeatures.get(name) == null)
            return def;
        else
            return intFeatures.get(name);
    }

    public void setIntsFeature(final String name) {
        setIntsFeature(name, new LinkedList<>());
    }

    public void setIntsFeature(final String name, final int min, final int max) {
        final LinkedList<Integer> list = new LinkedList<>();
        for (int i = min; i <= max; i++)
            list.add(i);
        setIntsFeature(name, list);
    }

    public void setIntsFeature(final String name, final List<Integer> list) {
        intsFeatures.put(name, list);
    }

    public List<Integer> getIntsFeature(final String name) {
        if (intsFeatures.get(name) == null) {
            System.err.println("Could not find feature " + name);
            System.exit(1);
            return new LinkedList<>();
        } else {
            return intsFeatures.get(name);
        }
    }

    public boolean hasIntsFeature(final String name) {
        return intsFeatures.get(name) != null
                && intsFeatures.get(name).size() > 0;
    }

    public void setFormVec(final Embedding e) {
        this.formVec = e;
    }

    public Embedding getFormVec() {
        return formVec;
    }

    public void setLemmaVec(final Embedding e) {
        this.lemmaVec = e;
    }

    public Embedding getLemmaVec() {
        return lemmaVec;
    }

    public void setGoldPos(final boolean goldPos) {
        this.goldPos = goldPos;
    }

    public boolean getGoldPos() {
        return goldPos;
    }

    public void setPosTagger(final PosTagger tagger) {
        this.posTagger = tagger;
    }

    public PosTagger getPosTagger() {
        return posTagger;
    }

}
