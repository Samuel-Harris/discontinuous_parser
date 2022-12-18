package standrews.constbase;

public class EnhancedConstLeaf extends ConstLeaf {
    private final double[] wordEmbedding;

    public EnhancedConstLeaf(ConstLeaf leaf, double[] wordEmbedding) {
        super(leaf.getIndex(), leaf.getForm(), leaf.getCat(), leaf.getLabel());
        this.wordEmbedding = wordEmbedding;
    }

    public EnhancedConstLeaf(EnhancedConstLeaf old) {
        super(old.getIndex(), old.getForm(), old.getCat(), old.getLabel());
        this.wordEmbedding = old.wordEmbedding;
    }

    public EnhancedConstLeaf(double[] wordEmbedding) {
        super(0, null, null, null);
        this.wordEmbedding = wordEmbedding;
    }

    public double[] getWordEmbedding() {
        return wordEmbedding;
    }
}
