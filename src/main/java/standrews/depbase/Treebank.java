/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.depbase;

import standrews.depautomata.DependencyGraph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * CoNLL-U treebank containing any number of dependency structures.
 */
public class Treebank {
	/**
	 * The dependency structures.
	 */
	public final DependencyStructure[] depStructs;

	/**
	 * Construct a treebank from given dependency structures.
	 *
	 * @param depStructs The dependency structures.
	 */
	public Treebank(final DependencyStructure[] depStructs) {
		this.depStructs = new DependencyStructure[depStructs.length];
		System.arraycopy(depStructs, 0, this.depStructs, 0, depStructs.length);
	}

	/**
	 * Read treebank from file in CoNLL-U form.
	 *
	 * @param filename Path of source file.
	 * @param maxSize Maximum number of trees to be read.
	 */
	public Treebank(final String filename, int maxSize) {
		final List<DependencyStructure> sentences = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			final List<String> commentLines = new ArrayList<>();
			final List<String> tokenLines = new ArrayList<>();
			String line;
			while ((line = br.readLine()) != null && sentences.size() < maxSize) {
				if (Utils.isCommentLine(line))
					commentLines.add(line);
				else if (line.isEmpty()) {
					if (!tokenLines.isEmpty()) {
						sentences.add(new DependencyStructure(
								commentLines.toArray(new String[0]),
								tokenLines.toArray(new String[0])));
					}
					commentLines.clear();
					tokenLines.clear();
				} else {
					tokenLines.add(line);
				}
			}
			if (!tokenLines.isEmpty()) {
				sentences.add(new DependencyStructure(
						commentLines.toArray(new String[0]),
						tokenLines.toArray(new String[0])));
			}
		} catch (IOException e) {
			final Logger log = Logger.getLogger(getClass().getName());
			log.setParent(Logger.getGlobal());
			log.severe("Could not read treebank, error reading file: " + filename);
			System.exit(1);
		} catch (IllegalArgumentException e) {
			final Logger log = Logger.getLogger(getClass().getName());
			log.setParent(Logger.getGlobal());
			log.severe("Could not read treebank, error reading file: " +
					filename + "\n" + e);
			System.exit(1);
		}
		this.depStructs = sentences.toArray(new DependencyStructure[0]);
	}

	public Treebank(final String filename) {
		this(filename, Integer.MAX_VALUE);
	}

	public Treebank(final File file) {
		this(file.getPath());
	}

	public Treebank(final File file, int maxSize) {
		this(file.getPath(), maxSize);
	}

	public int nTrees() {
		return depStructs.length;
	}

	public int nNonprojTrees() {
		int nonproj = 0;
		for (DependencyStructure struct : depStructs) {
			final DependencyGraph graph = new DependencyGraph(struct.getNormalTokens());
			if (!graph.isProjective())
				nonproj++;
		}
		return nonproj;
	}

	public double percentNonprojTrees() {
		return 100.0 * nNonprojTrees() / nTrees();
	}

	public int nWords() {
		int nWords = 0;
		for (DependencyStructure struct : depStructs) {
			nWords += struct.getNormalTokens().length;
		}
		return nWords;
	}

	/**
	 * Write treebank to file in CoNLL-U form.
	 * @param filename Path of target file.
	 */
	public void write(final String filename) {
		ArrayList<String> lines = new ArrayList<>();
		for (DependencyStructure depStruct : depStructs) {
			lines.add(depStruct.toString());
		}
		try {
			Files.write(Paths.get(filename), lines, Charset.forName("UTF-8"));
		} catch (IOException e) {
			final Logger log = Logger.getLogger(getClass().getName());
			log.setParent(Logger.getGlobal());
			log.severe("Could not write treebank, error writing file: " + filename);
			System.exit(1);
		}
	}
}
