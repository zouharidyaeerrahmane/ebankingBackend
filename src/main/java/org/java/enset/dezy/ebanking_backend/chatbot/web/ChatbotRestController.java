package org.java.enset.dezy.ebanking_backend.chatbot.web;

import lombok.RequiredArgsConstructor;
import org.java.enset.dezy.ebanking_backend.chatbot.dto.ChatRequest;
import org.java.enset.dezy.ebanking_backend.chatbot.dto.ChatResponse;
import org.java.enset.dezy.ebanking_backend.chatbot.service.RagService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint REST du chatbot RAG.
 *
 * POST /chatbot/ask
 * Authorization: Bearer <JWT>
 *
 * Body :  { "question": "...", "accountId": "..." (optionnel) }
 * Retour: { "answer": "..." }
 */
@RestController
@RequestMapping("/chatbot")
@CrossOrigin("*")
@RequiredArgsConstructor
public class ChatbotRestController {

    private final RagService ragService;

    @PostMapping("/ask")
    @PreAuthorize("hasAuthority('SCOPE_USER')")
    public ChatResponse ask(@RequestBody ChatRequest request) {
        String answer = ragService.answer(request.getQuestion(), request.getAccountId());
        return new ChatResponse(answer);
    }
}
