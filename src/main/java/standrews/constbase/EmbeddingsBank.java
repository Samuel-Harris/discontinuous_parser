package standrews.constbase;

import com.codepoetics.protonpack.StreamUtils;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import standrews.classification.FeatureVectorGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

import static java.util.Arrays.compare;

public class EmbeddingsBank {
    private final HashMap<String, SentenceEmbeddingsMetadata> sentenceIdEmbeddingMap;
    private final double[] blankEmbeddingVector;

    public EmbeddingsBank(String bankName, String directory) {
        blankEmbeddingVector = new double[FeatureVectorGenerator.getEmbeddingSize()];
        Arrays.fill(blankEmbeddingVector, -1);

        sentenceIdEmbeddingMap = new HashMap<>();

        switch (bankName) {
            case "tiger":
                for (int i = 0; i < 505; i++) {
                    final int fileNumber = i;
                    try {
                        StreamUtils.zipWithIndex(Files.lines(Paths.get(directory + "metadata_" + i + ".txt")))
                                .forEach(zip -> {
                                    String[] sentenceIndexAndLength = zip.getValue().split(":", 2);
                                    sentenceIdEmbeddingMap.put(sentenceIndexAndLength[0], new SentenceEmbeddingsMetadata(directory, fileNumber, (int) zip.getIndex(), Integer.parseInt(sentenceIndexAndLength[1])));
                                });
//                        Files.lines(Paths.get(directory + "metadata_" + i + ".txt"))
//                                .forEach(line -> sentenceIdEmbeddingMap.put(line, new Pair<>(directory + "embeddings_" + index + ".npy")));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
//                    embeddings.add();
                }
                break;
            default:
                System.out.println("error, invalid treebank");
                System.exit(1);
        }
    }

    public SentenceEmbeddingsMetadata getSentenceEmbeddingsMetadata(String sentenceId) {
        return sentenceIdEmbeddingMap.get(sentenceId);
    }

    public double[][] getSentenceEmbeddings(String sentenceId) {
        SentenceEmbeddingsMetadata sentenceEmbeddingsMetaData = getSentenceEmbeddingsMetadata(sentenceId);
        INDArray matrix = Nd4j.readNpy(sentenceEmbeddingsMetaData.getFilePath())
                .get(NDArrayIndex.point(sentenceEmbeddingsMetaData.getlineNumber()),
                        NDArrayIndex.interval(0, sentenceEmbeddingsMetaData.getSentenceLength()),
                        NDArrayIndex.all());

        return matrix.toDoubleMatrix();
    }
}
