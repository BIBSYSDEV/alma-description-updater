package no.unit.http;

import java.io.IOException;
import java.net.http.HttpResponse;

public interface Connection {

    HttpResponse<String> sendGet(String mmsId) throws IOException, InterruptedException;

    HttpResponse<String> sendPut(String mmsId, String xml) throws IOException, InterruptedException;

}
