package searchengine.exceptions.searchingExceptions;

public class IncorrectSearchQueryException extends RuntimeException {
    public IncorrectSearchQueryException() {
        super("Задан некорректный поисковый запрос");
    }
}
