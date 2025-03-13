package searchengine.exceptions.indexingExceptions;

public class IndexingInProcessException extends RuntimeException {
    public IndexingInProcessException() {
        super("Индексация уже в процессе");
    }
}
