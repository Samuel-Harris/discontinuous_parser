package standrews.constbase;

public class SentenceMetadata {
    private final String id;
    private final int length;
    private final int index;

    public SentenceMetadata(String id, int length, int index) {
        this.id = id;
        this.length = length;
        this.index = index;
    }

    public String getId() {
        return id;
    }

    public int getLength() {
        return length;
    }

    public int getIndex() {
        return index;
    }
}
