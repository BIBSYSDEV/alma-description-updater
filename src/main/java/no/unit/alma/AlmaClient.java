package no.unit.alma;

import java.util.Optional;
import no.unit.http.AlmaConnectionFactory;
import no.unit.http.ReadUpdateConnection;
import no.unit.http.ReadUpdateConnectionFactory;
import no.unit.http.HttpOperation;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.HttpStatusCode;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

public class AlmaClient {

    private static final Logger logger = LoggerFactory.getLogger(AlmaClient.class);

    private static final int DEFAULT_RETRY_INTERVAL_IN_SECONDS = 3;
    private static final int MAX_ATTEMPTS = 3;
    private static final int FIRST_ATTEMPT = 1;
    private static final String RETRIEVED_BIB_RECORD = "Successfully retrieved Bib record with mms_id {} from Alma";
    private static final String UPDATED_BIB_RECORD = "Successfully updated Bib record with mms_id {} in Alma";
    private static final String ATTEMPT_FAILED_WITH_STATUS_CODE = "Attempt {} failed with status code: {}";
    private static final String ATTEMPT_FAILED = "Attempt {} failed: {}";

    private final ReadUpdateConnection connection;
    private final Integer retryIntervalInSeconds;

    @JacocoGenerated
    public AlmaClient() {
        this(new AlmaConnectionFactory(), DEFAULT_RETRY_INTERVAL_IN_SECONDS);
    }

    public AlmaClient(ReadUpdateConnectionFactory connectionFactory, Integer retryIntervalInSeconds) {
        this.connection = connectionFactory.create();
        this.retryIntervalInSeconds = Optional.ofNullable(retryIntervalInSeconds)
                                          .orElse(DEFAULT_RETRY_INTERVAL_IN_SECONDS);
    }

    /**
     * Method to do GET-calls to ALMA with retries, sleeps for (n) seconds before retrying.
     * @param mmsId For identifying the record in ALMA.
     * @return HttpResponse with the ALMA response or null if failing.
     * @throws InterruptedException when the sleep is interrupted.
     */
    public HttpResponse<String> getBibRecordFromAlmaWithRetries(String mmsId) throws InterruptedException {
        return executeWithRetries(() -> connection.sendGet(mmsId), RETRIEVED_BIB_RECORD, mmsId);
    }

    /**
     * Method to do PUT-calls to ALMA with retries, sleeps for (n) seconds before retrying.
     * @param mmsId For identifying the record in ALMA.
     * @return HttpResponse with the ALMA response or null if failing.
     * @throws InterruptedException when the sleep is interrupted.
     */
    public HttpResponse<String> putBibRecordInAlmaWithRetries(String mmsId, String updatedRecord)
        throws InterruptedException {

        return executeWithRetries(() -> connection.sendPut(mmsId, updatedRecord), UPDATED_BIB_RECORD, mmsId);
    }

    private HttpResponse<String> executeWithRetries(HttpOperation almaOperation, String successLogMessage, String mmsId)
        throws InterruptedException {

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            addDelayOnNewAttempts(attempt);

            try {
                var response = almaOperation.execute();

                if (isSuccessful(response)) {
                    logger.info(successLogMessage, mmsId);
                    return response;
                } else {
                    logger.error(ATTEMPT_FAILED_WITH_STATUS_CODE, attempt, response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                logger.error(ATTEMPT_FAILED, attempt, e.getMessage());
            }
        }

        return null;
    }

    private void addDelayOnNewAttempts(int attempt) throws InterruptedException {
        if (attempt > FIRST_ATTEMPT) {
            TimeUnit.SECONDS.sleep(retryIntervalInSeconds);
        }
    }

    private boolean isSuccessful(HttpResponse<String> almaResponse) {
        return almaResponse != null && almaResponse.statusCode() == HttpStatusCode.OK;
    }

}
