package standrews.classification;

import org.deeplearning4j.datasets.iterator.DoublesDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MLP {
    private final MultiLayerNetwork network;
    private final ArrayList<Pair<double[], double[]>> observations;
    private final ResponseVectorGenerator responseVectorGenerator;
    private boolean isTraining;
    private boolean isValidating;
    private int epochsWithoutImprovement;
    private double bestLossScore;
    private double lastLossScore;
    private double lastTrainLossScore;
    private final int miniBatchSize;
    private final double tol;
    private final int patience;

    public MLP(MultiLayerNetwork network, ResponseVectorGenerator responseVectorGenerator, int miniBatchSize, double tol, int patience) {
        this.network = network;
        this.responseVectorGenerator = responseVectorGenerator;
        this.miniBatchSize = miniBatchSize;
        this.tol = tol;
        this.patience = patience;

        observations = new ArrayList<>();
        isTraining = true;
        isValidating = false;
        epochsWithoutImprovement = 0;
        bestLossScore = Double.POSITIVE_INFINITY;
        lastLossScore = Double.POSITIVE_INFINITY;
    }

    public void addObservation(double[] featureVector, Object response) {
        if (isTraining || isValidating) {
            observations.add(new Pair<>(featureVector, responseVectorGenerator.generateResponseVector(response)));

            if (isTraining && observations.size() == miniBatchSize) {
                train();
            }
        }
    }

    public boolean isTraining() {
        return isTraining;
    }

    public void startValidating() {
        isValidating = true;
    }

    public void stopValidating() {
        isValidating = false;
    }

    public void applyEarlyStoppingIfApplicable(double trainLossScore, double validLossScore) {
        applyEarlyStoppingIfApplicable(validLossScore);
        this.lastTrainLossScore = trainLossScore;
    }

    public void applyEarlyStoppingIfApplicable(double lossScore) {
        if (!isTraining) {
            return;
        }

        lastLossScore = lossScore;

        if (lossScore < bestLossScore-tol) {
            bestLossScore = lossScore;
            epochsWithoutImprovement = 0;
        } else {
            epochsWithoutImprovement++;

            if (epochsWithoutImprovement > patience) {
                isTraining = false;
            }
        }
    }

    public int getEpochsWithoutImprovement() {
        return epochsWithoutImprovement;
    }

    public double getLastTrainLossScore() {
        return lastTrainLossScore;
    }

    public double getLastLossScore() {
        return lastLossScore;
    }

    public void train() {
        if (observations.isEmpty() || !isTraining) {
            return;
        }

        DataSetIterator iter = new DoublesDataSetIterator(observations, observations.size());

        network.fit(iter);

        observations.clear();

        System.gc();
    }

    public void save(String filePath) throws IOException {
        network.save(new File(filePath));
    }

    public double validateMiniBatch() {
        if (isTraining && !observations.isEmpty()) {
            DataSetIterator iter = new DoublesDataSetIterator(observations, observations.size());

            INDArray lossScores = network.scoreExamples(iter, false);

            double lossScoreSum = Arrays.stream(lossScores.toDoubleVector()).sum();

            observations.clear();

            return lossScoreSum;
        } else {
            return 0;
        }
    }

    public Object predict(double[] featureVector) {
        return predictAll(featureVector)[0];
    }

    public Object[] predictAll(double[] featureVector) {  // predicts action and orders them by most probable
        INDArray features = Nd4j.create(new double[][] {featureVector});
        INDArray output = network.output(features, false);
        final double[] scores = output.toDoubleVector();
        return responseVectorGenerator.getLabelsFromScores(scores);
//        return IntStream.range(0, scores.length)
//                .boxed()
//                .sorted((x, y) -> Double.compare(scores[y], scores[x]))
//                .map(x -> responseVectorGenerator.getResponseValue(x))
//                .toArray(Object[]::new);
    }
}
