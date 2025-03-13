package searchengine.exceptions.indexingExceptions;

public class IndexingRuntimeException extends RuntimeException {
    public IndexingRuntimeException() {
        super("Ошибка на стороне сервера");
    }
}
