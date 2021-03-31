package no.unit.secret;

import com.google.gson.Gson;
import no.unit.exceptions.SecretRetrieverException;
import no.unit.secret.SecretFormat;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.DecryptionFailureException;
import software.amazon.awssdk.services.secretsmanager.model.InternalServiceErrorException;
import software.amazon.awssdk.services.secretsmanager.model.InvalidParameterException;
import software.amazon.awssdk.services.secretsmanager.model.InvalidRequestException;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

import java.util.Base64;


public class SecretRetriever {


    private static final String SECRET_ERROR_MESSAGE =
            "Error while retrieving secret from AWS.";

    /**
     * This method gives you access to the alma api key,
     * it assumes you have the correct credentials.
     * @return String The api key used to access the Alma api.
     * @throws SecretRetrieverException when something goes wrong.
     */
    public static String getAlmaApiKeySecret() throws SecretRetrieverException {
        final String secretName = "ALMA_APIKEY";
        Region region = Region.EU_WEST_1;

        // Create a Secrets Manager client
        SecretsManagerClient client = SecretsManagerClient.builder()
                .region(region)
                .build();

        // In this sample we only handle the specific exceptions for the 'GetSecretValue' API.
        // See https://docs.aws.amazon.com/secretsmanager/latest/apireference/API_GetSecretValue.html
        // We rethrow the exception by default.

        GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();
        GetSecretValueResponse getSecretValueResponse;

        try {
            getSecretValueResponse = client.getSecretValue(getSecretValueRequest);
        } catch (DecryptionFailureException | InternalServiceErrorException
                | InvalidParameterException | InvalidRequestException | ResourceNotFoundException e) {
            // Secrets Manager can't decrypt the protected secret text using the provided KMS key.
            // Deal with the exception here, and/or rethrow at your discretion.
            throw new SecretRetrieverException(SECRET_ERROR_MESSAGE, e);
        } finally {
            client.close();
        }

        Gson g = new Gson();
        String secret;
        String decodedBinarySecret;
        // Decrypts secret using the associated KMS CMK.
        // Depending on whether the secret is a string or binary, one of these fields will be populated.
        if (getSecretValueResponse.secretString() != null) {
            secret = getSecretValueResponse.secretString();
            SecretFormat secretJson = g.fromJson(secret,SecretFormat.class);
            return secretJson.ALMA_APIKEY;
        } else {
            decodedBinarySecret = new String(Base64.getDecoder()
                    .decode(getSecretValueResponse.secretBinary().asByteBuffer()).array());
            SecretFormat secretJson = g.fromJson(decodedBinarySecret, SecretFormat.class);
            return secretJson.ALMA_APIKEY;
        }
    }
}
