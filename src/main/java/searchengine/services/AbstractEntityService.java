package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.AbstractEntity;
import searchengine.repositories.CommonEntityRepository;

public class AbstractEntityService<E extends AbstractEntity, R extends CommonEntityRepository<E>>
        implements CommonEntityService<E> {

    protected final R repository;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public AbstractEntityService(R repository) {
        this.repository = repository;
    }

    @Override
    public int getTotalCount() {
        return (int) repository.count();
    }

    @Override
    public void deleteById(int id) {
        repository.deleteById(id);
    }

    @Override
    public void deleteAll() {
        repository.deleteAllInBatch();
    }
}
