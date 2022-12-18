package standrews.classification;

import standrews.constautomata.HatConfig;
import standrews.constbase.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FeatureVectorGenerator {
    private final Map<String, Integer> catIndexMap;
    private final Map<String, Integer> posIndexMap;
    private final Map<String, Integer> catAndPosIndexMap;
    private final static int embeddingVectorLength = 768;
    private final int vectorLength;
    private final int embeddingAndPosVectorLength;
    private final int posVectorLength;
    private final Double[] blankEmbeddingsAndPosVector;

    public FeatureVectorGenerator(final ConstTreebank treebank) {
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
        posVectorLength = posIndexMap.size();
        embeddingAndPosVectorLength = embeddingVectorLength + posVectorLength;

        blankEmbeddingsAndPosVector = new Double[embeddingAndPosVectorLength];
        for (int i = 0; i < embeddingAndPosVectorLength; i++) {
            blankEmbeddingsAndPosVector[i] = 0.0;
        }

        HatConfig testConfig = new HatConfig("", new ConstLeaf[0], new double[0][0]);
        vectorLength = generateFeatureVector(testConfig).length;
    }

    public double[] generateFeatureVector(HatConfig config) {
        List<Double> features = new ArrayList<>();

        // add one-hot encoded hat symbol to input features
        features.addAll(Arrays.asList(oneHotEncodeHatSymbol(config)));

        // add embeddings and parts of speech of leftmost and rightmost dependencies of top 2 elements of the stack
        if (config.stackLength() > 1) {
            ConstNode topOfStack = config.getStackRight(0);
            features.addAll(getLeftmostAndRightmostDependentEmbeddingsAndPos(topOfStack));
            if (config.stackLength() > 2) {
                ConstNode secondTopOfStack = config.getStackRight(1);
                features.addAll(getLeftmostAndRightmostDependentEmbeddingsAndPos(secondTopOfStack));
            } else {
                features.addAll(Arrays.asList(blankEmbeddingsAndPosVector.clone()));
                features.addAll(Arrays.asList(blankEmbeddingsAndPosVector.clone()));
            }
        } else {
            features.addAll(Arrays.asList(blankEmbeddingsAndPosVector.clone()));
            features.addAll(Arrays.asList(blankEmbeddingsAndPosVector.clone()));
            features.addAll(Arrays.asList(blankEmbeddingsAndPosVector.clone()));
            features.addAll(Arrays.asList(blankEmbeddingsAndPosVector.clone()));
        }

        // to get fellow index cat:
//        final int abs = config.getHatAbsoluteIndex(fellowIndex);
//        String cat = config.getStackLeft(abs).getCat();

        return features.stream().mapToDouble(x -> x).toArray();
    }

    private Double[] oneHotEncodeHatSymbol(HatConfig config) {
        int hatSymbolVectorLength = catAndPosIndexMap.size() + 1;

        // initialise hatSymbolVector
        Double[] hatSymbolVector = new Double[hatSymbolVectorLength];
        for (int i = 0; i < hatSymbolVectorLength; i++) {
            hatSymbolVector[i] = 0.0;
        }


        Optional<String> hatSymbol = config.getHatSymbol();

        if (hatSymbol.isPresent()) {
            hatSymbolVector[catAndPosIndexMap.get(hatSymbol.get())] = 1.0;  // add hat symbol to feature vector
        } else {
            hatSymbolVector[hatSymbolVectorLength-1] = 1.0;  // there is no hat
        }

        return hatSymbolVector;
    }

    private List<Double> getLeftmostAndRightmostDependentEmbeddingsAndPos(ConstNode node) {
        // getting leftmost and rightmost dependents
        EnhancedConstLeaf leftmostDependent = getLeftmostDependent(node);
        EnhancedConstLeaf rightmostDependent = getRightmostDependent(node);

        // getting leftmost and rightmost dependents' embeddings and one-hot encoded parts of speech
        Double[] leftmostDependentEmbeddingAndPosVector = getEmbeddingsAndPos(leftmostDependent);
        Double[] rightmostDependentEmbeddingAndPosVector = getEmbeddingsAndPos(rightmostDependent);

        return Stream.concat(Arrays.stream(leftmostDependentEmbeddingAndPosVector), Arrays.stream(rightmostDependentEmbeddingAndPosVector))
                .collect(Collectors.toList());
    }

    private Double[] getEmbeddingsAndPos(EnhancedConstLeaf node) {
        // initialising one-hot encoded parts of speech vectors
        double[] posVector = new double[posVectorLength];
        for (int i = 0; i < posVectorLength; i++) {
            posVector[i] = 0.0;
        }

        // getting parts of speech of leftmost and rightmost dependents
        posVector[posIndexMap.get(node.getCat())] = 1.0;

        // getting embeddings of leftmost and rightmost dependents
        double[] wordEmbeddingVector = node.getWordEmbedding();

        // putting parts of speech and embeddings into a single vector
        double[] embeddingAndPosVector = new double[embeddingAndPosVectorLength];
        System.arraycopy(posVector, 0, embeddingAndPosVector, 0, posVectorLength);
        System.arraycopy(wordEmbeddingVector, 0, embeddingAndPosVector, posVectorLength, embeddingVectorLength);

        return Arrays.stream(embeddingAndPosVector)
                .boxed()
                .toArray(Double[]::new);
    }

    private EnhancedConstLeaf getLeftmostDependent(ConstNode node) {
        if (node instanceof EnhancedConstLeaf) {
            return (EnhancedConstLeaf) node;
        } else if (node instanceof ConstInternal) {
            ConstInternal constituent = (ConstInternal) node;
            return getLeftmostDependent(constituent.getLeftMostChild());
        } else {
            System.out.println("Invalid ConstNode class");
            System.exit(1);
            return null;
        }
    }

    private EnhancedConstLeaf getRightmostDependent(ConstNode node) {
        if (node instanceof EnhancedConstLeaf) {
            return (EnhancedConstLeaf) node;
        } else if (node instanceof ConstInternal) {
            ConstInternal constituent = (ConstInternal) node;
            return getRightmostDependent(constituent.getRightMostChild());
        } else {
            System.out.println("Invalid ConstNode class");
            System.exit(1);
            return null;
        }
    }

    public int getVectorLength() {
        return vectorLength;
    }

    public Map<String, Integer> getCatIndexMap() {
        return catIndexMap;
    }

    public static int getEmbeddingVectorLength() {
        return embeddingVectorLength;
    }
}
