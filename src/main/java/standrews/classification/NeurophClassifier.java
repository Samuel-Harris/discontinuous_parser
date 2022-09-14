/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.classification;

import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.core.events.LearningEvent;
import org.neuroph.core.events.LearningEventListener;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.BackPropagation;
import org.neuroph.nnet.learning.MomentumBackpropagation;
import org.neuroph.util.TransferFunctionType;
import org.tensorflow.op.math.Mul;
import standrews.aux.TimerMilli;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class NeurophClassifier extends Classifier {

    private int[] layers;
    private double learningRate;
    private int nIterations;

    private MultiLayerPerceptron network;

    public NeurophClassifier(String filename) {
        super(filename);
        setParameters();
    }

    public NeurophClassifier() {
        this(null);
    }

    private void setParameters() {
        layers = new int[]{200};
        learningRate = 0.1;
        nIterations = 5;
    }

    public void saveModel() {
        checkFilename();
        final File file = new File(filename);
        file.getParentFile().mkdirs();
        network.save(filename);
    }

    public void loadModel() {
        checkFilename();
        try {
            final File file = new File(filename);
            if (!file.exists())
                throw new IOException("file does not exist");
            network = (MultiLayerPerceptron) MultiLayerPerceptron.createFromFile(file);
        } catch (IOException e) {
            final Logger log = Logger.getLogger(getClass().getName());
            log.setParent(Logger.getGlobal());
            log.severe("Could not load model, error reading file: " +
                    filename + "\n" + e);
            System.exit(1);
        }
    }

    public void train() {
        nPredictors = nAtomicPredictors();
        nResponses = seenResponses.size();
        responseLabels = seenResponses.toArray(new String[nResponses]);
        network = makeNetwork(nPredictors, nResponses);

        final DataSet dataSet = new DataSet(nPredictors, nResponses);
        for (final Observation obs : observations) {
            final double[] inputData = predictorsDoubleArray(obs.features);
            final double[] outputData = responseDoubleArray(obs.response);
            dataSet.addRow(new DataSetRow(inputData, outputData));
        }

        // final DataSet dataSet = DataSet.createFromFile("iris_data_normalised.txt", 4, 3, ",");
        // System.out.println(dataSet);

        final TimerMilli timer = new TimerMilli();
        timer.start();
        network.learn(dataSet);
        timer.stop();
        reportFine("neuroph training took " + timer.seconds() + " s");
    }

    private MultiLayerPerceptron makeNetwork(final int nPredictors, final int nResponses) {
        final int[] layersAll = new int[layers.length + 2];
        layersAll[0] = nPredictors;
        System.arraycopy(layers, 0, layersAll, 1, layers.length);
        layersAll[layersAll.length-1] = nResponses;
        // final MultiLayerPerceptron network = new MultiLayerPerceptron(layersAll);
        // final TransferFunctionType type = TransferFunctionType.LINEAR;
        // final TransferFunctionType type = TransferFunctionType.TANH;
        final TransferFunctionType type = TransferFunctionType.SIGMOID;
        // final TransferFunctionType type = TransferFunctionType.GAUSSIAN;
        final MultiLayerPerceptron network = new MultiLayerPerceptron(type, nPredictors, 200, nResponses);
        // final MultiLayerPerceptron network = (MultiLayerPerceptron) new MultiLayerPerceptron(4, 16, 3);
        final MomentumBackpropagation backProp = (MomentumBackpropagation) network.getLearningRule();
        backProp.setLearningRate(learningRate);
        backProp.setMaxError(0.01);
        backProp.setMomentum(0.7);
        backProp.setMaxIterations(nIterations);
        // backProp.setBatchMode(true);
        backProp.addListener(new LearningListener());
        return network;
    }

    static class LearningListener implements LearningEventListener {
        @Override
        public void handleLearningEvent(LearningEvent event) {
            BackPropagation bp = (BackPropagation) event.getSource();
            System.out.println("Current iteration: " + bp.getCurrentIteration());
            System.out.println("Error: " + bp.getTotalNetworkError());
        }
    }

    public String predict(final Features feats) {
        return predictAll(feats)[0];
    }

    public String[] predictAll(final Features feats) {
        network.setInput(predictorsDoubleArray(feats));
        network.calculate();
        final double[] scores = network.getOutput();
        return IntStream.range(0, nResponses)
                .boxed()
                .sorted((x, y) -> Double.compare(scores[y], scores[x]))
                .map(x -> responseLabels[x])
                .toArray(String[]::new);
    }

}
