package standrews.classification;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class MLPFactory {
    private final int seed;
    private final int[] layers;
    private final double learningRate;
    private final double l2Lambda;
    private final double dropoutRate;

    public MLPFactory(int inputSize, int[] hiddenLayerSizes, double learningRate, double l2Lambda, double dropoutRate, int seed) {
        this.learningRate = learningRate;
        this.seed = seed;
        this.l2Lambda = l2Lambda;
        this.dropoutRate = dropoutRate;

        // setting network layer sizes
        layers = new int[hiddenLayerSizes.length + 2];
        layers[0] = inputSize;
        System.arraycopy(hiddenLayerSizes, 0, layers, 1, hiddenLayerSizes.length);
    }

    public MLP makeMLP(ResponseVectorGenerator responseVectorGenerator, int miniBatchSize, double tol, int patience) {
        layers[layers.length - 1] = responseVectorGenerator.getVectorSize();

        NeuralNetConfiguration.ListBuilder builder = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .updater(new Adam(learningRate))
                .list();
        for (int n = 1; n < layers.length; n++) {
            boolean last = n == layers.length - 1;
            builder = builder.layer(n - 1,
                    (last ? new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD) :
                            new DenseLayer.Builder())
                            .nIn(layers[n - 1])
                            .nOut(layers[n])
                            .weightInit(WeightInit.XAVIER)
                            .activation(last ? Activation.SOFTMAX : Activation.RELU)
                            .l2(l2Lambda)
                            .dropOut(dropoutRate)
                            .build());
        }
        MultiLayerConfiguration conf = builder.build();
        MultiLayerNetwork network = new MultiLayerNetwork(conf);
        network.init();

        return new MLP(network, responseVectorGenerator, miniBatchSize, tol, patience);
    }
}
