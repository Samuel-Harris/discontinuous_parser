/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.depbase;

import java.util.Arrays;
import java.util.List;

public class Accuracy {

	public static double unlabeledAttachmentSum(final DependencyStructure depStruct1,
												final DependencyStructure depStruct2) {
		List<Token> tokens1 = Arrays.asList(depStruct1.getNormalTokens());
		List<Token> tokens2 = Arrays.asList(depStruct2.getNormalTokens());
		if (tokens1.size() != tokens2.size()) {
			return 0;
		}
		int sum = 0;
		for (int i = 0; i < tokens1.size(); i++) {
			Token token1 = tokens1.get(i);
			Token token2 = tokens2.get(i);
			if (token1.head.equals(token2.head)) {
				sum++;
			}
		}
		return sum;
	}

	public static double labeledAttachmentSum(final DependencyStructure depStruct1,
											  final DependencyStructure depStruct2) {
		List<Token> tokens1 = Arrays.asList(depStruct1.getNormalTokens());
		List<Token> tokens2 = Arrays.asList(depStruct2.getNormalTokens());
		if (tokens1.size() != tokens2.size()) {
			return 0;
		}
		int sum = 0;
		for (int i = 0; i < tokens1.size(); i++) {
			Token token1 = tokens1.get(i);
			Token token2 = tokens2.get(i);
			if (token1.head.equals(token2.head) &&
					token1.deprel.equals(token2.deprel)) {
				sum++;
			}
		}
		return sum;
	}
}
