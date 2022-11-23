/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constbase;

import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;

import java.util.Arrays;
import java.util.stream.Collectors;

public class SentenceWordIterator implements SentenceIterator {
    protected final ConstTreebank treebank;
    protected SentencePreProcessor preproc;
    protected int i;

    public SentenceWordIterator(final ConstTreebank treebank) {
        this.treebank = treebank;
        this.i = 0;
    }

    public void finish() {
    }

    public SentencePreProcessor getPreProcessor() {
        return preproc;
    }

    @Override
    public boolean hasNext() {
        return i < treebank.getTrees().length;
    }

    @Override
    public String nextSentence() {
        final ConstTree tree = treebank.getTrees()[i++];
        final ConstLeaf[] leaves = tree.getLeaves();
        final String s = Arrays.stream(leaves)
                .map(t -> flattenLeaf(t))
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
     * How is leaf translated to string?
     *
     * @param leaf
     * @return
     */
    protected String flattenLeaf(final ConstLeaf leaf) {
        return leaf.getForm();
    }

}
