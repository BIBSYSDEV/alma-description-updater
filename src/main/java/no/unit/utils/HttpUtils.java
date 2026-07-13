package no.unit.utils;

import java.net.http.HttpResponse;
import software.amazon.awssdk.http.HttpStatusCode;

public final class HttpUtils {

    private HttpUtils() {

    }

    public static boolean isSuccessful(HttpResponse<String> almaResponse) {
        return almaResponse != null && almaResponse.statusCode() == HttpStatusCode.OK;
    }

    public static boolean nonSuccessful(HttpResponse<String> httpResponse) {
        return httpResponse == null || httpResponse.statusCode() != HttpStatusCode.OK;
    }

}
