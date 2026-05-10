package org.java.enset.dezy.ebanking_backend.chatbot.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Charge les documents .txt depuis resources/docs/ et implémente
 * une recherche par mots-clés (RAG sans embeddings).
 *
 * Algorithme : score = nombre de mots de la requête présents dans le chunk
 * (mots > 3 caractères, insensible à la casse).
 */
@Service
@Slf4j
public class DocumentSearchService {

    private final List<String> chunks = new ArrayList<>();

    private static final String[] DOC_FILES = {
            "docs/banking_faq.txt",
            "docs/banking_services.txt",
            "docs/banking_operations.txt"
    };

    @PostConstruct
    public void loadDocuments() {
        log.info("=== Chargement de la base de connaissances ===");
        for (String file : DOC_FILES) {
            try {
                ClassPathResource resource = new ClassPathResource(file);
                String content = new BufferedReader(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));

                // Découper par paragraphes (double saut de ligne)
                String[] paragraphs = content.split("\n\\s*\n");
                int count = 0;
                for (String p : paragraphs) {
                    String trimmed = p.trim();
                    if (trimmed.length() > 50) {
                        chunks.add(trimmed);
                        count++;
                    }
                }
                log.info("  [OK] {} — {} chunks chargés", file, count);
            } catch (Exception e) {
                log.error("  [ERREUR] {} : {}", file, e.getMessage());
            }
        }
        log.info("=== Base de connaissances prête ({} chunks total) ===", chunks.size());
    }

    /**
     * Retourne les {@code topK} chunks les plus pertinents pour la requête.
     */
    public List<String> search(String query, int topK) {
        if (chunks.isEmpty() || query == null || query.isBlank()) {
            return List.of();
        }

        // Extraire les mots significatifs de la requête (> 3 caractères)
        Set<String> queryWords = Arrays.stream(
                        query.toLowerCase().replaceAll("[^a-zàâçéèêëîïôûùüÿñæœ0-9 ]", " ").split("\\s+"))
                .filter(w -> w.length() > 3)
                .collect(Collectors.toSet());

        if (queryWords.isEmpty()) {
            return chunks.stream().limit(topK).collect(Collectors.toList());
        }

        return chunks.stream()
                .map(chunk -> {
                    String chunkLower = chunk.toLowerCase();
                    long score = queryWords.stream()
                            .filter(chunkLower::contains)
                            .count();
                    return Map.entry(chunk, score);
                })
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
