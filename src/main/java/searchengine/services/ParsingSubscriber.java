package searchengine.services;

import searchengine.dto.parsing.ParsingTaskResult;

public interface ParsingSubscriber {
    void update(ParsingTaskResult result);
}
