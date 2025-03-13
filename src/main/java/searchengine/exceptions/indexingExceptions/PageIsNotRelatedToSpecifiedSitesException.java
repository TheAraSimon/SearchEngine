package searchengine.exceptions.indexingExceptions;

public class PageIsNotRelatedToSpecifiedSitesException extends RuntimeException {
    public PageIsNotRelatedToSpecifiedSitesException() {
        super("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
    }
}
