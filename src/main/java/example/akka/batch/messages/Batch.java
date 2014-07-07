package example.akka.batch.messages;

import java.util.List;

public class Batch<T> {

    private final List<T> batchedContent;

    public Batch(List<T> batchedContent) {
        this.batchedContent = batchedContent;
    }

    public List<T> getBatchedContent() {
        return batchedContent;
    }

}
