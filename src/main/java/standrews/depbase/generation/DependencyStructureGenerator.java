/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.depbase.generation;

import standrews.depbase.*;

import java.util.Collections;
import java.util.Random;
import java.util.Vector;

/**
 * Random generation of projective dependency tree.
 */
public class DependencyStructureGenerator {

	/**
	 * Maximum length.
	 */
	private final int maxLength;
	/**
	 * Probability that one more child is to be produced.
	 */
	private final double childProb;

	/**
	 * Random generator.
	 */
	private Random random = new Random();

	public DependencyStructureGenerator(final int maxLength, final double childProb) {
		this.maxLength = maxLength;
		this.childProb = childProb;
	}

	/**
	 * Node in tree.
	 */
	private static class Node {
		public Vector<Node> leftNodes = new Vector<Node>();
		public Vector<Node> rightNodes = new Vector<Node>();
		public int size() {
			return 1 + leftSize() + rightSize();
		}
		public int leftSize() {
			int s = 0;
			for (Node child : leftNodes)
				s += child.size();
			return s;
		}
		public int rightSize() {
			int s = 0;
			for (Node child : rightNodes)
				s += child.size();
			return s;
		}
	}

	private class Rec {
		public int lengthRemain = maxLength;
	}

	private Node generate() {
		return generate(new Rec());
	}

	private Node generate(Rec rec) {
		Node node = new Node();
		rec.lengthRemain--;
		while (rec.lengthRemain > 0) {
			if (random.nextDouble() < childProb) {
				if (random.nextDouble() < 0.5)
					node.leftNodes.add(generate(rec));
				else
					node.rightNodes.add(generate(rec));
			} else
				break;
		}
		return node;
	}

	public Token[] generateDepStruct() {
		Vector<Token> tokens = new Vector<>();
		generate(tokens, generate(), 1, 0);
		return tokens.toArray(new Token[tokens.size()]);
	}

	private int generate(Vector<Token> tokens, Node node, int i, int parent) {
		int head = i + node.leftSize();
		for (Node child : node.leftNodes) {
			i = generate(tokens, child, i, head);
		}
		tokens.add(new Token(new String[] {"" + (i++),
				"form",	"lemma", "ADJ",	"_", "_", "" + parent,
				"acl", "_",	"_"}));
		for (Node child : node.rightNodes) {
			i = generate(tokens, child, i, head);
		}
		return i;
	}

	/**
	 * Generate dependency structure from 2-dimensional array.
	 * The first field should be in order.
	 * @param edges
	 * @return
	 */
	public Token[] generateDepStruct(int[][] edges) {
		Vector<Token> tokens = new Vector<>();
		for (int[] edge : edges)
			tokens.add(new Token(new String[] {"" + edge[0],
				"form",	"lemma", "ADJ",	"_", "_", "" + edge[1],
				"acl", "_",	"_"}));
		return tokens.toArray(new Token[tokens.size()]);
	}

	private static void printNode(Node node) {
		printNode(node, 0);
	}

	private static void printNode(Node node, int level) {
		for (Node child : node.leftNodes) {
			printNode(child, level + 1);
		}
		System.out.println(String.join("",
				Collections.nCopies(level * 3, " ")) + "*");
		for (Node child : node.rightNodes) {
			printNode(child, level + 1);
		}
	}

	/**
	 * For testing.
	 * @param args
	 */
	public static void main(String[] args) {
		DependencyStructureGenerator gen = new DependencyStructureGenerator(20, 0.45);
		for (int i = 0; i < 100; i++) {
			System.out.println("---");
			Token[] struct = gen.generateDepStruct();
			for (Token t : struct)
				System.out.println(t);
		}
	}
}
