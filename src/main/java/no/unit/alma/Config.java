package no.unit.alma;

import no.unit.exceptions.SchedulerException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class Config {

    public static final String ALMA_SRU_HOST_KEY = "ALMA_SRU_HOST";
    public static final String ALMA_API_HOST_KEY = "ALMA_API_HOST";
    public static final String ALMA_API_KEY = "ALMA_APIKEY";

    private transient String secretKey;
    private transient String almaApiHost;
    private transient String almaSruHost;

    private final transient Environment environment;

    /**
     * Config class to hold common variables for caching.
     */
    @JacocoGenerated
    public Config() {
        this(new Environment());
    }

    /**
     * Config class to hold common variables for caching.
     * @param environment Environment for injection
     */
    public Config(Environment environment) {
        this.environment = environment;
        try {
            initVariables();
        } catch (SchedulerException e) {
            throw new RuntimeException("Error while setting up env-variables and secretKeys. " + e.getMessage());
        }
    }

    /**
     * A method for assigning values to the secretkey and checking the environment variables.
     * @throws SchedulerException When something goes wrong.
     */
    private void initVariables() throws SchedulerException {
        try {
            almaApiHost = environment.readEnv(ALMA_API_HOST_KEY);
            almaSruHost = environment.readEnv(ALMA_SRU_HOST_KEY);
            secretKey = environment.readEnv(ALMA_API_KEY);
        } catch (IllegalStateException e) {
            throw new SchedulerException("Failed to initialize variables. ", e);
        }

    }

    public String getAlmaApiHost() {
        return almaApiHost;
    }

    public String getAlmaSruHost() {
        return almaSruHost;
    }

    public String getSecretKey() {
        return secretKey;
    }

}
