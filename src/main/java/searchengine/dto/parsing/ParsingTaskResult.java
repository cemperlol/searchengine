package searchengine.dto.parsing;

import lombok.Data;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Map;

@Data
public class ParsingTaskResult {

    private Site site;

    private Page page;

    private Map<String, Integer> lemmasAndFrequency;
}
