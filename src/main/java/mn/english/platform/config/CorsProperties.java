package mn.english.platform.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Web sites (origins) that may call this API from a browser. Empty = none. */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null
                ? List.of()
                : allowedOrigins.stream().filter(origin -> !origin.isBlank()).toList();
    }
}
