package standrews.constbase;

import javafx.util.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class DataLoaderThread implements Runnable {
    private final BlockingQueue<Pair<List<String>, List<double[][]>>> miniBatchQueue;
    private final List<String> sentenceIds;  // sentence ids to load. NOT ALL SENTENCE IDS. Just the ones in the mini-batch
    private final ConcurrentHashMap<String, SentenceEmbeddingsMetadata> sentenceIdEmbeddingMap;

    public DataLoaderThread(BlockingQueue<Pair<List<String>, List<double[][]>>> miniBatchQueue,
                            List<String> sentenceIds,
                            ConcurrentHashMap<String, SentenceEmbeddingsMetadata> sentenceIdEmbeddingMap) {
        this.miniBatchQueue = miniBatchQueue;
        this.sentenceIds = sentenceIds;
        this.sentenceIdEmbeddingMap = sentenceIdEmbeddingMap;
    }

    @Override
    public void run() {
        List<double[][]> sentenceEmbeddingsMiniBatch = new ArrayList<>();
        for (String sentenceId: sentenceIds) {
            sentenceEmbeddingsMiniBatch.add(getSentenceEmbeddings(sentenceId));
        }

        miniBatchQueue.add(new Pair<>(sentenceIds, sentenceEmbeddingsMiniBatch));
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
