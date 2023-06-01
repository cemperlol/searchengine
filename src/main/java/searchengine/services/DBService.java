package searchengine.services;

public interface DBService {

    int getTotalCount();

    void deleteById(int id);

    void deleteAll();
}
