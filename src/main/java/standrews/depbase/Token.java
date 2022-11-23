/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.depbase;

/**
 * CoNLL-U token.
 * Source: <a href="http://universaldependencies.org/format.html" target="_blank">http://universaldependencies.org/format.html</a>
 */
public class Token implements Comparable<Token> {

    /**
     * Dummy token that can be used as root.
     */
    public final static Token ROOT = new Token();
    /**
     * CoNLL-U token ID field: word index, integer starting at 1 for each new sentence;
     * may be a range for multiword tokens; may be a decimal number for empty nodes.
     */
    public final Id id;
    /**
     * CoNLL-U token FORM field: word form or punctuation symbol.
     */
    public final Form form;
    /**
     * CoNLL-U token LEMMA field: lemma or stem of word form.
     */
    public final Lemma lemma;
    /**
     * CoNLL-U token UPOS field: universal part-of-speech tag.
     */
    public final Upos upos;
    /**
     * CoNLL-U token XPOS field: language-specific part-of-speech tag; underscore if not available.
     */
    public final Xpos xpos;
    /**
     * CoNLL-U token FEATS field: list of morphological features from the universal feature inventory
     * or from a defined language-specific extension; underscore if not available.
     */
    public final Feats feats;
    /**
     * CoNLL-U token HEAD field: head of the current word, which is either a value of ID or zero (0).
     */
    public final Id head;
    /**
     * CoNLL-U token DEPREL: universal dependency relation to the HEAD (root iff HEAD = 0) or
     * a defined language-specific subtype of one.
     */
    public final Deprel deprel;
    /**
     * CoNLL-U token DEPS field: enhanced dependency graph in the form of a list of head-deprel pairs.
     */
    public final Deps deps;
    /**
     * CoNLL-U token MISC field: any other annotation.
     */
    public final Misc misc;

    /**
     * Construct a token from given values.
     */
    private Token(final Id id, final Form form, final Lemma lemma, final Upos upos, final Xpos xpos,
                  final Feats feats, final Id head, final Deprel deprel,
                  final Deps deps, final Misc misc) {
        this.id = id;
        this.form = form;
        this.lemma = lemma;
        this.upos = upos;
        this.xpos = xpos;
        this.feats = feats;
        this.head = head;
        this.deprel = deprel;
        this.deps = deps;
        this.misc = misc;
    }

    /**
     * Construct a dummy token for the root.
     */
    private Token() {
        this(Id.ROOT, null, null, null, null,
                null, null, null, null, null);
    }

    /**
     * Parse a token from the columns that make it up.
     *
     * @param columns The columns that represent a token in CoNLL-U format.
     */
    public Token(final String[] columns) {
        this(new Id(columns[0]),
                new Form(columns[1]),
                new Lemma(columns[2]),
                new Upos(columns[3]),
                new Xpos(columns[4]),
                new Feats(columns[5]),
                new Id(columns[6]),
                new Deprel(columns[7]),
                new Deps(columns.length > 8 ? columns[8] : "_"),
                new Misc(columns.length > 9 ? columns[9] : "_"));
    }

    /**
     * Construct a copy, except that the head and deprel are different.
     * May be used to give token a parent, or to make token an orphan.
     *
     * @param head   The new HEAD value.
     * @param deprel The new DEPREL value.
     */
    private Token(final Token old, final Id head, final Deprel deprel) {
        this(old.id, old.form, old.lemma, old.upos, old.xpos, old.feats, head, deprel,
                old.deps, old.misc);
    }

    /**
     * Construct a copy, but replace the Upos.
     */
    private Token(final Token old, final Upos upos) {
        this(old.id, old.form, old.lemma, upos, old.xpos, old.feats, old.head, old.deprel,
                old.deps, old.misc);
    }

    /**
     * Copy the token, but remove parent.
     *
     * @return Altered token.
     */
    public Token getOrphaned() {
        return new Token(this, Id.ROOT, new Deprel("root"));
    }

    /**
     * Copy the token, but set parent and dependency relation.
     *
     * @param head   The new HEAD value.
     * @param deprel The new DEPREL value.
     * @return Altered token.
     */
    public Token getParented(final Id head, final Deprel deprel) {
        return new Token(this, head, deprel);
    }

    /**
     * Copy the token, but replace deprel by universal part only.
     *
     * @return Altered token.
     */
    public Token getUniDeprel() {
        return new Token(this, this.head, new Deprel(this.deprel.uniPart()));
    }

    public Token getUposRetagged(final String upos) {
        return new Token(this, new Upos(upos));
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof Token && id.equals(((Token) other).id);
    }

    @Override
    public int compareTo(final Token other) {
        return id.compareTo(other.id);
    }

    @Override
    public String toString() {
        if (equals(ROOT))
            return "ROOT";
        final StringBuilder sb = new StringBuilder();
        sb.append(id.toString());
        sb.append("\t");
        sb.append(form.toString());
        sb.append("\t");
        sb.append(lemma.toString());
        sb.append("\t");
        sb.append(upos.toString());
        sb.append("\t");
        sb.append(xpos.toString());
        sb.append("\t");
        sb.append(feats.toString());
        sb.append("\t");
        sb.append(head.toString());
        sb.append("\t");
        sb.append(deprel.toString());
        sb.append("\t");
        sb.append(deps.toString());
        sb.append("\t");
        sb.append(misc.toString());
        return sb.toString();
    }
}
