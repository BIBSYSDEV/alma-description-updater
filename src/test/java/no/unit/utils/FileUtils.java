package no.unit.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import no.unit.alma.DocumentXmlParserTest;

public class FileUtils {

    /**
     * A helper method that returns a string from a source.
     * @param file The file/source you want to retrieve the string from.
     * @return A string-value representing the content of the source.
     * @throws Exception when something goes wrong.
     */
    public static String setup(String file) throws Exception {
        var stream = DocumentXmlParserTest.class.getResourceAsStream(file);
        if (stream == null) {
            throw new RuntimeException("Cannot find resource " + file);
        }
        var reader = new InputStreamReader(stream);
        var br = new BufferedReader(reader);
        String line;
        var sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            sb.append(line.trim());
        }
        return sb.toString();
    }

}
