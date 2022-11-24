package standrews.classification;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class FellowResponseVectorGenerator implements ResponseVectorGenerator {
    public static final int absMaxFellowIndex = 2;
    private final Map<Integer, Integer> fellowIndexToIndexMap;
    private final Map<Integer, Integer> indexToFellowIndexMap;
    private final int vectorSize;

    public FellowResponseVectorGenerator() {
        fellowIndexToIndexMap = new HashMap<>();
        indexToFellowIndexMap = new HashMap<>();
        for (int i = 1; i < absMaxFellowIndex + 2; i++) {
            fellowIndexToIndexMap.put(-i, absMaxFellowIndex + 1- i);
            fellowIndexToIndexMap.put(i, i + absMaxFellowIndex);

            indexToFellowIndexMap.put(absMaxFellowIndex + 1- i, -i);
            indexToFellowIndexMap.put(i + absMaxFellowIndex, i);
        }

        vectorSize = 2*absMaxFellowIndex + 2;
    }

    @Override
    public double[] generateResponseVector(Object response) {
        double[] responseVector = new double[vectorSize];
        responseVector[fellowIndexToIndexMap.get((int) response)] = 1;

        return responseVector;
    }

    @Override
    public int getVectorSize() {
        return vectorSize;
    }

    @Override
    public Object[] getLabelsFromScores(double[] scores) {
        return IntStream.range(0, scores.length)
                .boxed()
                .sorted((x, y) -> Double.compare(scores[y], scores[x]))
                .map(indexToFellowIndexMap::get)
                .toArray(Integer[]::new);
    }
}
