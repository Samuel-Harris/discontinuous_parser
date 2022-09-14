/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.lexical;

import standrews.depbase.DependencyStructure;
import standrews.depbase.SentenceFormIterator;
import standrews.depbase.Treebank;
import standrews.depbase.Upos;

import java.util.Arrays;

public abstract class TreebankTagger extends VectorTagger {

	private Word2VecMapping form2vec;

	public TreebankTagger(final String corpus, final int len) {
		super(len, Upos.UPOSS);
		form2vec = new Word2VecMapping();
		form2vec.train(new SentenceFormIterator(corpus), len);
		train(corpus);
	}

	public void train(final String corpus) {
		Treebank treebank = new Treebank(corpus);
		for (DependencyStructure struct : treebank.depStructs) {
			final String[] forms = struct.getFormStrings();
			final String[] tags = struct.getUposStrings();
			final double[][] vecs = Arrays.stream(forms)
					.map(f -> form2vec.get(f))
					.toArray(double[][]::new);
			addTrain(vecs, tags);
		}
	}

	public double eval(final String corpus) {
		Treebank treebank = new Treebank(corpus);
		int n = 0;
		int correct = 0;
		for (DependencyStructure struct : treebank.depStructs) {
			final String[] forms = struct.getFormStrings();
			final String[] tags = struct.getUposStrings();
			final double[][] vecs = Arrays.stream(forms)
					.map(f -> form2vec.get(f))
					.toArray(double[][]::new);
			String[] predicted = predict(vecs);
			for (int i = 0; i < tags.length; i++) {
				// System.out.println("correct " + tags[i]);
				// System.out.println("predicted " + predicted[i]);
				if (predicted[i].equals(tags[i]))
					correct++;
				n++;
			}
		}
		return n == 0 ? 0 : 1.0 * correct / n;
	}

	public String[] predict(final String[] in) {
		final double[][] vecs = Arrays.stream(in)
					.map(f -> form2vec.get(f))
					.toArray(double[][]::new);
		return predict(vecs);
	}

	/**
	 * For testing.
	 * @param args
	 */
	public static void main(String[] args) {
		String dir = "/home/mjn/Data/UniversalDependencies/ud-treebanks-v2.2/UD_English-EWT/";
		String trainCorpus = dir + "en_ewt-ud-train.conllu";
		String testCorpus = dir + "en_ewt-ud-test.conllu";
		TreebankTagger tagger = new NeuralTreebankTagger(trainCorpus, 100);
		double acc = tagger.eval(testCorpus);
		System.out.println("Accuracy is " + acc);
	}
}
