package standrews.classification;

//import javafx.util.Pair;
import org.deeplearning4j.datasets.iterator.DoublesDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;

import java.io.*;
import java.util.*;

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

    public MLP(MultiLayerNetwork network, ResponseVectorGenerator responseVectorGenerator, int miniBatchSize, double tol, int patience, File lossFile) throws IOException {
        this.network = network;
        this.responseVectorGenerator = responseVectorGenerator;
        this.miniBatchSize = miniBatchSize;
        this.tol = tol;
        this.patience = patience;

        observations = new ArrayList<>();

        double[] lossValues;
        try (BufferedReader br = new BufferedReader(new FileReader(lossFile))) {
            lossValues = Arrays.stream(br.readLine().split(",")).mapToDouble(Double::parseDouble).toArray();
        }

        bestLossScore = Double.POSITIVE_INFINITY;
        int minLossIndex = -1;
        for (int i = 0; i < lossValues.length; i++) {
            if (lossValues[i] < bestLossScore-tol) {
                bestLossScore = lossValues[i];
                minLossIndex = i;
            }
        }
        epochsWithoutImprovement = lossValues.length-1-minLossIndex;

        isTraining = epochsWithoutImprovement <= patience;
        isValidating = false;
        lastLossScore = lossValues[lossValues.length-1];
    }

    private void clearObservations() {
//        observations = new ArrayList<>();
        observations.clear();
//        System.gc();
    }

    public void addObservation(double[] featureVector, Object response) {
        if (isValidating && isTraining) {
            observations.add(new Pair<>(featureVector, responseVectorGenerator.generateResponseVector(response)));
        } else if (isTraining) {
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
        if (!isTraining) {
            clearObservations();
            return;
        }

        applyEarlyStoppingIfApplicable(validLossScore);
        this.lastTrainLossScore = trainLossScore;
    }

    public void applyEarlyStoppingIfApplicable(double lossScore) {
        if (!isTraining) {
            clearObservations();
            return;
        }

        lastLossScore = lossScore == 0 ? Double.POSITIVE_INFINITY : lossScore;

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
            clearObservations();
            return;
        }

        DataSetIterator iter = new DoublesDataSetIterator(observations, observations.size());

        network.fit(iter);

        clearObservations();
    }

    public void save(String filePath) throws IOException {
        network.save(new File(filePath));
    }

    public double validateMiniBatch() {
        if (isTraining && !observations.isEmpty()) {
            double lossScoreSum;

            DataSetIterator iter = new DoublesDataSetIterator(observations, observations.size());
            try (INDArray lossScores = network.scoreExamples(iter, false)) {
                lossScoreSum = Arrays.stream(lossScores.toDoubleVector()).sum();
            }

            clearObservations();

            return lossScoreSum;
        } else {
            clearObservations();
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
    }
}
