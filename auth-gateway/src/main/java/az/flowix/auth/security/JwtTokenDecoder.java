package az.flowix.auth.security;

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
public class JwtTokenDecoder {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenDecoder.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<String> extractRoles(String accessToken) {
        var roles = extractClaims(accessToken).get("roles");
        return stringList(roles);
    }

    public Map<String, Object> extractClaims(String accessToken) {
        try {
            var payload = decodePayload(accessToken);
            return objectMapper.readValue(payload, new TypeReference<>() {
            });
        } catch (Exception ex) {
            log.warn("Failed to extract claims from JWT", ex);
            return Collections.emptyMap();
        }
    }

    public List<String> extractPermissions(String accessToken) {
        return stringList(extractClaims(accessToken).get("permissions"));
    }

    public String extractUiScope(String accessToken) {
        var scope = extractClaims(accessToken).get("uiScope");
        return scope instanceof String value ? value : null;
    }

    private List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return List.of();
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
