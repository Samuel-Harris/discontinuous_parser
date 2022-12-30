package standrews.classification;

public class FeatureVector {
    private final double[] staticFeatures;
    private final double[][] stackElementVectors;
    private final double[][] bufferElementVectors;

    public FeatureVector(double[] staticFeatures, double[][] stackElementVectors, double[][] bufferElementVectors) {
        this.staticFeatures = staticFeatures;
        this.stackElementVectors = stackElementVectors;
        this.bufferElementVectors = bufferElementVectors;
    }

    public double[] getStaticFeatures() {
        return staticFeatures;
    }

    public double[][] getStackElementVectors() {
        return stackElementVectors;
    }

    public double[][] getBufferElementVectors() {
        return bufferElementVectors;
    }
}
