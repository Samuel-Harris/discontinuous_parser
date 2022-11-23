/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.depbase;

import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Iterator over sentences in treebank.
 */
public abstract class SentenceTokenIterator implements SentenceIterator {
    protected final Treebank treebank;
    protected SentencePreProcessor preproc;
    protected int i;

    public SentenceTokenIterator(final String path) {
        this.treebank = new Treebank(path);
        this.i = 0;
    }

    public void finish() {
    }

    public SentencePreProcessor getPreProcessor() {
        return preproc;
    }

    @Override
    public boolean hasNext() {
        return i < treebank.depStructs.length;
    }

    @Override
    public String nextSentence() {
        final DependencyStructure struct = treebank.depStructs[i++];
        final Token[] tokens = struct.getNormalTokens();
        final String s = Arrays.stream(tokens)
                .map(t -> flattenToken(t))
                .collect(Collectors.joining(" "));
        return s;
    }

    public void reset() {
        i = 0;
    }

    public void setPreProcessor(SentencePreProcessor preproc) {
        this.preproc = preproc;
    }

    /**
     * How is token translated to string?
     *
     * @param token
     * @return
     */
    protected abstract String flattenToken(final Token token);
}

