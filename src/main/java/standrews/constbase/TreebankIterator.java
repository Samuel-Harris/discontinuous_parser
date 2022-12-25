package standrews.constbase;

import javafx.util.Pair;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TreebankIterator {
    private final List<MinibatchMetadata> miniBatchMetadataList;
    private int miniBatchesFetched;
    private int miniBatchesReturned;
    private final BlockingQueue<List<Pair<String, double[][]>>> miniBatchQueue;
    private final Random rng;

    public TreebankIterator(List<MinibatchMetadata> miniBatchMetadataList, int queueSize, Random rng) {
        this.miniBatchMetadataList = miniBatchMetadataList;
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
        Collections.shuffle(miniBatchMetadataList, rng);
    }

    private boolean hasNext() {
        return miniBatchesReturned < miniBatchMetadataList.size();
    }

    private boolean canFetchNextBatch() {
        return miniBatchesFetched < miniBatchMetadataList.size();
    }

    public Optional<List<Pair<String, double[][]>>> next() {
        if (hasNext()) {
            if (canFetchNextBatch()) {
                fetchNextBatch();
            }

            try {
                List<Pair<String, double[][]>> miniBatch = miniBatchQueue.take();
                miniBatchesReturned++;
                Collections.shuffle(miniBatch, rng);
                return Optional.of(miniBatch);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private void fetchNextBatch() {
        DataLoaderThread dataLoaderThread = new DataLoaderThread(miniBatchQueue, miniBatchMetadataList.get(miniBatchesFetched));
        dataLoaderThread.run();

        miniBatchesFetched++;
    }
}
