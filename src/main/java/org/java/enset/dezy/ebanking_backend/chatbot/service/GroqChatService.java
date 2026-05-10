package org.java.enset.dezy.ebanking_backend.chatbot.service;

import lombok.extern.slf4j.Slf4j;
import org.java.enset.dezy.ebanking_backend.chatbot.dto.GroqRequest;
import org.java.enset.dezy.ebanking_backend.chatbot.dto.GroqResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Appelle directement l'API Groq (compatible OpenAI) via RestTemplate.
 * Pas besoin de Spring AI — un simple appel HTTP suffit.
 */
@Service
@Slf4j
public class GroqChatService {

    private static final String SYSTEM_PROMPT = """
            Tu es un assistant bancaire virtuel de l'application E-Banking ENSET.
            Tu aides les clients avec leurs questions sur leurs comptes et opérations bancaires.
            Règles :
            - Réponds toujours en français, de manière claire et professionnelle.
            - Base tes réponses sur le contexte fourni.
            - Si l'information n'est pas dans le contexte, dis-le honnêtement.
            - Ne révèle jamais de données relatives à d'autres clients.
            """;

    private final RestTemplate restTemplate;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.chat.model}")
    private String model;

    @Value("${groq.chat.temperature}")
    private double temperature;

    @Value("${groq.chat.max-tokens}")
    private int maxTokens;

    public GroqChatService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String chat(String userMessage) {
        GroqRequest request = GroqRequest.builder()
                .model(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .messages(List.of(
                        GroqRequest.Message.builder().role("system").content(SYSTEM_PROMPT).build(),
                        GroqRequest.Message.builder().role("user").content(userMessage).build()
                ))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<GroqRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<GroqResponse> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, GroqResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String content = response.getBody().getContent();
                log.debug("Groq réponse reçue ({} caractères)", content != null ? content.length() : 0);
                return content != null ? content : "Aucune réponse générée.";
            }

            log.warn("Réponse Groq inattendue : {}", response.getStatusCode());
            return "Le service de chat est temporairement indisponible.";

        } catch (Exception e) {
            log.error("Erreur appel Groq API : {}", e.getMessage());
            return "Erreur lors de la communication avec le service IA. Veuillez réessayer.";
        }
    }
}
