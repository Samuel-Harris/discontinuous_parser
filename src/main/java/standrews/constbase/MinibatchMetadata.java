package standrews.constbase;

import com.codepoetics.protonpack.StreamUtils;
import javafx.util.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MinibatchMetadata {
    private final String embeddingsFilePath;
    private List<SentenceMetadata> sentenceMetadataList;

    public MinibatchMetadata(String fileDirectory, int fileNumber) {
        embeddingsFilePath = fileDirectory + "embeddings_" + fileNumber + ".npy";

        try {
            try (Stream<String> lines = Files.lines(Paths.get(fileDirectory + "metadata_" + fileNumber + ".txt"))) {
                sentenceMetadataList = StreamUtils.zipWithIndex(lines)
                        .map(zip -> {
                            String[] sentenceIdAndLength = zip.getValue().split(":", 2);
                            return new SentenceMetadata(sentenceIdAndLength[0], Integer.parseInt(sentenceIdAndLength[1]), (int) zip.getIndex());
                        }).collect(Collectors.toCollection(ArrayList::new));
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public List<String> getSentenceIds() {
        return sentenceMetadataList.stream()
                .map(SentenceMetadata::getId)
                .collect(Collectors.toList());
    }

    public int getSize() {
        return sentenceMetadataList.size();
    }

    public List<Pair<String, double[][]>> loadEmbeddings() {
        try (INDArray embeddingsMatrix = Nd4j.readNpy(embeddingsFilePath)) {
            return sentenceMetadataList.stream()
                    .map(sentenceMetadata ->
                            new Pair<>(sentenceMetadata.getId(),
                                    embeddingsMatrix.get(NDArrayIndex.point(sentenceMetadata.getIndex()),
                                            NDArrayIndex.interval(0, sentenceMetadata.getLength()),
                                            NDArrayIndex.all()
                                    ).toDoubleMatrix()))
                    .collect(Collectors.toList());
        }
    }
}
