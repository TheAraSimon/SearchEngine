package searchengine.exceptions.indexingExceptions;

public class IndexingNotStartedException extends RuntimeException {
    public IndexingNotStartedException() {
        super("Индексация не запущена");
    }
}
