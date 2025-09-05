package no.unit.http;

import java.io.IOException;
import java.net.http.HttpResponse;

public interface Connection {

    HttpResponse<String> sendGet(String id) throws IOException, InterruptedException;

    HttpResponse<String> sendPut(String id, String payload) throws IOException, InterruptedException;

}
