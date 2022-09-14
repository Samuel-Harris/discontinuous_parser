/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constbase;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

public class NegraTreebank extends ConstTreebank {

	public NegraTreebank(String id,
						 Set<String> poss, Set<String> cats, Set<String> labels,
						 ConstTree[] trees) {
		super(id, poss, cats, labels, trees);
	}

	public NegraTreebank(final String filename) {
		final List<ConstTree> sentences = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(
				// new FileInputStream(filename), "iso-8859-1"))) {
				new FileInputStream(filename), "utf-8"))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (isMarker(line, "BOS")) {
					sentences.add(readSentence(line, br));
				} else if (isMarker(line, "FORMAT")) {
					readFormat(line, br);
				} else if (isMarker(line, "BOT ORIGIN")) {
					readOrigin(line, br);
				} else if (isMarker(line, "BOT EDITOR")) {
					readEditor(line, br);
				} else if (isMarker(line, "BOT WORDTAG")) {
					readWordTag(line, br);
				} else if (isMarker(line, "BOT MORPHTAG")) {
					readMorphTag(line, br);
				} else if (isMarker(line, "BOT NODETAG")) {
					readNodeTag(line, br);
				} else if (isMarker(line, "BOT EDGETAG")) {
					readEdgeTag(line, br);
				} else if (isMarker(line, "BOT SECEDGETAG")) {
					readSecedgeTag(line, br);
				} else if (isComment(line)) {
					readComment(line);
				} else {
					final Logger log = logger();
					log.warning("Strange line: " + line);
				}
			}
		} catch (IOException e) {
			final Logger log = logger();
			log.severe("Could not read treebank, error reading file: " + filename + "\n" + e);
			System.exit(1);
		} catch (IllegalArgumentException e) {
			final Logger log = logger();
			log.severe("Could not read treebank, error reading file: " + filename + "\n" + e);
			System.exit(1);
		}
		this.trees = sentences.toArray(new ConstTree[0]);
	}

	private void readFormat(String line, BufferedReader br) throws IOException {
		// do nothing
	}

	private ConstTree readSentence(String line, BufferedReader br) throws IOException {
		String[] fields = line.split("\\s");
		if (fields.length < 2)
			throw new IOException("Strange BOS start: " + line);
		String treeId = fields[1];
		ConstTree tree = new ConstTree(treeId);
		while ((line = br.readLine()) != null) {
			if (isMarker(line, "EOS")) {
				return tree;
			} else if (isMarker(line, "")) {
				line = line.replace("\n", "")
						.replaceAll("^#", "")
						.replaceAll("\\s+", "\t");
				String[] interFields = line.split("\t");
				if (interFields.length < 5)
					throw new IOException("Strange sentence line: " + line);
				String id = interFields[0];
				String cat = interFields[1];
				String unk = interFields[2];
				String label = interFields[3];
				String parentId = interFields[4];
				ConstInternal node = tree.addInternal(id, cat, label);
				if (parentId.equals("0"))
					tree.addRoot(node);
				else
					tree.addParent(node, parentId);

			} else {
				line = line.replace("\n", "")
						.replaceAll("\\s+", "\t");
				String[] wordFields = line.split("\t");
				if (wordFields.length < 5)
					throw new IOException("Strange sentence line: " + line);
				String form = wordFields[0];
				String pos = wordFields[1];
				String unk = wordFields[2];
				String label = wordFields[3];
				String parentId = wordFields[4];
				ConstLeaf node = tree.addLeaf(form, pos, label);
				if (parentId.equals("0"))
					tree.addRoot(node);
				else
					tree.addParent(node, parentId);
			}
		}
		return tree;
	}

	private void readOrigin(String line, BufferedReader br) throws IOException {
		while ((line = br.readLine()) != null) {
			if (isMarker(line, "EOT ORIGIN"))
				return;
		}
	}

	private void readEditor(String line, BufferedReader br) throws IOException {
		while ((line = br.readLine()) != null) {
			if (isMarker(line, "EOT EDITOR"))
				return;
		}
	}

	private void readWordTag(String line, BufferedReader br) throws IOException {
		while ((line = br.readLine()) != null) {
			if (isMarker(line, "EOT WORDTAG"))
				return;
		}
	}

	private void readMorphTag(String line, BufferedReader br) throws IOException {
		while ((line = br.readLine()) != null) {
			if (isMarker(line, "EOT MORPHTAG"))
				return;
		}
	}

	private void readNodeTag(String line, BufferedReader br) throws IOException {
		while ((line = br.readLine()) != null) {
			if (isMarker(line, "EOT NODETAG"))
				return;
		}
	}

	private void readEdgeTag(String line, BufferedReader br) throws IOException {
		while ((line = br.readLine()) != null) {
			if (isMarker(line, "EOT EDGETAG"))
				return;
		}
	}

	private void readSecedgeTag(String line, BufferedReader br) throws IOException {
		while ((line = br.readLine()) != null) {
			if (isMarker(line, "EOT SECEDGETAG"))
				return;
		}
	}

	private void readComment(String line) {

	}

	private boolean isMarker(String line, String mark) {
		return line.startsWith("#" + mark);
	}

	private boolean isComment(String line) {
		return line.startsWith("%");
	}

	public void write(final String filename) {
		ArrayList<String> lines = new ArrayList<>();
		for (ConstTree tree : getTrees()) {
			lines.add(treeToString(tree));
		}
		try {
			Files.write(Paths.get(filename), lines, Charset.forName("UTF-8"));
		} catch (IOException e) {
			final Logger log = logger();
			log.severe("Could not write treebank, error writing file: " + filename);
			System.exit(1);
		}
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (ConstTree tree : getTrees()) {
			buf.append(treeToString(tree) + "\n");
		}
		return buf.toString();
	}

	public static String treeToString(ConstTree tree) {
		String[] leafParents = new String[tree.length()];
		TreeMap<String,String> internalParents = new TreeMap<>();
		getParents(tree, leafParents, internalParents);
		final StringBuilder sb = new StringBuilder();
		sb.append("#BOS " + tree.getId() + "\n");
		for (int i = 0; i < tree.length(); i++) {
			sb.append(leafToString(tree.getLeaf(i), leafParents));
		}
		for (String id : tree.getInternalIds()) {
			sb.append(internalToString(tree.getInternal(id), internalParents));
		}
		sb.append("#EOS " + tree.getId());
		return sb.toString();
	}

	private static String leafToString(ConstLeaf leaf, String[] parents) {
		return leaf.getForm() + "\t\t\t" +
				leaf.getCat() + "\t--\t\t" +
				leaf.getLabel() + "\t" +
				parents[leaf.getIndex()] + "\n";
	}

	private static String internalToString(ConstInternal node, TreeMap<String,String> parents) {
		String label = node.getLabel() == null ? "--" : node.getLabel();
		return "#" + node.getId() + "\t\t\t" +
				node.getCat() + "\t--\t\t" +
				label + "\t" +
				parents.get(node.getId()) + "\n";
	}

	private static void getParents(ConstTree tree,
							String[] leafParents,
							TreeMap<String,String> internalParents) {
		for (ConstNode node : tree.getRoots()) {
			if (node instanceof ConstLeaf) {
				ConstLeaf leaf = (ConstLeaf) node;
				leafParents[leaf.getIndex()] = "0";
			} else {
				ConstInternal internal = (ConstInternal) node;
				internalParents.put(internal.getId(), "0");
			}
		}
		for (String id : tree.getInternalIds()) {
			ConstInternal parent = tree.getInternal(id);
			for (ConstNode node : parent.getChildren()) {
				if (node instanceof ConstLeaf) {
					ConstLeaf leaf = (ConstLeaf) node;
					leafParents[leaf.getIndex()] = parent.getId();
				} else {
					ConstInternal internal = (ConstInternal) node;
					internalParents.put(internal.getId(), parent.getId());
				}
			}
		}
	}

	public ConstTreebank part(int from, int to) {
		ConstTree[] partTrees = Arrays.asList(trees).subList(from, to)
				.toArray(new ConstTree[0]);
		return new NegraTreebank(id, poss, cats, labels, partTrees);
	}

	private Logger logger() {
		final Logger log = Logger.getLogger(getClass().getName());
		log.setParent(Logger.getGlobal());
		return log;
	}

	public static void main(String[] args) {
		NegraTreebank bank = new NegraTreebank("/home/mjn/Data/Negra/negra-attach.export");
		bank.write("/home/mjn/work/billy/test.trash");
	}
}
