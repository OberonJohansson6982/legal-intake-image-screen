package dev.example.legalintake;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(InfraiSettings.class)
public class LegalIntakeApplication {
    public static void main(String[] args) { SpringApplication.run(LegalIntakeApplication.class, args); }
}
