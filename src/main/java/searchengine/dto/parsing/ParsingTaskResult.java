package searchengine.dto.parsing;

import lombok.Data;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;

@Data
public class ParsingTaskResult {

    private Site site;

    private Page page;

    private List<Lemma> lemmas;

    private List<Index> indexes;
}
