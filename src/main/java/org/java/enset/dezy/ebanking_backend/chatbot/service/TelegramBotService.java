package org.java.enset.dezy.ebanking_backend.chatbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

/**
 * Bot Telegram — client du service RAG intégré dans le backend e-banking.
 *
 * Commandes :
 *   /start              — message de bienvenue
 *   /help               — aide et exemples
 *   /compte <accountId> — interroge un compte spécifique (données temps réel)
 *   <texte libre>       — question RAG générale
 */
@Service
@Slf4j
public class TelegramBotService implements SpringLongPollingBot, LongPollingUpdateConsumer {

    private final TelegramClient telegramClient;
    private final RagService ragService;
    private final String botToken;

    private static final String WELCOME = """
            Bonjour %s ! Je suis votre assistant E-Banking ENSET.

            Je peux vous aider avec :
            • Questions générales sur la banque
            • Comptes courant et épargne
            • Opérations bancaires (débit, crédit, virement)

            Commandes spéciales :
            /compte <identifiant> — infos en temps réel sur un compte
            /help                 — exemples de questions
            """;

    private static final String HELP = """
            Exemples de questions :
            • "Quelle est la différence entre un compte courant et un compte épargne ?"
            • "Comment effectuer un virement ?"
            • "Qu'est-ce qu'un découvert autorisé ?"
            • "Mon compte est suspendu, que faire ?"

            Pour interroger un compte précis :
              /compte abc123-...
            """;

    public TelegramBotService(
            RagService ragService,
            @Value("${telegram.bot.token}") String botToken) {
        this.ragService = ragService;
        this.botToken = botToken;
        this.telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(List<Update> updates) {
        updates.forEach(this::handleUpdate);
    }

    private void handleUpdate(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        Message msg = update.getMessage();
        String chatId    = msg.getChatId().toString();
        String text      = msg.getText().trim();
        String firstName = msg.getFrom().getFirstName();

        log.info("[Telegram] {} ({}) : {}", firstName, chatId, text);

        if ("/start".equals(text)) {
            send(chatId, WELCOME.formatted(firstName));
            return;
        }
        if ("/help".equals(text)) {
            send(chatId, HELP);
            return;
        }

        // /compte <accountId>  →  contexte temps réel depuis la DB
        if (text.startsWith("/compte ")) {
            String accountId = text.substring(8).trim();
            sendTyping(chatId);
            send(chatId, ragService.answer("Donne-moi les informations détaillées de ce compte.", accountId));
            return;
        }

        // Question libre → RAG général
        sendTyping(chatId);
        send(chatId, ragService.answer(text));
    }

    private void send(String chatId, String text) {
        try {
            telegramClient.execute(SendMessage.builder().chatId(chatId).text(text).build());
        } catch (TelegramApiException e) {
            log.error("Erreur envoi message Telegram : {}", e.getMessage());
        }
    }

    private void sendTyping(String chatId) {
        try {
            telegramClient.execute(SendChatAction.builder().chatId(chatId).action("typing").build());
        } catch (TelegramApiException e) {
            log.warn("Indicateur typing impossible : {}", e.getMessage());
        }
    }

    @AfterBotRegistration
    public void onRegistered(BotSession session) {
        log.info("Bot Telegram enregistré — session active : {}", session.isRunning());
    }
}
