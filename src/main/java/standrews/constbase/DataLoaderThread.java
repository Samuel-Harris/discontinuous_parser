package standrews.constbase;

import javafx.util.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        List<Pair<Integer, double[][]>> miniBatchIndexEmbeddingsPairsList = new ArrayList<>();  // index-embedding pair

        HashMap<String, Pair<List<Integer>, List<SentenceEmbeddingsMetadata>>> filePathsMap = new HashMap<>();
        for (int index = 0; index < sentenceIds.size(); index++) {
            String sentenceId = sentenceIds.get(index);

            SentenceEmbeddingsMetadata sentenceEmbeddingsMetadata = sentenceIdEmbeddingMap.get(sentenceId);
            String filePath = sentenceEmbeddingsMetadata.getFilePath();
            if (filePathsMap.containsKey(filePath)) {
                filePathsMap.get(filePath).getKey().add(index);
                filePathsMap.get(filePath).getValue().add(sentenceEmbeddingsMetadata);
            } else {
                filePathsMap.put(filePath, new Pair<>( new ArrayList<>(List.of(index)), new ArrayList<>(List.of(sentenceEmbeddingsMetadata))));
            }
        }

        for (String filePath: filePathsMap.keySet()) {
            Pair<List<Integer>, List<SentenceEmbeddingsMetadata>> metaDataPairList = filePathsMap.get(filePath);

            List<Integer> indexList = metaDataPairList.getKey();
            List<double[][]> sentenceEmbeddingsList = getSentenceEmbeddingsList(filePath, metaDataPairList.getValue());

            miniBatchIndexEmbeddingsPairsList.addAll(IntStream.range(0, sentenceEmbeddingsList.size())
                    .mapToObj(i -> new Pair<>(indexList.get(i), sentenceEmbeddingsList.get(i)))
                    .collect(Collectors.toList()));
        }

        miniBatchQueue.add(new Pair<>(sentenceIds,
                miniBatchIndexEmbeddingsPairsList.stream()
                        .sorted(new IndexEmbeddingsComparator())
                        .map(Pair::getValue)
                        .collect(Collectors.toList())));
    }


    private static class IndexEmbeddingsComparator implements Comparator<Pair<Integer, double[][]>> {
        @Override
        public int compare(Pair<Integer, double[][]> a, Pair<Integer, double[][]> b) {
            return a.getKey() - b.getKey();
        }
    }

    public List<double[][]> getSentenceEmbeddingsList(String filePath, List<SentenceEmbeddingsMetadata> sentenceEmbeddingsMetadataList) {
        INDArray matrix = Nd4j.readNpy(filePath);

        return sentenceEmbeddingsMetadataList.stream()
                .map(sentenceEmbeddingsMetaData ->
                        matrix.get(NDArrayIndex.point(sentenceEmbeddingsMetaData.getlineNumber()),
                                NDArrayIndex.interval(0, sentenceEmbeddingsMetaData.getSentenceLength()),
                                NDArrayIndex.all()).toDoubleMatrix()).collect(Collectors.toList());
    }
}
