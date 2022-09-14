/*
 * Copyright (c) 2018. University of St Andrews
 */

package standrews.tabular;

import standrews.depmethods.NormalArcEagerParser;

import java.util.List;

public class NormalArcEagerBilexGrammar extends SplitBilexGrammar {

	// Labels, also with elements that may be top-of-stack.
	public static final String N = "nil";
	public static final String L = "leftChild";
	public static final String R = "rightChild";
	public static final String Ltos = "leftChild-tos";
	public static final String Rtos = "rightChild-tos";
	public static final String Lb = "leftChild-block-right";

	public static String[] delexs = new String[] {N, L, R, Ltos, Rtos, Lb};

	/**
	 *
	 */
	public NormalArcEagerBilexGrammar() {
		super(delexs);
		addLeft(N, L, N);
		addLeft(N, Ltos, N);
		addLeft(N, Lb, N);
		addLeft(N, N, N);
		addRight(N, N, N);
		addMiddle(N, N, N);

		addRight(L, N, Ltos);
		addRight(L, Ltos, Ltos);
		addRight(L, Rtos, Ltos);
		addRight(Ltos, N, Ltos);
		addRight(R, N, Rtos);
		addRight(R, Ltos, Rtos);
		addRight(R, Rtos, Rtos);
		addRight(Rtos, N, Rtos);

		addMiddle(L, L, L);
		addMiddle(L, Ltos, Ltos);
		addMiddle(Ltos, Ltos, Ltos);
		addMiddle(R, R, R);
		addMiddle(R, Rtos, Rtos);
		addMiddle(Rtos, Rtos, Rtos);

		addRight(Lb, N, Lb);
		addMiddle(Lb, Lb, Lb);
	}

	public static String[] getInput(final List<String> labels, final int suffixLen, final boolean leftRightBlock) {
		final String[] in = new String[labels.size() + suffixLen];
		for (int i = 0; i < labels.size(); i++) {
			final String label = labels.get(i);
			if (label.equals(NormalArcEagerParser.leftChild))
				in[i] = i == labels.size()-1 ? leftRightBlock ? Lb : Ltos : L;
			else if (label.equals(NormalArcEagerParser.rightChild))
				in[i] = i == labels.size()-1 ? Rtos : R;
			else
				in[i] = N;
		}
		for (int i = labels.size(); i < labels.size() + suffixLen; i++) {
			in[i] = N;
		}
		return in;
	}

}
