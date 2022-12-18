package standrews.constbase;

import javafx.util.Pair;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class TreebankIterator {
    private final List<String> sentenceIds;
    private final ConcurrentHashMap<String, SentenceEmbeddingsMetadata> sentenceIdEmbeddingMap;
    private int miniBatchesFetched;
    private int miniBatchesReturned;
    private final int miniBatchSize;
    private final BlockingQueue<Pair<List<String>, List<double[][]>>> miniBatchQueue;
    private final Random rng;

    public TreebankIterator(List<String> sentenceIds, HashMap<String, SentenceEmbeddingsMetadata> sentenceIdEmbeddingMap, int miniBatchSize, int queueSize, Random rng) {
        this.sentenceIds = sentenceIds;
        this.sentenceIdEmbeddingMap = new ConcurrentHashMap<>(sentenceIdEmbeddingMap);
        this.miniBatchSize = miniBatchSize;
        this.rng = rng;

        reset();

        miniBatchQueue = new ArrayBlockingQueue<>(queueSize+1);
        for (int i = 0; i < queueSize; i++) {
            if (canFetchNextBatch()) {
                fetchNextBatch();
            }
        }
    }

    public void reset() {
        miniBatchesFetched = 0;
        miniBatchesReturned = 0;
        Collections.shuffle(sentenceIds, rng);
    }

    private boolean hasNext() {
        return miniBatchesReturned * miniBatchSize < sentenceIds.size();
    }

    private boolean canFetchNextBatch() {
        return miniBatchesFetched * miniBatchSize < sentenceIds.size();
    }

    public Optional<Pair<List<String>, List<double[][]>>> next() {
        if (hasNext()) {
            if (canFetchNextBatch()) {
                fetchNextBatch();
            }

            try {
                Pair<List<String>, List<double[][]>> miniBatch = miniBatchQueue.take();
                miniBatchesReturned++;
                return Optional.of(miniBatch);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private void fetchNextBatch() {
        List<String> sentenceIdsMiniBatch = new ArrayList<>(sentenceIds.subList(miniBatchesFetched * miniBatchSize, Math.min((miniBatchesFetched + 1) * miniBatchSize, sentenceIds.size())));

        DataLoaderThread dataLoaderThread = new DataLoaderThread(miniBatchQueue, sentenceIdsMiniBatch, sentenceIdEmbeddingMap);
        dataLoaderThread.run();

        miniBatchesFetched++;
    }
}
