package no.unit.alma;

import com.fasterxml.jackson.databind.util.JSONPObject;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class AlmaConnectionTest {

    private static final String MMS_ID = "991325803064702201";

    @Test
    public void testAlmaConnectionSendGet() throws Exception{
        AlmaConnection almaCon = new AlmaConnection();
        SecretRetriever secretRetriever = new SecretRetriever();
        String secretKey = secretRetriever.getSecret();
        HttpResponse<String> result = almaCon.sendGet(MMS_ID, secretKey);
        System.out.println(result.body());
        assertEquals(200, result.statusCode());
    }
}