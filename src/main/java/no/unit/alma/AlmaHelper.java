package no.unit.alma;

import software.amazon.awssdk.http.HttpStatusCode;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

public class AlmaHelper {

    /**
     * A method that sends a get request to ALMA.
     * @param mmsId The mms id needed to specify which post to retrieve.
     * @return A http response mirroring the response from the get request sent to ALMA.
     * @throws InterruptedException When something goes wrong.
     * @throws IOException When something goes wrong.
     */
    private HttpResponse<String> getBibRecordFromAlma(String mmsId, String secretKey, String almaApiHost)
            throws InterruptedException, IOException {
        HttpResponse<String> almaResponse = AlmaConnection.getInstance().sendGet(mmsId, secretKey, almaApiHost);
        return almaResponse;
    }

    /**
     * A method that sends a put request to ALMA.
     * @param mmsId The mms id needed to specify which post to update.
     * @param updatedXml The string which we want to update the post with.
     * @return A http response mirroring the response from the put request sent to ALMA.
     * @throws InterruptedException When something goes wrong.
     * @throws IOException When something goes wrong.
     */
    private HttpResponse<String> putBibRecordInAlma(String mmsId, String updatedXml,
                                                    String secretKey, String almaApiHost)
            throws InterruptedException, IOException {
        HttpResponse<String> almaResponse = AlmaConnection.getInstance().sendPut(mmsId, secretKey,
                updatedXml, almaApiHost);
        return almaResponse;
    }

    /**
     * Method to retry GET-calls to ALMA, sleeps for 3 seconds before retrying.
     * @param mmsId For identifying the record in ALMA.
     * @return HttpResponse<String> with the ALMA response or null if failing.
     * @throws InterruptedException when the sleep is interrupted.
     */
    public HttpResponse<String> getBibRecordFromAlmaWithRetries(String mmsId, String secretKey, String almaApiHost)
            throws InterruptedException, IOException {
        HttpResponse<String> almaResponse;
        try {

            almaResponse = getBibRecordFromAlma(mmsId, secretKey, almaApiHost);
        } catch (InterruptedException | IOException e) {
            almaResponse = null; //NOPMD
            System.err.println(e.getMessage());
        }

        if (almaResponse != null && almaResponse.statusCode() == HttpStatusCode.OK) {
            return almaResponse;
        } else {

            TimeUnit.SECONDS.sleep(3);
            try {
                almaResponse = getBibRecordFromAlma(mmsId, secretKey, almaApiHost);
            } catch (InterruptedException | IOException e) {
                almaResponse = null; //NOPMD
            }
            if (almaResponse != null && almaResponse.statusCode() == HttpStatusCode.OK) {
                return almaResponse;
            } else {
                TimeUnit.SECONDS.sleep(3);
                almaResponse = getBibRecordFromAlma(mmsId, secretKey, almaApiHost);
                return almaResponse;
            }
        }
    }

    /**
     * Method to retry Put-calls to ALMA, sleeps for 3 seconds before retrying.
     * @param mmsId For identifying the record in ALMA.
     * @return HttpResponse<String> with the ALMA response or null if failing.
     * @throws InterruptedException when the sleep is interrupted.
     */
    public HttpResponse<String> putBibRecordInAlmaWithRetries(String mmsId, String updatedRecord,
                                                              String secretKey, String almaApiHost)
            throws InterruptedException {
        HttpResponse<String> response;
        try {
            response = putBibRecordInAlma(mmsId, updatedRecord, secretKey, almaApiHost);
        } catch (InterruptedException | IOException e) {
            response = null; //NOPMD
        }
        if (response != null && response.statusCode() == HttpStatusCode.OK) {
            return response;
        } else {
            TimeUnit.SECONDS.sleep(3);
            try {
                response = putBibRecordInAlma(mmsId, updatedRecord, secretKey, almaApiHost);
            } catch (InterruptedException | IOException e) {
                response = null; //NOPMD
            }
            if (response != null && response.statusCode() == HttpStatusCode.OK) {
                return response;
            } else {
                TimeUnit.SECONDS.sleep(3);
                try {
                    response = putBibRecordInAlma(mmsId, updatedRecord, secretKey, almaApiHost);
                } catch (InterruptedException | IOException e) {
                    response = null; //NOPMD
                }
                if (response != null && response.statusCode() == HttpStatusCode.OK) {
                    return response;
                } else {
                    return null;
                }
            }
        }
    }
}
