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
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.util.ArrayList;
import java.util.List;

public class MLPFactory {
    private List<Observation> observations;
    private int seed;
    private final int[] layers;
    private int batchSize;
    private int nEpochs;
    private double learningRate;
    private double tol;


    public MLPFactory(int inputSize, int batchSize, int nEpochs, int[] hiddenLayerSizes, double learningRate, double tol) {
        this.batchSize = batchSize;
        this.nEpochs = nEpochs;
        this.learningRate = learningRate;
        this.tol = tol;

        // setting network layer sizes
        layers = new int[hiddenLayerSizes.length + 2];
        layers[0] = inputSize;
        System.arraycopy(hiddenLayerSizes, 0, layers, 1, hiddenLayerSizes.length);

        observations = new ArrayList<>();
        seed = 123;
    }

    public MLP makeMLP(ResponseVectorGenerator responseVectorGenerator) {
        layers[layers.length-1] = responseVectorGenerator.getVectorSize();

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

        return new MLP(network, batchSize, nEpochs, responseVectorGenerator);
    }
}
