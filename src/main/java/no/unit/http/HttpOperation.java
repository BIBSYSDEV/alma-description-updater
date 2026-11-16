package no.unit.http;

import java.io.IOException;
import java.net.http.HttpResponse;

@FunctionalInterface
public interface HttpOperation {

    HttpResponse<String> execute() throws IOException, InterruptedException;

}
