package no.unit.alma;

import java.io.IOException;
import java.net.http.HttpResponse;

@FunctionalInterface
public interface AlmaOperation {

    HttpResponse<String> execute() throws IOException, InterruptedException;

}
