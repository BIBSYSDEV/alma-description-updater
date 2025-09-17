package no.unit.http;

import java.io.IOException;
import java.net.http.HttpResponse;

@FunctionalInterface
public interface UpdateConnection {

    HttpResponse<String> sendPut(String id, String payload) throws IOException, InterruptedException;

}
