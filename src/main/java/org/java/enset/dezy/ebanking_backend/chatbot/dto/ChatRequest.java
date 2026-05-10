package org.java.enset.dezy.ebanking_backend.chatbot.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String question;
    private String accountId; // optionnel — enrichit le contexte avec les données du compte
}
