package standrews.classification;

import org.nd4j.linalg.api.ndarray.INDArray;
import standrews.constautomata.HatConfig;
import standrews.constbase.ConstTreebank;
import standrews.constbase.EmbeddingsBank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FeatureVectorGenerator {
    private final EmbeddingsBank embeddingsBank;

    private final Map<String, Integer> catIndexMap;
    private final Map<String, Integer> posIndexMap;
    private final Map<String, Integer> catAndPosIndexMap;

    private final int hatSymbolFeatureIndex;  // index in feature vector that hat symbols start at
//    private int fellowSymbolFeatureIndex;  // index in feature vector that fellow symbols start at
//    private int fellowIndexFeatureIndex;  // index in feature vector that fellow indices start at
//    private final int topStackElementsIndex;  // index of top stack element embeddings

    private final int vectorSize;

    private final static int embeddingSize = 768;

    public FeatureVectorGenerator(final ConstTreebank treebank, final EmbeddingsBank embeddingsBank) {
        this.embeddingsBank = embeddingsBank;

        // making map to one-hot encode parts of speech
        List<String> poss = new ArrayList<>(treebank.getPoss());
        posIndexMap = new HashMap<>();
        catAndPosIndexMap = new HashMap<>();
        for (int i = 0; i < poss.size(); i++) {
            posIndexMap.put(poss.get(i), i);
            catAndPosIndexMap.put(poss.get(i), i);
        }

        // making map to one-hot encode categories
        List<String> categories = new ArrayList<>(treebank.getCats());
        catIndexMap = new HashMap<>();
        for (int i = 0; i < categories.size(); i++) {
            catIndexMap.put(categories.get(i), i);
            catAndPosIndexMap.put(categories.get(i), poss.size() + i);
        }

        hatSymbolFeatureIndex = 0;
//        topStackElementsIndex = catIndexMap.size() + 1;
//        fellowSymbolFeatureIndex = catAndPosIndexMap.size();
//        fellowIndexFeatureIndex = fellowSymbolFeatureIndex + catAndPosIndexMap.size();
        vectorSize = catAndPosIndexMap.size() + 1;
    }

    public double[] generateFeatureVector(HatConfig config) {
        double[] featureVector = new double[vectorSize];

        Optional<String> hatCat = config.getHatSymbol();

        if (hatCat.isPresent()) {
            featureVector[hatSymbolFeatureIndex + catAndPosIndexMap.get(hatCat.get())] = 1;  // add hat symbol to feature vector
        } else {
            featureVector[hatSymbolFeatureIndex + catAndPosIndexMap.size()] = 1;  // there is no hat
        }

//        double[][] sentenceEmbeddings = embeddingsBank.getEmbeddings("s" + config.getId());

        // to get fellow index cat:
//        final int abs = config.getHatAbsoluteIndex(fellowIndex);
//        String cat = config.getStackLeft(abs).getCat();

        return featureVector;
    }

    public int getVectorSize() {
        return vectorSize;
    }

    public Map<String, Integer> getCatIndexMap() {
        return catIndexMap;
    }

    public static int getEmbeddingSize() {
        return embeddingSize;
    }
}
