/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constautomata;

import standrews.constbase.ConstLeaf;
import standrews.constbase.ConstNode;

public class HatConfig extends SimpleConfig {
	/**
	 * Index of hat in prefix.
	 */
	protected int hatIndex;
	private String hatSymbol;

	/**
	 * Construct initial configuration for given sentence.
	 *
	 *
	 */
	public HatConfig(final String id, final ConstLeaf[] input) {
		super(id, input);
		hatIndex = 0;
	}

	/**
	 * Deep copy.
	 */
	protected HatConfig(final HatConfig config) {
		super(config);
		hatIndex = config.hatIndex;
	}

	/**
	 * Get index of hat, indexed from left to right.
	 *
	 * @return Index.
	 */
	public int getAbsoluteHatIndex() {
		return hatIndex;
	}

	/**
	 * Set index of hat, indexed from left to right.
	 *
	 * @param hatIndex Index of hat.
	 */
	public void setAbsoluteHatIndex(final int hatIndex) {
		this.hatIndex = hatIndex;
	}

	/**
	 * Set index of hat, given index relative to hat.
	 */
	public void setRelativeHatIndex(final int i) {
		this.hatIndex += i;
	}

	/**
	 * Decrement hat index.
	 */
	public void decrementHatIndex() {
		hatIndex--;
	}

	/**
	 * Turn absolute index into one relative to hat.
	 *
	 * @param i Absolute index.
	 * @return Relative index.
	 */
	public int getHatRelativeIndex(final int i) {
		return i - hatIndex;
	}

	/**
	 * Turn index relative to hat into absolute index.
	 *
	 * @param i Absolute index.
	 * @return Relative index.
	 */
	public int getHatAbsoluteIndex(final int i) {
		return i + hatIndex;
	}

	/**
	 * Get element at index relative to hat index.
	 *
	 * @param i Relative index.
	 * @return Element.
	 */
	public ConstNode getStackHat(final int i) {
		return getStackLeft(getHatAbsoluteIndex(i));
	}

	/**
	 * Remove element at index relative to hat.
	 */
	public ConstNode removeStackHat(final int i) {
		return removeStackLeft(getHatAbsoluteIndex(i));
	}

	@Override
	public String toString() {
		return "Hat at: " + hatIndex + "\n" + super.toString();
	}
}
