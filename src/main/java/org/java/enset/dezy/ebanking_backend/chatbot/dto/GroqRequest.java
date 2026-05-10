package org.java.enset.dezy.ebanking_backend.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GroqRequest {

    private String model;
    private List<Message> messages;
    private double temperature;

    @JsonProperty("max_tokens")
    private int maxTokens;

    @Data
    @Builder
    public static class Message {
        private String role;
        private String content;
    }
}
