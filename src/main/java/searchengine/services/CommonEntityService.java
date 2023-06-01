package searchengine.services;

import searchengine.model.AbstractEntity;

public interface CommonEntityService<E extends AbstractEntity> {

    int getTotalCount();

    void deleteById(int id);

    void deleteAll();
}
