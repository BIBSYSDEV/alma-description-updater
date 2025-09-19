package no.unit.alma;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import no.unit.marc.Reference;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceListCreator {

    private static final Logger logger = LoggerFactory.getLogger(ReferenceListCreator.class);

    private static final String NO_ANSWER_FROM_SRU = "No answer from SRU for isbn: {}";

    private final AlmaProxyClient almaProxyClient;

    @JacocoGenerated
    public ReferenceListCreator() {
        this(new AlmaProxyClient());
    }

    public ReferenceListCreator(AlmaProxyClient almaProxyClient) {
        this.almaProxyClient = almaProxyClient;
    }

    public List<Reference> create(String isbn) throws IOException, InterruptedException {
        var referenceList = almaProxyClient.getReferenceListByIsbn(isbn);
        if (nullOrEmpty(referenceList)) {
            logNoAnswerFromSru(isbn);
            return Collections.emptyList();
        }
        return referenceList;
    }

    private boolean nullOrEmpty(List<Reference> referenceList) {
        return referenceList == null || referenceList.isEmpty();
    }

    private void logNoAnswerFromSru(String isbn) {
        logger.info(NO_ANSWER_FROM_SRU, isbn);
    }

}
