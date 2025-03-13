package searchengine.exceptions.searchingExceptions;

public class EmptyIndexListException extends RuntimeException {
    public EmptyIndexListException() {
        super("Ни одна страница не была проиндексирована");
    }
}
