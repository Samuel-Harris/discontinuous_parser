package standrews.classification;

import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.conf.layers.recurrent.Bidirectional;
import org.deeplearning4j.nn.conf.preprocessor.RnnToFeedForwardPreProcessor;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class MLPFactory {
    private final int seed;
    private final int[] layers;
    private final double learningRate;
    private final double dropoutRate;
    private final int staticFeatureVectorSize;
    private final int stackLayerInputSize;
    private final int stackBiLSTMSize;
    private final int bufferLayerInputSize;
    private final int bufferBiLSTMSize;

    public MLPFactory(int staticFeatureVectorSize, int stackLayerInputSize, int stackBiLSTMSize,
                      int bufferLayerInputSize, int bufferBiLSTMSize, int[] hiddenLayerSizes,
                      double learningRate, double dropoutRate, int seed) {
        this.learningRate = learningRate;
        this.seed = seed;
        this.dropoutRate = dropoutRate;
        this.staticFeatureVectorSize = staticFeatureVectorSize;
        this.stackLayerInputSize = stackLayerInputSize;
        this.stackBiLSTMSize = stackBiLSTMSize;
        this.bufferLayerInputSize = bufferLayerInputSize;
        this.bufferBiLSTMSize = bufferBiLSTMSize;

        // setting network layer sizes
        layers = new int[hiddenLayerSizes.length];
        System.arraycopy(hiddenLayerSizes, 0, layers, 0, hiddenLayerSizes.length);
    }

    public MLP makeMLP(ResponseVectorGenerator responseVectorGenerator, int miniBatchSize, double tol, int patience) {
        layers[layers.length - 1] = responseVectorGenerator.getVectorSize();

        ComputationGraphConfiguration.GraphBuilder graphBuilder = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .updater(new Adam(learningRate))
                .graphBuilder()
                .addInputs("staticStateInput", "stackInput", "bufferInput")
                .setInputTypes(InputType.feedForward(staticFeatureVectorSize), InputType.recurrent(stackLayerInputSize), InputType.recurrent(bufferLayerInputSize))
                .addLayer("stackLSTMLayer",
                        new Bidirectional(Bidirectional.Mode.CONCAT,
                                new LSTM.Builder()
                                        .nIn(stackLayerInputSize)
                                        .nOut(stackBiLSTMSize)
                                        .weightInit(WeightInit.XAVIER)
                                        .activation(Activation.TANH)
                                        .forgetGateBiasInit(1)
//                            .l2(l2Lambda)
                                        .dropOut(dropoutRate)
                                        .gradientNormalization(GradientNormalization.ClipL2PerLayer)
                                        .gradientNormalizationThreshold(1.0)
                                        .biasInit(1.0)
                                        .build()),
                        "stackInput")
                .addLayer("poolStackLSTMLayer",
                        new GlobalPoolingLayer.Builder()
                                .poolingType(PoolingType.AVG)
                                .build()
                        , "stackLSTMLayer")
                .addLayer("bufferLSTMLayer",
                        new Bidirectional(Bidirectional.Mode.CONCAT,
                                new LSTM.Builder()
                                        .nIn(bufferLayerInputSize)
                                        .nOut(bufferBiLSTMSize)
                                        .weightInit(WeightInit.XAVIER)
                                        .activation(Activation.TANH)
//                            .l2(l2Lambda)
                                        .dropOut(dropoutRate)
                                        .gradientNormalization(GradientNormalization.ClipL2PerLayer)
                                        .gradientNormalizationThreshold(1.0)
                                        .biasInit(1.0)
                                        .build()),
                        "bufferInput")
                .addLayer("poolBufferLSTMLayer",
                        new GlobalPoolingLayer.Builder()
                                .poolingType(PoolingType.AVG)
                                .build()
                        , "bufferLSTMLayer")
                .addLayer("layer_0",
                        new DenseLayer.Builder()
                                .nIn(staticFeatureVectorSize + stackLayerInputSize + bufferLayerInputSize)
                                .nOut(layers[0])
                                .weightInit(WeightInit.XAVIER)
                                .activation(Activation.RELU)
//                            .l2(l2Lambda)
                                .dropOut(dropoutRate)
                                .build(),
                        "staticStateInput", "poolStackLSTMLayer", "poolBufferLSTMLayer");

        for (int i = 1; i < layers.length; i++) {
            graphBuilder = graphBuilder.addLayer("layer_" + i,
                    new DenseLayer.Builder()
                            .nIn(layers[i - 1])
                            .nOut(layers[i])
                            .weightInit(WeightInit.XAVIER)
                            .activation(Activation.RELU)
//                            .l2(l2Lambda)
                            .dropOut(dropoutRate)
                            .build(),
                    "layer_" + (i-1));
        }

        graphBuilder = graphBuilder.addLayer("layer_" + layers.length,
                new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .nIn(layers[layers.length - 1])
                        .nOut(responseVectorGenerator.getVectorSize())
                        .weightInit(WeightInit.XAVIER)
                        .activation(Activation.SOFTMAX)
//                            .l2(l2Lambda)
                        .dropOut(dropoutRate)
                        .build(),
                "layer_" + (layers.length-1))
                .setOutputs("layer_" + layers.length);

        ComputationGraphConfiguration conf = graphBuilder.build();
        ComputationGraph network = new ComputationGraph(conf);
        network.init();


        return new MLP(network, responseVectorGenerator, miniBatchSize, tol, patience);
    }
}
