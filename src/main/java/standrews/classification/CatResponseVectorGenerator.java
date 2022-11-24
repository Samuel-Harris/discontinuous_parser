package standrews.classification;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

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
    public Object[] getLabelsFromScores(double[] scores) {
        return IntStream.range(0, scores.length)
                .boxed()
                .sorted((x, y) -> Double.compare(scores[y], scores[x]))
                .map(indexCatMap::get)
                .toArray(String[]::new);
    }
}
