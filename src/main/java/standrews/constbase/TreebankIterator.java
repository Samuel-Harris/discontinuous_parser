package standrews.constbase;

import javafx.util.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class TreebankIterator {
    private final List<String> allSentenceIds;
    private final HashMap<String, SentenceEmbeddingsMetadata> sentenceIdEmbeddingMap;
    private int batchNumber = 0;
    private final int batchSize;

    public TreebankIterator(List<String> allSentenceIds, HashMap<String, SentenceEmbeddingsMetadata> sentenceIdEmbeddingMap, int batchSize) {
        this.allSentenceIds = allSentenceIds;
        this.sentenceIdEmbeddingMap = sentenceIdEmbeddingMap;
        this.batchSize = batchSize;
    }

    public void reset() {
        batchNumber = 0;
        Collections.shuffle(allSentenceIds);
    }

    public boolean hasNext() {
        return batchNumber*batchSize < allSentenceIds.size();
    }

    public Pair<List<String>, List<double[][]>> next() {
        List<String> batchSentenceIds = allSentenceIds.subList(batchNumber * batchSize, Math.min((batchNumber + 1) * batchSize, allSentenceIds.size()));


        List<double[][]> batchSentenceEmbeddings = new ArrayList<>();
        for (String sentenceId: batchSentenceIds) {
            batchSentenceEmbeddings.add(getSentenceEmbeddings(sentenceId));
        }

        batchNumber++;
        return new Pair<>(batchSentenceIds, batchSentenceEmbeddings);
    }

    public double[][] getSentenceEmbeddings(String sentenceId) {
        SentenceEmbeddingsMetadata sentenceEmbeddingsMetaData = sentenceIdEmbeddingMap.get(sentenceId);
        INDArray matrix = Nd4j.readNpy(sentenceEmbeddingsMetaData.getFilePath())
                .get(NDArrayIndex.point(sentenceEmbeddingsMetaData.getlineNumber()),
                        NDArrayIndex.interval(0, sentenceEmbeddingsMetaData.getSentenceLength()),
                        NDArrayIndex.all());

        return matrix.toDoubleMatrix();
    }
}
