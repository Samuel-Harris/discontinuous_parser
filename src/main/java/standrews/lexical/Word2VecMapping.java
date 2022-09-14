/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.lexical;

import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import standrews.depbase.SentenceFormIterator;

import java.util.Collection;

public class Word2VecMapping extends Embedding {

	private int minWordFrequency = 4;
	private int iterations = 50;
	private int layerSize = 0;
	private int windowSize = 1;
	private int vocabSize = 10000;

	protected Word2Vec vec = null;
	private String unknownWord;

	/**
	 * Empty constructor. Maps everything to the empty vector.
	 */
	public Word2VecMapping() {
	}

	public void setMinWordFrequency(int minWordFrequency) {
		this.minWordFrequency = minWordFrequency;
	}

	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
	}

	/**
	 *
	 * @param path Path to corpus.
	 * @param len Layersize and length of vectors, typically 100.
	 */
	// public void train(final String path, final int len) {
	public void train(final SentenceIterator iter, final int len) {
		// final Treebank treebank = new Treebank(path);
		layerSize = len;
		TokenizerFactory t = new DefaultTokenizerFactory();
		// SentenceIterator iter = new SentIterator(treebank);
		vec = new Word2Vec.Builder()
				.minWordFrequency(minWordFrequency)
				.limitVocabularySize(vocabSize)
				.useUnknown(true)
                .iterations(iterations)
                .layerSize(layerSize)
                .seed(42)
                .windowSize(windowSize)
                .iterate(iter)
                .tokenizerFactory(t)
                .build();
		unknownWord = vec.getUNK();
		vec.fit();
	}

	public int getLength() {
		return layerSize;
	}

	public double[] get(final String word) {
		if (vec == null)
			return new double[0];
		double[] v = vec.getWordVector(word);
		if (v == null) {
			double[] unk = vec.getWordVector(unknownWord);
			if (unk == null)
				return new double[layerSize];
			else
				return normalize ? normalizeUnit(unk) : unk;
		} else {
			return normalize ? normalizeUnit(v) : v;
		}
	}

	/**
	 * Return dummy vector of zeros only.
	 * @return
	 */
	public double[] get() {
		return new double[layerSize];
	}

	public static void main(String[] args) {
		Word2VecMapping m = new Word2VecMapping();
		m.train(new SentenceFormIterator("/home/mjn/Data/UniversalDependencies/ud-treebanks-v2.2/UD_English-EWT/en_ewt-ud-train.conllu"), 100);
		double[] king = m.get("king");
		double[] queen = m.get("queen");
		double[] actress = m.get("actress");
		Collection<String> lst = m.vec.wordsNearestSum("day", 10);
		for (String n : lst)
			System.out.println(n);
		System.out.println("ok");
	}


}
