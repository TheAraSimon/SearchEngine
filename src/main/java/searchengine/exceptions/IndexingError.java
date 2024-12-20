package searchengine.exceptions;

public class IndexingError extends RuntimeException{
    public IndexingError() {
        super();
    }

    public IndexingError(String message) {
        super(message);
    }

}
