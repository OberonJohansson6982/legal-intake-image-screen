package dev.example.legalintake;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("infrai")
public record InfraiSettings(String baseUrl, String moderationModel, int maxAttempts) {
    public String apiKey() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        return key;
    }
}
