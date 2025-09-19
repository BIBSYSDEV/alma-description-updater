package no.unit.alma;

import static no.unit.utils.HttpUtils.nonSuccessful;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import no.unit.http.AlmaProxyConnectionFactory;
import no.unit.http.ReadConnection;
import no.unit.http.ReadConnectionFactory;
import no.unit.marc.Reference;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlmaProxyClient {

    private static final Logger logger = LoggerFactory.getLogger(AlmaProxyClient.class);

    private static final String NON_SUCCESSFUL_ANSWER =
        "Non successful answer from SRU for isbn: {} with status code {}";

    private final ReadConnection connection;

    @JacocoGenerated
    public AlmaProxyClient() {
        this(new AlmaProxyConnectionFactory());
    }

    public AlmaProxyClient(ReadConnectionFactory connectionFactory) {
        this.connection = connectionFactory.create();
    }

    /**
     * Retrieve a list of reference objects based on the isbn you enter.
     * @param isbn The isbn you wish to retrieve refrence objects based on.
     * @return A list of reference objects matching the isbn, this list will usually contain only one reference object.
     * @throws IOException when something goes wrong
     * @throws InterruptedException when something goes wrong
     */
    public List<Reference> getReferenceListByIsbn(String isbn) throws IOException, InterruptedException {
        var almaSruResponse = fetchFromAlmaSruProxy(isbn);
        if (nonSuccessful(almaSruResponse)) {
            logger.warn(NON_SUCCESSFUL_ANSWER, isbn, almaSruResponse.statusCode());
            return Collections.emptyList();
        }

        return createReferenceList(almaSruResponse.body());
    }

    // TODO: Remove
    private HttpResponse<String> fetchFromAlmaSruProxy(String isbn) throws IOException, InterruptedException {
        return connection.sendGet(isbn);
    }

    private List<Reference> createReferenceList(String response) {
        var gsonBuilder = new GsonBuilder();
        var gson = gsonBuilder.create();
        var listOfMyClassObject = new TypeToken<List<Reference>>() {}.getType();

        return gson.fromJson(response, listOfMyClassObject);
    }

}
