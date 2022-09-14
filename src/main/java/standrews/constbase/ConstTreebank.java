/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constbase;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

public class ConstTreebank {

	protected String id;

	protected Set<String> poss;

	protected Set<String> cats;

	protected Set<String> labels;

	protected ConstTree[] trees;

	public ConstTreebank(String id,
						 Set<String> poss, Set<String> cats, Set<String> labels,
						 ConstTree[] trees) {
		this.id = id;
		this.poss = poss;
		this.cats = cats;
		this.labels = labels;
		this.trees = trees;
	}

	public ConstTreebank() {
		this("", new TreeSet<>(), new TreeSet<>(), new TreeSet<>(),
				new ConstTree[0]);
	}

	public String getId() {
		return id;
	}

	public Set<String> getPoss() {
		return poss;
	}

	public Set<String> getCats() {
		return cats;
	}

	public Set<String> getPossAndCats() {
		Set<String> all = new TreeSet<String>(poss);
		poss.addAll(cats);
		return all;
	}

	public Set<String> getLabels() {
		return labels;
	}

	public ConstTree[] getTrees() {
		return trees;
	}

	public int nTrees() {
		return trees.length;
	}

	public int nWords() {
		int nWords = 0;
		for (ConstTree tree : trees) {
			nWords += tree.length();
		}
		return nWords;
	}

	public boolean checkSymbolConsistency() {
		for (ConstTree tree : trees)
			if (!tree.isSymbolConsistent(poss, cats, labels))
				return false;
		return true;
	}

	public void checkCycles() {
		for (ConstTree tree : trees)
			if (tree.hasCycle())
				System.out.println(tree.id);
	}

	public void removeCycles() {
		for (ConstTree tree : trees) {
			while (tree.removeOneCycle()) {
			}
		}
	}

	public void gatherSymbols() {
		for (ConstTree tree : trees)
			tree.gatherSymbols(poss, cats, labels);
	}

	public ConstTreebank part(int from, int to) {
		ConstTree[] partTrees = Arrays.asList(trees).subList(from, to)
				.toArray(new ConstTree[0]);
		return new ConstTreebank(id, poss, cats, labels, partTrees);
	}

	private Logger logger() {
		final Logger log = Logger.getLogger(getClass().getName());
		log.setParent(Logger.getGlobal());
		return log;
	}

}
