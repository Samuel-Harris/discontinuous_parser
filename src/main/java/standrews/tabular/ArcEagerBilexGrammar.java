/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.tabular;

import standrews.depmethods.ArcEagerParser;

import java.util.List;

public class ArcEagerBilexGrammar extends SplitBilexGrammar {

	// Labels, also with elements that may be top-of-stack.
	public static final String N = "nil";
	public static final String L = "leftChild";
	public static final String L1 = "leftChild-one-stackchild";
	public static final String R = "rightChild";
	public static final String R1 = "rightChild-one-stackchild";
	// public static final String Ltos = "leftChild-tos";
	// public static final String Rtos = "rightChild-tos";
	public static final String Lb = "leftChild-block-right";
	public static final String Nsb = "nil-shiftblock";
	public static final String Lrb = "leftChild-reduceblock";
	public static final String Rrb = "rightChild-reduceblock";

	public static String[] delexs = new String[] {N, L, R, L1, R1, Lb,
	Nsb, Lrb, Rrb};

	/**
	 *
	 */
	public ArcEagerBilexGrammar() {
		super(delexs);
		addRight(R, N, R);
		addRight(R, Nsb, R);
		addRight(R, R, R1);
		addRight(R, L, R1);
		addRight(Rrb, N, R);
		addRight(Rrb, R, R1);
		addRight(Rrb, L, R1);
		addRight(R1, N, R1);
		addRight(R1, Nsb, R1);
		addMiddle(R, R, R);
		addMiddle(R, R1, R);
		addMiddle(Rrb, R, R);
		addMiddle(Rrb, R1, R);
		addMiddle(Rrb, Rrb, Rrb);

		addRight(L, N, L);
		addRight(L, Nsb, L);
		addRight(L, R, L1);
		addRight(L, L, L1);
		addRight(Lrb, N, L);
		addRight(Lrb, R, L1);
		addRight(Lrb, L, L1);
		addRight(L1, N, L1);
		addRight(L1, Nsb, L1);
		addMiddle(L, L, L);
		addMiddle(L, L1, L);
		addMiddle(Lrb, L, L);
		addMiddle(Lrb, L1, L);
		addMiddle(Lrb, Lrb, Lrb);

		addRight(N, N, N);
		addRight(Nsb, N, Nsb);
		addLeft(N, N, N);
		addLeft(N, L, N);
		addLeft(Nsb, L, N);
		addLeft(N, Lrb, N);
		addLeft(N, Lb, N);

		addMiddle(N, N, N);
		addMiddle(N, Nsb, N);
		addMiddle(Nsb, Nsb, Nsb);

		addRight(Lb, N, Lb);
		addMiddle(Lb, Lb, Lb);
	}

	public static String[] getInput(final List<String> labels,
									final int suffixLen,
									final int reduceBlockMin,
									final int reduceBlockMax,
									final boolean shiftBlock,
									final boolean leftRightBlock) {
		final String[] in = new String[labels.size() + suffixLen];
		for (int i = 0; i < labels.size(); i++) {
			final String label = labels.get(i);
			if (label.equals(ArcEagerParser.leftChild))
				in[i] = reduceBlockMin <= i && i < reduceBlockMax ? Lrb :
						i == labels.size()-1 && leftRightBlock ? Lb : L;
			else
				in[i] = reduceBlockMin <= i && i < reduceBlockMax ? Rrb : R;
		}
		for (int i = labels.size(); i < labels.size() + suffixLen; i++) {
			in[i] = shiftBlock && i == labels.size() ? Nsb : N;
		}
		/*
		System.out.println("INPUT " + reduceBlockMin);
		for (int i = 0; i < in.length; i++) {
			System.out.print(" " + in[i]);
		}
		System.out.println();
		*/
		return in;
	}

}
