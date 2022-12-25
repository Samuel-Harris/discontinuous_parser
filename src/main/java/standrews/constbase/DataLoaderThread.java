package standrews.constbase;

import javafx.util.Pair;
import java.util.*;
import java.util.concurrent.BlockingQueue;

public class DataLoaderThread implements Runnable {
    private final BlockingQueue<List<Pair<String, double[][]>>> miniBatchQueue;
    private final MinibatchMetadata minibatchMetadata;

    public DataLoaderThread(BlockingQueue<List<Pair<String, double[][]>>> miniBatchQueue,
                            MinibatchMetadata minibatchMetadata) {
        this.miniBatchQueue = miniBatchQueue;
        this.minibatchMetadata = minibatchMetadata;

    }

    @Override
    public void run() {
        miniBatchQueue.add(minibatchMetadata.loadEmbeddings());
    }
}
