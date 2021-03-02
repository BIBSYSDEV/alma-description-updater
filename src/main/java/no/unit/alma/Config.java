package no.unit.alma;

import no.unit.utils.StringUtils;

import java.awt.*;

public class Config {

    public static final String MISSING_ENVIRONMENT_VARIABLES = "Missing environment variables ALMA_SRU_HOST or ALMA_UPDATE_HOST";
    public static final String CORS_ALLOW_ORIGIN_HEADER_ENVIRONMENT_NAME = "ALLOWED_ORIGIN";
    public static final String ALMA_SRU_HOST_KEY = "ALMA_SRU_HOST";

    private String corsHeader;
    private String almaSruEndpoint;


    private Config() {
    }

    private static class LazyHolder {

        private static final Config INSTANCE = new Config();

        static {
            INSTANCE.setAlmaSruEndpoint(System.getenv(ALMA_SRU_HOST_KEY));
            INSTANCE.setCorsHeader(System.getenv(CORS_ALLOW_ORIGIN_HEADER_ENVIRONMENT_NAME));
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
        if (StringUtils.isEmpty(almaSruEndpoint)) {
            throw new RuntimeException(MISSING_ENVIRONMENT_VARIABLES);
        }
        return true;
    }

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
