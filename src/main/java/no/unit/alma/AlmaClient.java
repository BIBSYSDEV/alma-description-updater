package no.unit.alma;

import java.util.Optional;
import no.unit.http.AlmaConnectionFactory;
import no.unit.http.ReadUpdateConnection;
import no.unit.http.ReadUpdateConnectionFactory;
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
    private static final int MAX_RETRIES = 3;
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

    public AlmaClient(ReadUpdateConnectionFactory readUpdateConnectionFactory, Integer retryIntervalInSeconds) {
        this.connection = readUpdateConnectionFactory.create();
        this.retryIntervalInSeconds = Optional.ofNullable(retryIntervalInSeconds)
                                          .orElse(DEFAULT_RETRY_INTERVAL_IN_SECONDS);
    }

    /**
     * A method that sends a get request to ALMA.
     * @param mmsId The mms id needed to specify which post to retrieve.
     * @return A http response mirroring the response from the get request sent to ALMA.
     * @throws InterruptedException When something goes wrong.
     * @throws IOException When something goes wrong.
     */
    private HttpResponse<String> getBibRecordFromAlma(String mmsId) throws InterruptedException, IOException {
        return connection.sendGet(mmsId);
    }

    /**
     * A method that sends a put request to ALMA.
     * @param mmsId The mms id needed to specify which post to update.
     * @param updatedXml The string which we want to update the post with.
     * @return A http response mirroring the response from the put request sent to ALMA.
     * @throws InterruptedException When something goes wrong.
     * @throws IOException When something goes wrong.
     */
    private HttpResponse<String> putBibRecordInAlma(String mmsId, String updatedXml)
        throws InterruptedException, IOException {

        return connection.sendPut(mmsId, updatedXml);
    }

    /**
     * Method to retry GET-calls to ALMA, sleeps for 3 seconds before retrying.
     * @param mmsId For identifying the record in ALMA.
     * @return HttpResponse with the ALMA response or null if failing.
     */
    public HttpResponse<String> getBibRecordFromAlmaWithRetries(String mmsId) throws InterruptedException {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            addDelayOnNewAttempts(attempt);
            try {
                var almaResponse = getBibRecordFromAlma(mmsId);

                if (isSuccessful(almaResponse)) {
                    logger.info(RETRIEVED_BIB_RECORD, mmsId);
                    return almaResponse;
                } else  {
                    logger.error(ATTEMPT_FAILED_WITH_STATUS_CODE, attempt, almaResponse.statusCode());
                }
            } catch (InterruptedException | IOException e) {
                logger.error(ATTEMPT_FAILED, attempt, e.getMessage());
            }
        }

        return null;
    }

    /**
     * Method to retry Put-calls to ALMA, sleeps for 3 seconds before retrying.
     * @param mmsId For identifying the record in ALMA.
     * @return HttpResponse with the ALMA response or null if failing.
     * @throws InterruptedException when the sleep is interrupted.
     */
    public HttpResponse<String> putBibRecordInAlmaWithRetries(String mmsId, String updatedRecord)
        throws InterruptedException {

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            addDelayOnNewAttempts(attempt);
            try {
                var almaResponse = putBibRecordInAlma(mmsId, updatedRecord);

                if (isSuccessful(almaResponse)) {
                    logger.info(UPDATED_BIB_RECORD, mmsId);
                    return almaResponse;
                } else  {
                    logger.error(ATTEMPT_FAILED_WITH_STATUS_CODE, attempt, almaResponse.statusCode());
                }
            } catch (InterruptedException | IOException e) {
                logger.error(ATTEMPT_FAILED, attempt, e.getMessage());
            }
        }

        return null;
    }

    private void addDelayOnNewAttempts(int attempt) throws InterruptedException {
        if (attempt > 0) {
            TimeUnit.SECONDS.sleep(retryIntervalInSeconds);
        }
    }

    private boolean isSuccessful(HttpResponse<String> almaResponse) {
        return almaResponse != null && almaResponse.statusCode() == HttpStatusCode.OK;
    }

}
