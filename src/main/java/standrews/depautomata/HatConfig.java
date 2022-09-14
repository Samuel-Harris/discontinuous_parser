/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.depautomata;

import standrews.depbase.Token;

public class HatConfig extends SimpleConfig {
	/**
	 * Index of hat in prefix.
	 */
	protected int hatIndex;

	/**
	 * Construct initial configuration for given sentence.
	 *
	 * @param tokens Tokenized sentence.
	 */
	public HatConfig(final Token[] tokens) {
		super(tokens);
		hatIndex = 0;
	}

	/**
	 * If there is label attached to stack elements.
	 */
	public HatConfig(final Token[] tokens, final String rootLabel) {
		super(tokens, rootLabel);
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
	public DependencyVertex getPrefixHat(final int i) {
		return getPrefixLeft(getHatAbsoluteIndex(i));
	}

	/**
	 * Remove element at index relative to hat.
	 */
	public DependencyVertex removePrefixHat(final int i) {
		return removePrefixLeft(getHatAbsoluteIndex(i));
	}

	@Override
	public String toString() {
		return "Hat at: " + hatIndex + "\n" + super.toString();
	}
}
