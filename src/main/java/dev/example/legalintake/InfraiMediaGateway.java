package dev.example.legalintake;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.models.moderations.ModerationCreateParams;
import com.openai.models.moderations.ModerationImageUrlInput;
import com.openai.models.moderations.ModerationMultiModalInput;
import com.openai.models.moderations.ModerationTextInput;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

final class InfraiMediaGateway implements MatterPublicationService.MediaScreen {
    private final InfraiSettings settings; private final OpenAIClient ai; private final HttpClient http;
    private final ObjectMapper json; private final Sleeper sleeper;
    InfraiMediaGateway(InfraiSettings settings, OpenAIClient ai, HttpClient http, ObjectMapper json, Sleeper sleeper) {
        this.settings = settings; this.ai = ai; this.http = http; this.json = json; this.sleeper = sleeper;
    }

    @Override public MatterPublicationService.ModerationDecision moderate(String caption, byte[] image, String mediaType) {
        String dataUri = "data:" + mediaType + ";base64," + Base64.getEncoder().encodeToString(image);
        var text = ModerationMultiModalInput.ofText(ModerationTextInput.builder().text(caption).build());
        var imageInput = ModerationMultiModalInput.ofImageUrl(ModerationImageUrlInput.builder()
                .imageUrl(ModerationImageUrlInput.ImageUrl.builder().url(dataUri).build()).build());
        var params = ModerationCreateParams.builder().model(settings.moderationModel())
                .inputOfModerationMultiModalArray(List.of(text, imageInput)).build();
        boolean flagged = ai.moderations().create(params).results().stream().anyMatch(result -> result.flagged());
        return new MatterPublicationService.ModerationDecision(flagged);
    }

    @Override public String upload(byte[] image, String filename, String requestId) {
        JsonNode data = post("/image/upload", Map.of("file", Map.of("base64", Base64.getEncoder().encodeToString(image)),
                "filename", filename), requestId);
        return data.path("id").asText();
    }

    @Override public String resize(String image, String requestId) {
        JsonNode data = post("/image/resize", Map.of("image", image, "width", 1400, "height", 1800,
                "fit", "inside", "enlarge", false, "format", "webp", "store", true), requestId);
        return data.path("id").asText();
    }

    private JsonNode post(String path, Object body, String requestId) {
        for (int attempt = 0; attempt < settings.maxAttempts(); attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(settings.baseUrl() + path))
                        .timeout(Duration.ofSeconds(30)).header("Authorization", "Bearer " + settings.apiKey())
                        .header("Content-Type", "application/json").header("Idempotency-Key", requestId + path)
                        .method("POST", HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode envelope = json.readTree(response.body());
                if (response.statusCode() == 429 && attempt + 1 < settings.maxAttempts()) {
                    sleeper.sleep(retryDelay(response, attempt)); continue;
                }
                if (!envelope.path("ok").asBoolean(false)) {
                    JsonNode error = envelope.path("error");
                    throw new InfraiRejection(response.statusCode(), error.path("code").asText(), error.path("message").asText());
                }
                if (response.statusCode() >= 500) throw new IllegalStateException("Infrai transport response " + response.statusCode());
                return envelope.path("data");
            } catch (InfraiRejection error) { throw error;
            } catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException("Request interrupted", error);
            } catch (Exception error) { throw new IllegalStateException("Media request could not complete", error); }
        }
        throw new IllegalStateException("Retry budget exhausted");
    }
    private static long retryDelay(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After").map(Long::parseLong).map(seconds -> seconds * 1000L)
                .orElse(250L << attempt);
    }
    interface Sleeper { void sleep(long milliseconds) throws InterruptedException; }
    static final class InfraiRejection extends RuntimeException {
        final int status; final String code;
        InfraiRejection(int status, String code, String message) { super(message); this.status = status; this.code = code; }
    }
}
