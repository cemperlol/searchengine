package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.repositories.LemmaRepository;

@Service
public class LemmaService {

    private final LemmaRepository lemmaRepository;

    @Autowired
    public LemmaService(LemmaRepository lemmaRepository) {
        this.lemmaRepository = lemmaRepository;
    }
}
