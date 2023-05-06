package searchengine.dto.statistics;

import lombok.*;

@Getter @Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class IndexingResult {

    private final int siteId;

    private final boolean indexingSucceed;

    private String errorMessage;
}
