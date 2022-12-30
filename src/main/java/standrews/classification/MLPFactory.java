package standrews.classification;

import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.graph.MergeVertex;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.recurrent.Bidirectional;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class MLPFactory {
    private final int seed;
    private final int[] layers;
    private final int nAttentionHeads;
    private final double learningRate;
    private final double dropoutRate;
    private final int staticLayerInputSize;
    private final int staticLayerOutputSize;
    private final int stackLayerInputSize;
    private final int stackLayerOutputSize;
    private final int bufferLayerInputSize;
    private final int bufferLayerOutputSize;

    public MLPFactory(int staticLayerInputSize, int staticLayerOutputSize, int stackLayerInputSize, int stackLayerOutputSize, int bufferLayerInputSize, int bufferLayerOutputSize, int[] hiddenLayerSizes, int nAttentionHeads, double learningRate, double dropoutRate, int seed) {
        this.learningRate = learningRate;
        this.seed = seed;
        this.dropoutRate = dropoutRate;
        this.nAttentionHeads = nAttentionHeads;
        this.staticLayerInputSize = staticLayerInputSize;
        this.staticLayerOutputSize = staticLayerOutputSize;
        this.stackLayerInputSize = stackLayerInputSize;
        this.stackLayerOutputSize = stackLayerOutputSize;
        this.bufferLayerInputSize = bufferLayerInputSize;
        this.bufferLayerOutputSize = bufferLayerOutputSize;

        // setting network layer sizes
        layers = new int[hiddenLayerSizes.length + 2];
        layers[0] = staticLayerOutputSize + stackLayerOutputSize + bufferLayerOutputSize;
        System.arraycopy(hiddenLayerSizes, 0, layers, 1, hiddenLayerSizes.length);
    }

    public MLP makeMLP(ResponseVectorGenerator responseVectorGenerator, int miniBatchSize, double tol, int patience) {
        layers[layers.length - 1] = responseVectorGenerator.getVectorSize();

        ComputationGraphConfiguration.GraphBuilder graphBuilder = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .updater(new Adam(learningRate))
                .graphBuilder()
                .addInputs("hatInput", "stackInput", "bufferInput")
                .addLayer("hatInputLayer",
                        new DenseLayer.Builder()
                                .nIn(staticLayerInputSize)
                                .nOut(staticLayerOutputSize)
                                .weightInit(WeightInit.XAVIER)
                                .activation(Activation.RELU)
//                            .l2(l2Lambda)
                                .dropOut(dropoutRate)
                                .build(),
                        "hatInput")
                .addLayer("stackInputLayer",
                        new Bidirectional.Builder(Bidirectional.Mode.AVERAGE,
                                new LSTM.Builder()
                                        .nIn(stackLayerInputSize)
                                        .nOut(stackLayerOutputSize)
                                        .weightInit(WeightInit.XAVIER)
                                        .activation(Activation.TANH)
                                        .forgetGateBiasInit(1)
//                            .l2(l2Lambda)
                                        .dropOut(dropoutRate)
                                        .build())
                                .build(),
                        "stackInput")
                .addLayer("bufferInputLayer",
                        new Bidirectional.Builder(Bidirectional.Mode.AVERAGE,
                                new LSTM.Builder()
                                        .nIn(bufferLayerInputSize)
                                        .nOut(bufferLayerOutputSize)
                                        .weightInit(WeightInit.XAVIER)
                                        .activation(Activation.TANH)
                                        .forgetGateBiasInit(1)
//                            .l2(l2Lambda)
                                        .dropOut(dropoutRate)
                                        .build())
                                .build(),
                        "bufferInput")
                .addVertex("layer_0", new MergeVertex(), "hatInputLayer", "stackInputLayer", "bufferInputLayer");

        for (int i = 1; i < layers.length - 1; i++) {
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
        graphBuilder = graphBuilder.addLayer("layer_" + (layers.length-1),
                new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .nIn(layers[layers.length - 2])
                        .nOut(layers[layers.length - 1])
                        .weightInit(WeightInit.XAVIER)
                        .activation(Activation.SOFTMAX)
//                            .l2(l2Lambda)
                        .dropOut(dropoutRate)
                        .build(),
                "layer_" + (layers.length-2))
                .setOutputs("layer_" + (layers.length-2));

        ComputationGraphConfiguration conf = graphBuilder.build();
        ComputationGraph network = new ComputationGraph(conf);
        network.init();

//        NeuralNetConfiguration.ListBuilder builder = new NeuralNetConfiguration.Builder()
//                .seed(seed)
//                .updater(new Adam(learningRate))
//                .list();
//        builder = builder.layer(new SelfAttentionLayer.Builder()
//                .nIn(layers[0])
//                .nOut(layers[0])
//                .projectInput(true)
//                .headSize((int) Math.ceil((double) layers[0] / (double) nAttentionHeads))
//                .nHeads(nAttentionHeads)
//                .dropOut(dropoutRate)
//                .weightInit(WeightInit.XAVIER)
//                .build());
//        for (int n = 1; n < layers.length; n++) {
//            boolean last = n == layers.length - 1;
//            builder = builder.layer(n - 1,
//                    (last ? new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD) :
//                            new DenseLayer.Builder())
//                            .nIn(layers[n - 1])
//                            .nOut(layers[n])
//                            .weightInit(WeightInit.XAVIER)
//                            .activation(last ? Activation.SOFTMAX : Activation.RELU)
////                            .l2(l2Lambda)
//                            .dropOut(dropoutRate)
//                            .build());
//        }
//        MultiLayerConfiguration conf = builder.build();
//        MultiLayerNetwork network = new MultiLayerNetwork(conf);
//        network.init();


        return new MLP(network, responseVectorGenerator, miniBatchSize, tol, patience);
    }
}
