package standrews.classification;

import java.util.HashMap;
import java.util.Map;

public class CatResponseVectorGenerator implements ResponseVectorGenerator {
    private final Map<String, Integer> catIndexMap;
    private final Map<Integer, String> indexCatMap;

    public CatResponseVectorGenerator(Map<String, Integer> catIndexMap) {
        this.catIndexMap = catIndexMap;
        indexCatMap = new HashMap<>();

        for (Map.Entry<String, Integer> entry : catIndexMap.entrySet()) {
            indexCatMap.put(entry.getValue(), entry.getKey());
        }
    }

    @Override
    public double[] generateResponseVector(Object response) {
        double[] responseVector = new double[catIndexMap.size()];
        responseVector[catIndexMap.get((String) response)] = 1;

        return responseVector;
    }

    @Override
    public int getVectorSize() {
        return catIndexMap.size();
    }

    @Override
    public Object getResponseValue(int index) {
        return indexCatMap.get(index);
    }
}
