package dev.example.legalintake;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import java.net.http.HttpClient;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class InfraiConfiguration {
    @Bean OpenAIClient moderationClient(InfraiSettings settings) {
        return OpenAIOkHttpClient.builder().apiKey(settings.apiKey()).baseUrl(settings.baseUrl()).maxRetries(3).build();
    }
    @Bean InfraiMediaGateway mediaGateway(InfraiSettings settings, OpenAIClient moderationClient, ObjectMapper json) {
        return new InfraiMediaGateway(settings, moderationClient, HttpClient.newHttpClient(), json, Thread::sleep);
    }
    @Bean MatterPublicationService matterPublicationService(InfraiMediaGateway media, Clock clock) {
        return new MatterPublicationService(media, clock);
    }
    @Bean Clock clock() { return Clock.systemUTC(); }
}
