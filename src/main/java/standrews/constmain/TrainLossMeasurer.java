package standrews.constmain;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import standrews.classification.*;
import standrews.constbase.ConstTreebank;
import standrews.constbase.DatasetSplit;
import standrews.constextract.HatExtractor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TrainLossMeasurer {
    public static void main(String[] args) throws IOException {
        final String bankPath = "../datasets/tigercorpus2.1_original/corpus/tiger_negraformat.export";
        String actionFilePath = "tmp/actionClassifier";
        String catFilePath = "tmp/catClassifier";
        String fellowFilePath = "tmp/fellowClassifier";

        double trainRatio = 0.7;
        double validationRatio = 0.15;  // testRatio = 1 - trainRatio - validationRatio
        int seed = 123;
        Random rng = new Random(seed);
        int treebankIteratorQueueSize = 32;
        int networkMiniBatchSize = 512;
        int nEpochs = 1;

        ConstTreebank treebank = Experiments.tigerBank(bankPath, "right", rng, 505, trainRatio, validationRatio, treebankIteratorQueueSize);

        FeatureVectorGenerator featureVectorGenerator = new FeatureVectorGenerator(treebank);

        SimpleTrainer trainer = new SimpleTrainer(featureVectorGenerator, nEpochs, "",true);
        HatExtractor extractor;

        List<Double> actionTrainLossList = new ArrayList<>(nEpochs);
        List<Double> catTrainLossList = new ArrayList<>(nEpochs);
        List<Double> fellowTrainLossList = new ArrayList<>(nEpochs);

        for (int epoch = 0; epoch < nEpochs; epoch++) {
            extractor = new HatExtractor(
                    featureVectorGenerator,
                    networkMiniBatchSize,
                    1000,
                    1000,
                    actionFilePath + epoch,
                    catFilePath + epoch,
                    fellowFilePath + epoch);

            double[] losses = trainer.calculateAverageLossScores(treebank, extractor, DatasetSplit.TRAIN);
            actionTrainLossList.add(losses[0]);
            catTrainLossList.add(losses[1]);
            fellowTrainLossList.add(losses[2]);
        }

        trainer.writeLossListToFile("actionTrainLosses.csv", actionTrainLossList);
        trainer.writeLossListToFile("categoryTrainLosses.csv", catTrainLossList);
        trainer.writeLossListToFile("fellowTrainLosses.csv", fellowTrainLossList);
    }
}
