package searchengine.exceptions.searchingExceptions;

public class EmptySearchQueryException extends RuntimeException {
    public EmptySearchQueryException() {
        super("Задан пустой поисковый запрос");
    }
}
