package no.unit.utils;

import java.net.http.HttpResponse;

public final class HttpUtils {

    private HttpUtils() {

    }

    public static boolean isSuccessful(HttpResponse<String> almaResponse) {
        return almaResponse != null
               && almaResponse.statusCode() >= 200
               && almaResponse.statusCode() < 300;
    }

    public static boolean nonSuccessful(HttpResponse<String> httpResponse) {
        return !isSuccessful(httpResponse);
    }

}
