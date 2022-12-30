package standrews.classification;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.LearnedSelfAttentionLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.SelfAttentionLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class MLPFactory {
    private final int seed;
    private final int[] layers;
    private final int nAttentionHeads;
    private final int attentionHeadSize;
    private final double learningRate;
    private final double inputRetainProbability;

    public MLPFactory(int inputSize, int[] hiddenLayerSizes, int nAttentionHeads, int attentionHeadSize, double learningRate, double dropoutRate, int seed) {
        this.learningRate = learningRate;
        this.seed = seed;
        this.inputRetainProbability = 1.0-dropoutRate;
        this.nAttentionHeads = nAttentionHeads;
        this.attentionHeadSize = attentionHeadSize;

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
        if (nAttentionHeads > 0) {
            builder = builder.layer(0, new SelfAttentionLayer.Builder()
                    .nIn(layers[0])
                    .nOut(layers[0])
                    .projectInput(false)
                    .nHeads(1)
                    .headSize(attentionHeadSize)
                    .dropOut(inputRetainProbability)
                    .weightInit(WeightInit.XAVIER)
                    .build());

            builder = builder.layer(1,
                    new DenseLayer.Builder()
                            .nIn(layers[0])
                            .nOut(layers[1])
                            .weightInit(WeightInit.XAVIER)
                            .activation(Activation.RELU)
                            .dropOut(inputRetainProbability)
                            .build());

//            builder = builder.layer(1,
//                    new DenseLayer.Builder()
//                            .nIn(layers[0])
//                            .nOut(layers[1])
//                            .weightInit(WeightInit.XAVIER)
//                            .activation(Activation.RELU)
//                            .dropOut(inputRetainProbability)
//                            .build());

//            builder = builder.layer(0, new SelfAttentionLayer.Builder()
//                    .nIn(layers[1])
//                    .nOut(layers[1])
//                    .projectInput(true)
//                    .nHeads(nAttentionHeads)
//                    .headSize(attentionHeadSize)
//                    .dropOut(inputRetainProbability)
//                    .weightInit(WeightInit.XAVIER)
//                    .build());
        }

        for (int i = (nAttentionHeads > 0 ? 3 : 1); i < layers.length; i++) {
            boolean last = i == layers.length - 1;
            builder = builder.layer(i-1,
                    (last ? new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD): new DenseLayer.Builder())
                            .nIn(layers[i - 1])
                            .nOut(layers[i])
                            .weightInit(WeightInit.XAVIER)
                            .activation(last ? Activation.SOFTMAX : Activation.RELU)
//                            .l2(l2Lambda)
                            .dropOut(inputRetainProbability)
                            .build());
        }
        MultiLayerConfiguration conf = builder.build();
        MultiLayerNetwork network = new MultiLayerNetwork(conf);
        network.init();

        return new MLP(network, responseVectorGenerator, miniBatchSize, tol, patience);
    }
}
