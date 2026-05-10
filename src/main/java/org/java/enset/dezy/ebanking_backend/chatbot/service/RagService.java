package org.java.enset.dezy.ebanking_backend.chatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Pipeline RAG sans Spring AI :
 *
 *  1. RETRIEVE  — recherche par mots-clés dans les documents bancaires
 *  2. AUGMENT   — construction du prompt enrichi (docs + données compte)
 *  3. GENERATE  — appel HTTP direct à l'API Groq (LLM)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final DocumentSearchService documentSearchService;
    private final GroqChatService groqChatService;
    private final BankingContextService bankingContextService;

    private static final int TOP_K = 4;

    public String answer(String question) {
        return answer(question, null);
    }

    public String answer(String question, String accountId) {
        log.debug("RAG — question={}, accountId={}", question, accountId);

        // ── 1. RETRIEVE ──────────────────────────────────────────────────────
        List<String> relevantChunks = documentSearchService.search(question, TOP_K);
        log.debug("{} chunk(s) pertinent(s) récupéré(s)", relevantChunks.size());

        // ── 2. AUGMENT ───────────────────────────────────────────────────────
        StringBuilder context = new StringBuilder();

        if (!relevantChunks.isEmpty()) {
            context.append("=== Informations bancaires ===\n");
            context.append(relevantChunks.stream()
                    .map(c -> "• " + c)
                    .collect(Collectors.joining("\n\n")));
            context.append("\n\n");
        }

        if (accountId != null && !accountId.isBlank()) {
            context.append("=== Données du compte ===\n");
            context.append(bankingContextService.getAccountContext(accountId));
        } else {
            String summary = bankingContextService.getSystemSummary();
            if (!summary.isBlank()) {
                context.append("=== État du système ===\n").append(summary);
            }
        }

        String userPrompt = context.isEmpty()
                ? "Question : " + question
                : String.format("""
                        Contexte :
                        ─────────────────────────────────────
                        %s
                        ─────────────────────────────────────
                        Question : %s
                        """, context.toString().trim(), question);

        // ── 3. GENERATE ──────────────────────────────────────────────────────
        return groqChatService.chat(userPrompt);
    }
}
