package standrews.classification;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.ILossFunction;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.util.ArrayList;
import java.util.List;

public class MLPFactory {
    private int seed;
    private final int[] layers;
    private double learningRate;


    public MLPFactory(int inputSize, int[] hiddenLayerSizes, double learningRate, int seed) {
        this.learningRate = learningRate;

        // setting network layer sizes
        layers = new int[hiddenLayerSizes.length + 2];
        layers[0] = inputSize;
        System.arraycopy(hiddenLayerSizes, 0, layers, 1, hiddenLayerSizes.length);

        this.seed = seed;
    }

    public MLP makeMLP(ResponseVectorGenerator responseVectorGenerator, double tol, int patience) {
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
                            .build());
        }
        MultiLayerConfiguration conf = builder.build();
        MultiLayerNetwork network = new MultiLayerNetwork(conf);
        network.init();

        return new MLP(network, responseVectorGenerator, tol, patience);
    }
}
