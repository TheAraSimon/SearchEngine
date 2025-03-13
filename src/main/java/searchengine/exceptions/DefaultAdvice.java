package searchengine.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.searching.SearchingResponse;
import searchengine.exceptions.indexingExceptions.*;
import searchengine.exceptions.searchingExceptions.EmptyIndexListException;
import searchengine.exceptions.searchingExceptions.EmptySearchQueryException;
import searchengine.exceptions.searchingExceptions.IncorrectSearchQueryException;

@ControllerAdvice
public class DefaultAdvice {

    @ExceptionHandler(PageIsNotRelatedToSpecifiedSitesException.class)
    public ResponseEntity<IndexingResponse> handleIndexingException(PageIsNotRelatedToSpecifiedSitesException e) {
        IndexingResponse response = new IndexingResponse();
        response.setError(e.getMessage());
        response.setResult(false);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IndexingNotStartedException.class)
    public ResponseEntity<IndexingResponse> handleIndexingException(IndexingNotStartedException e) {
        IndexingResponse response = new IndexingResponse();
        response.setError(e.getMessage());
        response.setResult(false);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IndexingRuntimeException.class)
    public ResponseEntity<IndexingResponse> handleIndexingException(IndexingRuntimeException e) {
        IndexingResponse response = new IndexingResponse();
        response.setError(e.getMessage());
        response.setResult(false);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IndexingInProcessException.class)
    public ResponseEntity<IndexingResponse> handleIndexingException(IndexingInProcessException e) {
        IndexingResponse response = new IndexingResponse();
        response.setError(e.getMessage());
        response.setResult(false);
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    @ExceptionHandler({EmptySitesListException.class, PageIsNotAvailableException.class})
    public ResponseEntity<IndexingResponse> handleIndexingException(Exception e) {
        IndexingResponse response = new IndexingResponse();
        response.setError(e.getMessage());
        response.setResult(false);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({EmptySearchQueryException.class, IncorrectSearchQueryException.class})
    public ResponseEntity<SearchingResponse> handleSearchingException(Exception e) {
        SearchingResponse response = new SearchingResponse();
        response.setError(e.getMessage());
        response.setResult(false);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EmptyIndexListException.class)
    public ResponseEntity<SearchingResponse> handleSearchingException(EmptyIndexListException e) {
        SearchingResponse response = new SearchingResponse();
        response.setError(e.getMessage());
        response.setResult(false);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
}
