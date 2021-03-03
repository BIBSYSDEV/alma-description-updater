package no.unit.alma;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class GetRecordByIsbnConnection {

    public InputStreamReader connect(URL url) throws IOException {
        return new InputStreamReader(url.openStream());
    }

}
