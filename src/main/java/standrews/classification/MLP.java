package standrews.classification;

import org.deeplearning4j.datasets.iterator.DoublesDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import standrews.aux_.TimerMilli;

import java.util.ArrayList;

public class MLP {
    private MultiLayerNetwork network;
    private boolean trainingStarted;
    private ArrayList<Pair<double[], double[]>> observations;
    private final int batchSize;
    private final int nEpochs;
    private ResponseVectorGenerator responseVectorGenerator;

    public MLP(MultiLayerNetwork network, int batchSize, int nEpochs, ResponseVectorGenerator responseVectorGenerator) {
        this.network = network;
        this.batchSize = batchSize;
        this.nEpochs = nEpochs;
        this.responseVectorGenerator = responseVectorGenerator;

        trainingStarted = false;
        observations = new ArrayList<>();
    }

    public void addObservation(double[] featureVector, Object response) {
        observations.add(new Pair<>(featureVector, responseVectorGenerator.generateResponseVector(response)));
    }

    public void train() {
        if (observations.isEmpty())
            return;

        DataSetIterator iter = new DoublesDataSetIterator(observations, observations.size());

        network.fit(iter);

//        final TimerMilli timer = new TimerMilli();
//        timer.start();
//        for (int n = 0; n < nEpochs; n++) {
//            network.fit(iter);
//        }
//        timer.stop();
//        System.out.println("Deeplearning training took " + timer.seconds() + " s");
        observations.clear();
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
