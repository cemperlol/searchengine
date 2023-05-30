package searchengine.dao;

public interface DBService {

    int getTotalCount();

    void deleteById(int id);

    void deleteAll();
}
