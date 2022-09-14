/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.lexical;

import standrews.depbase.DependencyStructure;
import standrews.depbase.Treebank;
import standrews.depbase.Token;
import standrews.depbase.Upos;

import java.util.*;
import java.util.stream.Collectors;

public class HMMTagger implements PosTagger {

	private class StringSequence {
		public final String[] strings;

		public StringSequence(final String[] strings) {
			this.strings = strings;
		}

		public StringSequence() {
			this(new String[0]);
		}

		public int length() {
			return strings.length;
		}

		public boolean equals(final Object o) {
			if (!(o instanceof StringSequence))
				return false;
			else {
				final StringSequence other = (StringSequence) o;
				if (other.length() != this.length()) {
					return false;
				} else {
					for (int i = 0; i < other.length(); i++)
						if (!other.strings[i].equals(this.strings[i]))
							return false;
					return true;
				}
			}
		}

		public StringSequence prefix() {
			return prefix(length() - 1);
		}

		public StringSequence prefix(int n) {
			if (length() <= n) {
				System.err.println("wrong use of prefix()");
				System.exit(-1);
			}
			String[] shorter = new String[n];
			System.arraycopy(strings, 0, shorter, 0, n);
			return new StringSequence(shorter);
		}

		public StringSequence suffix(int n) {
			if (length() <= n) {
				return this;
			} else {
				String[] shorter = new String[n];
				System.arraycopy(strings, length() - n, shorter, 0, n);
				return new StringSequence(shorter);
			}
		}

		public String last() {
			return strings[strings.length - 1];
		}

		public int hashCode() {
			return Arrays.asList(this.strings).stream().
					collect(Collectors.joining("--")).hashCode();
		}

		public String toString() {
			StringBuffer buf = new StringBuffer();
			for (String s : strings)
				buf.append(s + " ");
			return buf.toString();
		}
	}

	public StringSequence concat(StringSequence history, String next) {
		String[] strings = new String[history.length() + 1];
		System.arraycopy(history.strings, 0, strings, 0, history.length());
		strings[history.length()] = next;
		return new StringSequence(strings);
	}

	private class WordPosPair {
		public final String word;
		public final String pos;
		public WordPosPair(final String word, final String pos) {
			this.word = word;
			this.pos = pos;
		}
		public boolean equals(final Object o) {
			if (!(o instanceof WordPosPair))
				return false;
			else {
				final WordPosPair other = (WordPosPair) o;
				return other.word.equals(this.word) &&
						other.pos.equals(this.pos);
			}
		}

		public int hashCode() {
			return (word + "--" + pos).hashCode();
		}
	}

	private class PossWordPair {

		public final StringSequence poss;
		public final String word;
		public PossWordPair(final StringSequence poss, final String word) {
			this.poss = poss;
			this.word = word;
		}
		public boolean equals(final Object o) {
			if (!(o instanceof PossWordPair))
				return false;
			else {
				final PossWordPair other = (PossWordPair) o;
				return other.word.equals(this.word) &&
						other.poss.equals(this.poss);
			}
		}

		public int hashCode() {
			return (word + "--" + poss.toString()).hashCode();
		}
	}

	/**
	 * N-grams for which N.
	 */
	protected final int N;

	/**
	 * Map word to frequency.
	 */
	protected final HashMap<String,Integer> wordFreq = new HashMap<>();

	/**
	 * Map pos to frequency.
	 */
	protected final HashMap<String,Integer> posFreq = new HashMap<>();

	/**
	 * Map combination of word and POS to frequency.
	 */
	protected final HashMap<WordPosPair,Integer> wordPosFreq = new HashMap<>();

	/**
	 * Map combination of poss and word to frequency.
	 */
	protected final HashMap<PossWordPair,Integer> possWordFreq = new HashMap<>();

	/**
	 * Map N-gram (truncated at start of sentence) to frequency.
	 */
	protected final HashMap<StringSequence,Integer> sequenceFreq = new HashMap<>();

	/**
	 * Map (N-1)-gram (truncated at start of sentence) to frequency.
	 */
	protected final HashMap<StringSequence,Integer> prevSequenceFreq = new HashMap<>();

	protected final String startMarker = "start marker";

	protected final String endMarker = "end marker";

	protected final String unknownWord = "unknown word";

	protected final int rareThreshold = 3;

	protected int beamSize = -1;

	public HMMTagger(final int N, final String corpus) {
		this.N = N;
		train(corpus);
	}

	public void setBeamSize(int beamSize) {
		this.beamSize = beamSize;
	}

	private void train(final String corpus) {
		Treebank treebank = new Treebank(corpus);
		countWords(treebank);
		countContexts(treebank);
	}

	private void countWords(Treebank treebank) {
		for (final DependencyStructure struct : treebank.depStructs) {
			final String[] forms = struct.getFormStrings();
			for (String form : forms)
				addWord(form);
		}
	}

	private void countContexts(Treebank treebank) {
		for (final DependencyStructure struct : treebank.depStructs) {
			final String[] forms = struct.getFormStrings();
			replaceRareWords(forms);
			final String[] tags = struct.getUposStrings();
			addCounts(forms, tags);
		}
	}

	private void replaceRareWords(String[] forms) {
		for (int i = 0; i < forms.length; i++)
			if (isRareWord(forms[i]))
				forms[i] = unknownWord;
	}

	private void addCounts(final String[] forms, final String[] tags) {
		String[] tagsExt = extend(tags);
		for (int i = 0; i < tags.length; i++) {
			addWordPos(forms[i], tags[i]);
			addPos(tags[i]);
		}
		for (int i = 1; i <= tagsExt.length; i++) {
			int j = Math.max(0, i - N);
			int len = i - j;
			String[] gram = new String[len];
			System.arraycopy(tagsExt, j, gram, 0, len);
			StringSequence seq = new StringSequence(gram);
			addPrevSequence(seq.prefix());
			addSequence(seq);
			if (i < tagsExt.length)
				addPossWord(seq, forms[i-1]);
		}
	}

	private String[] extend(String[] tags) {
		String[] tagsExt = new String[tags.length + 1];
		System.arraycopy(tags, 0, tagsExt, 0, tags.length);
		tagsExt[tags.length] = endMarker;
		return tagsExt;
	}

	private void addWord(String word) {
		if (wordFreq.get(word) == null)
			wordFreq.put(word, 0);
		wordFreq.put(word, wordFreq.get(word) + 1);
	}

	private int getWordFreq(String word) {
		if (wordFreq.get(word) == null)
			return 0;
		else {
			return wordFreq.get(word);
		}
	}

	public boolean isRareWord(String word) {
		return getWordFreq(word) <= rareThreshold;
	}

	public boolean isUnknownWord(String word) {
		return getWordFreq(word) == 0;
	}

	private void addPos(String pos) {
		if (posFreq.get(pos) == null)
			posFreq.put(pos, 0);
		posFreq.put(pos, posFreq.get(pos) + 1);
	}

	private int getPosFreq(String pos) {
		if (posFreq.get(pos) == null)
			return 0;
		else {
			return posFreq.get(pos);
		}
	}

	private void addWordPos(String word, String pos) {
		WordPosPair pair = new WordPosPair(word, pos);
		if (wordPosFreq.get(pair) == null)
			wordPosFreq.put(pair, 0);
		wordPosFreq.put(pair, wordPosFreq.get(pair) + 1);
	}

	private int getWordPosFreq(String word, String pos) {
		WordPosPair pair = new WordPosPair(word, pos);
		if (wordPosFreq.get(pair) == null)
			return 0;
		else
			return wordPosFreq.get(pair);
	}

	private void addPossWord(StringSequence poss, String word) {
		PossWordPair pair = new PossWordPair(poss, word);
		if (possWordFreq.get(pair) == null)
			possWordFreq.put(pair, 0);
		possWordFreq.put(pair, possWordFreq.get(pair) + 1);
	}

	private int getPossWordFreq(StringSequence poss, String word) {
		PossWordPair pair = new PossWordPair(poss, word);
		if (possWordFreq.get(pair) == null)
			return 0;
		else
			return possWordFreq.get(pair);
	}

	private void addSequence(StringSequence seq) {
		if (sequenceFreq.get(seq) == null)
			sequenceFreq.put(seq, 0);
		sequenceFreq.put(seq, sequenceFreq.get(seq) + 1);
	}

	private void addPrevSequence(StringSequence seq) {
		if (prevSequenceFreq.get(seq) == null)
			prevSequenceFreq.put(seq, 0);
		prevSequenceFreq.put(seq, prevSequenceFreq.get(seq) + 1);
	}

	private int getSequenceFreq(StringSequence seq) {
		if (sequenceFreq.get(seq) == null)
			return 0;
		else {
			return sequenceFreq.get(seq);
		}
	}

	private int getPrevSequenceFreq(StringSequence seq) {
		if (prevSequenceFreq.get(seq) == null)
			return 0;
		else {
			return prevSequenceFreq.get(seq);
		}
	}

	public double eval(final String corpus) {
		Treebank treebank = new Treebank(corpus);
		int n = 0;
		int correct = 0;
		for (DependencyStructure struct : treebank.depStructs) {
			final String[] forms = struct.getFormStrings();
			final String[] tags = struct.getUposStrings();
			final String[] predicted = predictSum(forms);
			// final String[] predicted = predict(forms);
			for (int i = 0; i < tags.length; i++) {
				if (predicted[i].equals(tags[i]))
					correct++;
				n++;
			}
		}
		return n == 0 ? 0 : 1.0 * correct / n;
	}

	private class SequenceProb extends HashMap<StringSequence,Double> {
	}
	private class BackPointer extends HashMap<StringSequence,StringSequence> {
	}

	private class Table {
		public int len;
		public SequenceProb[] forwardProb;
		public BackPointer[] backpointer;
		public SequenceProb[] backwardProb;

		public Table(int len) {
			this.len = len;
			forwardProb = new SequenceProb[len+1];
			backpointer = new BackPointer[len+1];
			backwardProb = new SequenceProb[len+1];
			for (int i = 0; i <= len; i++) {
				forwardProb[i] = new SequenceProb();
				backpointer[i] = new BackPointer();
				backwardProb[i] = new SequenceProb();
			}
			forwardProb[0].put(new StringSequence(), 1.0);
		}

		public Set<Map.Entry<StringSequence,Double>> getForwardPair(int i) {
			return forwardProb[i].entrySet();
		}

		public double getBackward(int i, StringSequence seq) {
			if (backwardProb[i].get(seq) == null)
				return 0;
			else
				return backwardProb[i].get(seq);
		}

		public void improveForward(int i, StringSequence seq,
								   double prob, StringSequence prev) {
			if (forwardProb[i].get(seq) == null || prob > forwardProb[i].get(seq)) {
				forwardProb[i].put(seq, prob);
				backpointer[i].put(seq, prev);
			}
		}

		public void addForward(int i, StringSequence seq, double prob) {
			if (forwardProb[i].get(seq) == null)
				forwardProb[i].put(seq, 0.0);
			forwardProb[i].put(seq, forwardProb[i].get(seq) + prob);
		}

		public void addBackward(int i, StringSequence seq, double prob) {
			if (backwardProb[i].get(seq) == null)
				backwardProb[i].put(seq, 0.0);
			backwardProb[i].put(seq, backwardProb[i].get(seq) + prob);
		}

		public void prune(int i) {
			if (beamSize < 0)
				return;
			double[] probs = forwardProb[i].values()
					.stream()
					.mapToDouble(Double::doubleValue)
					.toArray();
			Arrays.sort(probs);
			if (beamSize < probs.length) {
				double threshold = probs[probs.length-beamSize];
				forwardProb[i].entrySet().removeIf(entry -> entry.getValue() < threshold);
			}
		}

		public String[] backTrace(StringSequence seq) {
			String[] strings = dummy();
			if (seq == null)
				return strings;
			Vector<String> poss = new Vector<>();
			for (int i = len; i > 0; i--) {
				poss.add(0, seq.strings[seq.length()-1]);
				seq = backpointer[i].get(seq);
			}
			return poss.stream().toArray(String[]::new);
		}

		public String[] sumBestPos() {
			String[] strings = dummy();
			for (int i = 0; i < len; i++) {
				strings[i] = highestSumPos(i);
				if (strings[i] == null)
					return strings;
			}
			return strings;
		}

		public double[][] sumList() {
			double[][] probs = new double[len][];
			for (int i = 0; i < len; i++)
				probs[i] = sumList(i);
			return probs;
		}

		public double[] sumList(int i) {
			TreeMap<String,Double> poss = posSums(i);
			List<String> sortedPos = new LinkedList<>(posFreq.keySet());
			double[] probs = new double[sortedPos.size()];
			for (int j = 0; j < sortedPos.size(); j++) {
				if (poss.get(sortedPos.get(j)) != null)
					probs[j] = poss.get(sortedPos.get(j));
			}
			return probs;
		}

		private String highestSumPos(int i) {
			String bestPos = dummyString();
			double bestProb = 0;
			TreeMap<String,Double> poss = posSums(i);
			if (poss.size() == 0)
				return null;
			for (Map.Entry<String,Double> posPair : poss.entrySet()) {
				String p = posPair.getKey();
				double prob = posPair.getValue();
				if (prob > bestProb) {
					bestPos = p;
					bestProb = prob;
				}
			}
			return bestPos;
		}

		private TreeMap<String,Double> posSums(int i) {
			TreeMap<String,Double> posSums = new TreeMap<>();
			for (Map.Entry<StringSequence,Double> pair : getForwardPair(i+1)) {
				StringSequence seq = pair.getKey();
				double probForward = pair.getValue();
				double probBackward = getBackward(i+1, seq);
				double prob = probForward * probBackward;
				String last = seq.last();
				if (posSums.get(last) == null)
					posSums.put(last, 0.0);
				posSums.put(last, posSums.get(last) + prob);
			}
			return posSums;
		}

		private String dummyString() {
			return Upos.UPOSS[0];
		}

		private String[] dummy() {
			String[] strings = new String[len];
			for (int i = 0; i < len; i++)
				strings[i] = dummyString();
			return strings;
		}
	}

	public Token[] retag(final Token[] in) {
		String[] words = new String[in.length];
		Token[] retaggedTokens = new Token[in.length];
		for (int i = 0; i < in.length; i++)
			words[i] = in[i].form.toString();
		String[] pos = tag(words);
		for (int i = 0; i < in.length; i++)
			retaggedTokens[i] = in[i].getUposRetagged(pos[i]);
		return retaggedTokens;
	}

	public String[] tag(final String[] in) {
		return predictSum(in);
		// return predict(in);
	}

	public String[] predict(final String[] in) {
		Table table = new Table(in.length);
		for (int i = 0; i < in.length; i++) {
			String word = isRareWord(in[i]) ? unknownWord : in[i];
			for (String pos : posFreq.keySet()) {
				double emisProb = emission(word, pos);
				if (emisProb > 0) {
					for (Map.Entry<StringSequence,Double> p : table.getForwardPair(i)) {
						StringSequence prevSeq = p.getKey();
						double prevProb = p.getValue();
						StringSequence seq = concat(prevSeq, pos);
						double transProb = transitionLaplace(seq);
						emisProb = emissionLaplace(seq, word);
						double prob = prevProb * transProb * emisProb;
						if (prob > 0) {
							StringSequence truncSeq = seq.suffix(N - 1);
							table.improveForward(i+1, truncSeq, prob, prevSeq);
						}
					}
				}
			}
			table.prune(i+1);
		}
		double bestProb = 0;
		StringSequence backSeq = null;
		for (Map.Entry<StringSequence,Double> p : table.getForwardPair(in.length)) {
			StringSequence prevSeq = p.getKey();
			double prevProb = p.getValue();
			StringSequence seq = concat(prevSeq, endMarker);
			double transProb = transitionLaplace(seq);
			double prob = prevProb * transProb;
			if (prob > 0) {
				if (prob > bestProb) {
					bestProb = prob;
					backSeq = prevSeq;
				}
			}
		}
		if (backSeq == null)
			System.err.println("failed");
		return table.backTrace(backSeq);
	}

	public String[] predictSum(final String[] in) {
		Table table = fillTableSum(in);
		return table.sumBestPos();
	}

	public double[][] predictVector(final String[] in) {
		Table table = fillTableSum(in);
		return table.sumList();
	}

	private Table fillTableSum(final String[] in) {
		Table table = new Table(in.length);
		for (int i = 0; i < in.length; i++) {
			String word = isRareWord(in[i]) ? unknownWord : in[i];
			for (String pos : posFreq.keySet()) {
				double emisProb = emission(word, pos);
				if (emisProb > 0) {
					for (Map.Entry<StringSequence,Double> p : table.getForwardPair(i)) {
						StringSequence prevSeq = p.getKey();
						double prevProb = p.getValue();
						StringSequence seq = concat(prevSeq, pos);
						double transProb = transitionLaplace(seq);
						emisProb = emissionLaplace(seq, word);
						double prob = prevProb * transProb * emisProb;
						if (prob > 0) {
							StringSequence truncSeq = seq.suffix(N - 1);
							table.addForward(i+1, truncSeq, prob);
						}
					}
				}
			}
			table.prune(i+1);
		}
		double forwardProb = 0;
		for (Map.Entry<StringSequence,Double> p : table.getForwardPair(in.length)) {
			StringSequence prevSeq = p.getKey();
			double prevProb = p.getValue();
			StringSequence seq = concat(prevSeq, endMarker);
			double transProb = transitionLaplace(seq);
			double prob = prevProb * transProb;
			forwardProb += prob;
		}
		if (forwardProb == 0)
			System.err.println("failed");
		for (Map.Entry<StringSequence,Double> p : table.getForwardPair(in.length)) {
			StringSequence prevSeq = p.getKey();
			StringSequence seq = concat(prevSeq, endMarker);
			double transProb = transitionLaplace(seq);
			table.addBackward(in.length, prevSeq, transProb);
		}
		for (int i = in.length - 1; i >= 0; i--) {
			String word = isRareWord(in[i]) ? unknownWord : in[i];
			for (String pos : posFreq.keySet()) {
				double emisProb = emission(word, pos);
				if (emisProb > 0) {
					for (Map.Entry<StringSequence,Double> p : table.getForwardPair(i)) {
						StringSequence prevSeq = p.getKey();
						StringSequence seq = concat(prevSeq, pos);
						double transProb = transitionLaplace(seq);
						StringSequence truncSeq = seq.suffix(N - 1);
						double nextProb = table.getBackward(i+1, truncSeq);
						emisProb = emissionLaplace(seq, word);
						double prob = transProb * emisProb * nextProb;
						table.addBackward(i, prevSeq, prob);
					}
				}
			}
		}
		/*
		// Sanity check. Should be about equal.
		double backwardProb = table.getBackward(0, new StringSequence());
		System.out.println("forward " + forwardProb);
		System.out.println("backward " + backwardProb);
		*/
		return table;
	}

	private double emission(String word, String pos) {
		double numerator = getWordPosFreq(word, pos);
		double denominator = getPosFreq(pos);
		return denominator == 0 ? 0 : numerator / denominator;
	}


	private double emissionLaplace(StringSequence poss, String word) {
		double extra = 0.1;
		int corpusSize = 1000; // purely imaginary
		double numerator = getPossWordFreq(poss, word) + extra;
		double denominator = getSequenceFreq(poss) + extra * corpusSize;
		return denominator == 0 ? 0 : numerator / denominator;
	}

	private double transition(StringSequence seq) {
		StringSequence prev = seq.prefix();
		double numerator = getSequenceFreq(seq);
		double denominator = getPrevSequenceFreq(prev);
		return denominator == 0 ? 0 : numerator / denominator;
	}

	private double transitionLaplace(StringSequence seq) {
		double extra = 0.1;
		StringSequence prev = seq.prefix();
		double numerator = getSequenceFreq(seq) + extra;
		double denominator = getPrevSequenceFreq(prev) + extra * posFreq.keySet().size();
		return denominator == 0 ? 0 : numerator / denominator;
	}


	/**
	 * For testing.
	 * @param args
	 */
	public static void main(String[] args) {
		String dir = "/home/mjn/Data/UniversalDependencies/ud-treebanks-v2.2/UD_English-EWT/";
		String trainCorpus = dir + "en_ewt-ud-train.conllu";
		String testCorpus = dir + "en_ewt-ud-test.conllu";
		HMMTagger tagger = new HMMTagger(2, trainCorpus);
		double acc = tagger.eval(testCorpus);
		System.out.println("Accuracy is " + acc);
	}
}
