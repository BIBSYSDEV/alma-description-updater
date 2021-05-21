package no.unit.alma;

import software.amazon.awssdk.http.HttpStatusCode;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

public class AlmaHelper {

    private static final String TAG_978 = "978";
    private static final int TAG_11 = 11;
    private static final int TAG_10 = 10;

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

    /**
     * Method for converting ISBN to 10 or 13.
     * @param isbn The isbn to be converted.
     * @return Returns an isbn in the opposite format.
     */
    public String convertIsbn(String isbn) {
        String convertedIsbn;
        if (isbn.length() > TAG_11) {
            convertedIsbn = convert13To10(isbn);
        } else {
            convertedIsbn = convert10To13(isbn);
        }
        return convertedIsbn;
    }

    /**
     * Converts isbn10 to isbn13.
     * @param isbn10 the isbn to be converted.
     * @return the isbn13.
     */
    public String convert10To13(String isbn10) {
        String isbn = TAG_978 + isbn10.substring(0, isbn10.length() - 1);
        int sum = 0;
        int mulitiplier;
        for (int i = 0; i < isbn.length(); i++) {
            mulitiplier = (i % 2 == 0) ? 1 : 3;
            sum += Character.getNumericValue(isbn.charAt(i)) * mulitiplier;
        }

        int moduloResult = sum % 10;
        int lastDigit = 0;
        if (moduloResult != 0) {
            lastDigit = 10 - moduloResult;
        }
        String isbn13 = isbn + lastDigit;
        return isbn13;
    }

    /**
     * Converts isbn13 to isbn10.
     * @param isbn13 the isbn to be converted.
     * @return the isbn10.
     */
    public String convert13To10(String isbn13) {
        String isbn = isbn13.substring(3, isbn13.length() - 1);
        int sum = 0;
        for (int i = 0; i < isbn.length(); i++) {
            sum += Character.getNumericValue(isbn.charAt(i)) * (10 - i);
        }

        int checksum = 11 - (sum % 11);
        String lastDigit;
        if (checksum == TAG_10) {
            lastDigit = "X";
        } else if (checksum == TAG_11) {
            lastDigit = "0";
        } else {
            lastDigit = String.valueOf(checksum);
        }
        String isbn10 = isbn + lastDigit;
        return isbn10;
    }
}
