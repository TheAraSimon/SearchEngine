package searchengine.exceptions.indexingExceptions;

public class EmptySitesListException extends RuntimeException {
    public EmptySitesListException() {
        super("Список сайтов из конфигурационного файла пуст");
    }
}
