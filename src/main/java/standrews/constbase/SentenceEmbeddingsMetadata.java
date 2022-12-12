package standrews.constbase;

public class SentenceEmbeddingsMetadata {
    private final String filePath;
    private final int lineNumber;
    private final int sentenceLength;

    public SentenceEmbeddingsMetadata(String directory, int fileNumber, int lineNumber, int sentenceLength) {

        this.filePath = directory + "embeddings_" + fileNumber + ".npy";
        this.lineNumber = lineNumber;
        this.sentenceLength = sentenceLength;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getlineNumber() {
        return lineNumber;
    }

    public int getSentenceLength() {
        return sentenceLength;
    }
}
