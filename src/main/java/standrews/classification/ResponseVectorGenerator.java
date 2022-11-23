package standrews.classification;

public interface ResponseVectorGenerator {
    double[] generateResponseVector(Object response);

    int getVectorSize();

    Object getResponseValue(int index);
}
