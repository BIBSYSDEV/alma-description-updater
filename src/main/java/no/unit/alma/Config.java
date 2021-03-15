package no.unit.alma;

import no.unit.utils.StringUtils;
import nva.commons.utils.Environment;


public class Config {

    public static final String MISSING_ENVIRONMENT_VARIABLES =
            "Missing environment variables ALMA_SRU_HOST or ALMA_API_HOST";
    public static final String CORS_ALLOW_ORIGIN_HEADER_ENVIRONMENT_NAME = "ALLOWED_ORIGIN";
    public static final String ALMA_SRU_HOST_KEY = "ALMA_SRU_HOST";
    public static final String ALMA_API_KEY = "ALMA_API_HOST";

    private String corsHeader;
    private String almaSruEndpoint;
    private String almaApiEndpoint;
    private static final Environment envHandler = new Environment();


    private Config() {
    }

    private static class LazyHolder {

        private static final Config INSTANCE = new Config();

        static {
            INSTANCE.setAlmaApiEndpoint(envHandler.readEnv(ALMA_API_KEY));
            INSTANCE.setAlmaSruEndpoint(envHandler.readEnv(ALMA_SRU_HOST_KEY));
            try{
                INSTANCE.setCorsHeader(envHandler.readEnv(CORS_ALLOW_ORIGIN_HEADER_ENVIRONMENT_NAME));
            } catch (IllegalStateException e) {
                INSTANCE.setCorsHeader("*");
            }

        }
    }

    public static Config getInstance() {
        return LazyHolder.INSTANCE;
    }

    /**
     * Checking if almaSruEndpoint is present.
     *
     * @return <code>TRUE</code> if property is present.
     */
    public boolean checkProperties() {
        if (StringUtils.isEmpty(almaSruEndpoint) || StringUtils.isEmpty(almaApiEndpoint)) {
            throw new RuntimeException(MISSING_ENVIRONMENT_VARIABLES);
        }
        return true;
    }

    public String getAlmaApiEndpoint() { return almaApiEndpoint; }

    public void setAlmaApiEndpoint(String almaApiEndpoint) { this.almaApiEndpoint = almaApiEndpoint; }

    public String getAlmaSruEndpoint() {
        return almaSruEndpoint;
    }

    public void setAlmaSruEndpoint(String almaSruEndpoint) {
        this.almaSruEndpoint = almaSruEndpoint;
    }

    public String getCorsHeader() {
        return corsHeader;
    }

    public void setCorsHeader(String corsHeader) {
        this.corsHeader = corsHeader;
    }

}
