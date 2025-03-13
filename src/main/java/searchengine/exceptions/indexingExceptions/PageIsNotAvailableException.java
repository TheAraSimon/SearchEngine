package searchengine.exceptions.indexingExceptions;

public class PageIsNotAvailableException extends RuntimeException {
    public PageIsNotAvailableException() {
        super("Данная страница недоступна");
    }
}
