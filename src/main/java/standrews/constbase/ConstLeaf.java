/*
 * Copyright (c) 2019. University of St Andrews
 */

package standrews.constbase;

public class ConstLeaf extends ConstNode {

	private int index;

	private String form;

	private String cat;

	private String label;

	public ConstLeaf(int index, String form, String cat, String label) {
		this.index = index;
		this.form = form;
		this.cat = cat;
		this.label = label;
	}

	public ConstLeaf(final ConstLeaf old) {
		this(old.getIndex(), old.getForm(), old.getCat(), old.getLabel());
	}

	public boolean isTop() {
		return false;
	}

	public void setIndex(final int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public String getForm() {
		return form;
	}

	public void setCat(final String cat) {
		this.cat = cat;
	}

	public String getCat() {
		return cat;
	}

	public void setLabel(final String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public ConstLeaf[] getLeaves() {
		return new ConstLeaf[]{this};
	}

	public ConstLeaf getHeadLeaf() {
		return this;
	}

	public String toString() {
		return form + " " + cat + " " + label;
	}

	public String getIdentification() {
		return "#" + index;
	}
}
