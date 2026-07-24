package az.codlab.auth.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenValidator.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<String> extractRoles(String accessToken) {
        try {
            var payload = decodePayload(accessToken);
            Map<String, Object> claims = objectMapper.readValue(payload, new TypeReference<>() {
            });

            var roles = claims.get("roles");
            if (roles instanceof List<?> roleList) {
                return roleList.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .toList();

            }

            return Collections.emptyList();
        } catch (Exception ex) {
            log.warn("Failed to extract roles from JWT", ex);
            return Collections.emptyList();
        }
    }

    private String decodePayload(String jwt) {
        var parts = jwt.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid JWT format");
        }
        var decoded = Base64.getUrlDecoder().decode(parts[1]);
        return new String(decoded);
    }
}
