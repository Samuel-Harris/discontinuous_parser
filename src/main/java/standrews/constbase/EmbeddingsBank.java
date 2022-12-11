package standrews.constbase;

import com.codepoetics.protonpack.StreamUtils;
import javafx.util.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.tensorflow.ndarray.NdArray;
import standrews.classification.FeatureVectorGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

import static java.util.Arrays.compare;

public class EmbeddingsBank {
    private final HashMap<String, Pair<String, Integer>> sentenceIdEmbeddingMap;
    private final double[] blankEmbeddingVector;

    public EmbeddingsBank(String bankName, String directory) {
        blankEmbeddingVector = new double[FeatureVectorGenerator.getEmbeddingSize()];
        Arrays.fill(blankEmbeddingVector, -1);

        sentenceIdEmbeddingMap = new HashMap<>();

        switch (bankName) {
            case "tiger":
                for (int i = 0; i < 505; i++) {
                    final int index = i;
                    try {
                        StreamUtils.zipWithIndex(Files.lines(Paths.get(directory + "metadata_" + i + ".txt")))
                                .forEach(zip -> sentenceIdEmbeddingMap.put(zip.getValue(), new Pair<>(directory + "embeddings_" + index + ".npy", (int) zip.getIndex())));
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

    public double[][] getEmbeddings(String sentenceId) {
        Pair<String, Integer> sentenceLocation = sentenceIdEmbeddingMap.get(sentenceId);
        INDArray matrix = Nd4j.readNpy(sentenceLocation.getKey())
                .get(NDArrayIndex.point(sentenceLocation.getValue()), NDArrayIndex.all(), NDArrayIndex.all());

        for (int i = 0; i < matrix.rows(); i++) {
            if (compare(blankEmbeddingVector, matrix.get(NDArrayIndex.point(i), NDArrayIndex.all()).toDoubleVector()) == 0) {
                matrix = matrix.get(NDArrayIndex.interval(0, i));
                break;
            }
        }

        return matrix.toDoubleMatrix();
    }
}
