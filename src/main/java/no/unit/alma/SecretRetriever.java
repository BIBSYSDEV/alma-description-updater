package no.unit.alma;

import com.google.gson.Gson;
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

    private class SecretFormat {
        String almaApiKey;
    }

    /**
     * This method gives you access to the alma api key,
     * it assumes you have the correct credentials.
     * @return String The api key used to access the Alma api.
     */
    public static String getSecret() {
        String secretName = "ALMA_APIKEY";
        Region region = Region.of("eu-west-1");
        Gson g = new Gson();

        // Create a Secrets Manager client
        SecretsManagerClient client = SecretsManagerClient.builder()
                .region(region)
                .build();

        // In this sample we only handle the specific exceptions for the 'GetSecretValue' API.
        // See https://docs.aws.amazon.com/secretsmanager/latest/apireference/API_GetSecretValue.html
        // We rethrow the exception by default.

        String secret;
        String decodedBinarySecret;
        GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();
        GetSecretValueResponse getSecretValueResponse = null;

        try {
            getSecretValueResponse = client.getSecretValue(getSecretValueRequest);
        } catch (DecryptionFailureException e) {
            // Secrets Manager can't decrypt the protected secret text using the provided KMS key.
            // Deal with the exception here, and/or rethrow at your discretion.
            throw e;
        } catch (InternalServiceErrorException e) {
            // An error occurred on the server side.
            // Deal with the exception here, and/or rethrow at your discretion.
            throw e;
        } catch (InvalidParameterException e) {
            // You provided an invalid value for a parameter.
            // Deal with the exception here, and/or rethrow at your discretion.
            throw e;
        } catch (InvalidRequestException e) {
            // You provided a parameter value that is not valid for the current state of the resource.
            // Deal with the exception here, and/or rethrow at your discretion.
            throw e;
        } catch (ResourceNotFoundException e) {
            // We can't find the resource that you asked for.
            // Deal with the exception here, and/or rethrow at your discretion.
            throw e;
        }

        // Decrypts secret using the associated KMS CMK.
        // Depending on whether the secret is a string or binary, one of these fields will be populated.
        if (getSecretValueResponse.secretString() != null) {
            secret = getSecretValueResponse.secretString();
            SecretFormat secretJson = g.fromJson(secret,SecretFormat.class);
            return secretJson.almaApiKey;
        } else {
            decodedBinarySecret = new String(Base64.getDecoder()
                    .decode(getSecretValueResponse.secretBinary().asByteBuffer()).array());
            SecretFormat secretJson = g.fromJson(decodedBinarySecret, SecretFormat.class);
            return secretJson.almaApiKey;
        }
    }
}
