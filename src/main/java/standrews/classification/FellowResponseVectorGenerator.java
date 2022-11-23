package standrews.classification;

import java.util.HashMap;
import java.util.Map;

public class FellowResponseVectorGenerator implements ResponseVectorGenerator {
    public static final int absMaxFellowIndex = 2;
    private final Map<Integer, Integer> fellowIndexToIndexMap;
    private final Map<Integer, Integer> indexToFellowIndexMap;

    public FellowResponseVectorGenerator() {
        fellowIndexToIndexMap = new HashMap<>();
        indexToFellowIndexMap = new HashMap<>();
        for (int i = 1; i < absMaxFellowIndex + 1; i++) {
            fellowIndexToIndexMap.put(-i, absMaxFellowIndex-i);
            fellowIndexToIndexMap.put(i, i+absMaxFellowIndex-1);

            indexToFellowIndexMap.put(absMaxFellowIndex-i, -i);
            indexToFellowIndexMap.put(i+absMaxFellowIndex-1, i);
        }
    }

    @Override
    public double[] generateResponseVector (Object response) {
        double[] responseVector = new double[2 * absMaxFellowIndex];
        responseVector[fellowIndexToIndexMap.get((int) response)] = 1;

        return responseVector;
    }

    @Override
    public int getVectorSize() {
        return 2 * absMaxFellowIndex;
    }

    @Override
    public Object getResponseValue(int index) {
        return indexToFellowIndexMap.get(index);
    }
}
