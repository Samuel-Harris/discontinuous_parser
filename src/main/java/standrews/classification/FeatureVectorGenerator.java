package standrews.classification;

import standrews.constbase.ConstTreebank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FeatureVectorGenerator {
    private Map<String, Integer> catIndexMap;
    private Map<String, Integer> posIndexMap;
    private Map<String, Integer> catAndPosIndexMap;

    private final int hatSymbolFeatureIndex;  // index in feature vector that hat symbols start at
    private int fellowSymbolFeatureIndex;  // index in feature vector that fellow symbols start at
    private int fellowIndexFeatureIndex;  // index in feature vector that fellow indices start at
    private final int vectorSize;

    public FeatureVectorGenerator(ConstTreebank treebank) {
        List<String> poss = new ArrayList<>(treebank.getPoss());
        posIndexMap = new HashMap<>();
        catAndPosIndexMap = new HashMap<>();
        for (int i = 0; i < poss.size(); i++) {
            posIndexMap.put(poss.get(i), i);
            catAndPosIndexMap.put(poss.get(i), i);
        }

        List<String> categories = new ArrayList<>(treebank.getCats());
        catIndexMap = new HashMap<>();
        for (int i = 0; i < categories.size(); i++) {
            catIndexMap.put(categories.get(i), i);
            catAndPosIndexMap.put(categories.get(i), poss.size() + i);
        }

        hatSymbolFeatureIndex = 0;
//        fellowSymbolFeatureIndex = catAndPosIndexMap.size();
//        fellowIndexFeatureIndex = fellowSymbolFeatureIndex + catAndPosIndexMap.size();
        vectorSize = catAndPosIndexMap.size() + 1;
    }

    public double[] generateFeatureVector(Optional<String> hatSymbol) {
        double[] featureVector = new double[vectorSize];

        ;
        if (hatSymbol.isPresent()) {
            featureVector[hatSymbolFeatureIndex + catAndPosIndexMap.get(hatSymbol.get())] = 1;  // add hat symbol to feature vector
        } else {
            featureVector[hatSymbolFeatureIndex + catAndPosIndexMap.size()] = 1;  // there is no hat
        }

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
}
