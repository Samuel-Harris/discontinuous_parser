package standrews.classification;

import standrews.constmethods.HatParser;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class ActionResponseVectorGenerator implements ResponseVectorGenerator {
    private final Map<String, Integer> actionIndexMap;
    private final Map<Integer, String> indexActionMap;

    public ActionResponseVectorGenerator() {
        actionIndexMap = new HashMap<>();
        indexActionMap = new HashMap<>();
        for (int i = 0; i < HatParser.actionNames.length; i++) {
            actionIndexMap.put(HatParser.actionNames[i], i);
            indexActionMap.put(i, HatParser.actionNames[i]);
        }
    }

    @Override
    public double[] generateResponseVector(Object response) {
        double[] responseVector = new double[actionIndexMap.size()];
        responseVector[actionIndexMap.get((String) response)] = 1;

        return responseVector;
    }

    @Override
    public int getVectorSize() {
        return actionIndexMap.size();
    }

    @Override
    public Object[] getLabelsFromScores(double[] scores) {
        return IntStream.range(0, scores.length)
                .boxed()
                .sorted((x, y) -> Double.compare(scores[y], scores[x]))
                .map(indexActionMap::get)
                .toArray(String[]::new);
    }
}
