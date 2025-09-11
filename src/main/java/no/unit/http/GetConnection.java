package no.unit.http;

import java.io.IOException;
import java.net.http.HttpResponse;

@FunctionalInterface
public interface GetConnection {

    HttpResponse<String> sendGet(String id) throws IOException, InterruptedException;

}
