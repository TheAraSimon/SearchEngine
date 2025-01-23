package searchengine.services.repositoryServices;

import org.springframework.http.ResponseEntity;

import java.util.Collection;
import java.util.Optional;

public interface CRUDService<T> {
    Object getByUrl(String url);

    void create(T item);

    void update(T item);

    void deleteByUrl(String url);
}
