package rocks.inspectit.ocelot.config.model.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import javax.validation.constraints.AssertFalse;
import java.net.URL;
import java.time.Duration;

/**
 * Defines the settings for using a HTTP property source.
 */
@Data
@NoArgsConstructor
public class HttpConfigSettings {

    /**
     * Whether a HTTP property source should be used.
     */
    private boolean enabled;

    /**
     * The URL for fetching the configuration.
     */
    private URL url;

    /**
     * The frequency of polling the HTTP endpoint.
     */
    @NonNull
    private Duration frequency;

    /**
     * The connection timeout to use - the time to establish the connection with the remote host.
     */
    private Duration connectionTimeout;

    /**
     * The socket timeout to use - the time waiting for data after establishing the connection; maximum time of inactivity between two data packets.
     */
    private Duration socketTimeout;

    @AssertFalse(message = "The specified timeout values should not be negative!")
    public boolean isNegativeTimeout() {
        boolean negativeConnectionTimeout = connectionTimeout != null && connectionTimeout.isNegative();
        boolean negativeReadTimeout = socketTimeout != null && socketTimeout.isNegative();
        return negativeConnectionTimeout || negativeReadTimeout;
    }
}