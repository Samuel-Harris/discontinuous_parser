/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constbase.heads;

import standrews.constbase.ConstInternal;

public class RightHeadFinder extends HeadFinder {
	protected int getHeadIndex(ConstInternal node) {
		return node.getChildren().length-1;
	}
}
