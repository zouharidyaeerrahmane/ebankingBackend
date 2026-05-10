package org.java.enset.dezy.ebanking_backend.chatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.enset.dezy.ebanking_backend.dtos.*;
import org.java.enset.dezy.ebanking_backend.exceptions.BankAccountNotFoundException;
import org.java.enset.dezy.ebanking_backend.services.BankAccountService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Fournit un contexte en temps réel depuis la base de données bancaire.
 * Ce contexte s'ajoute aux documents statiques dans le pipeline RAG,
 * permettant au chatbot de répondre sur des données concrètes (soldes, comptes…).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankingContextService {

    private final BankAccountService bankAccountService;

    /**
     * Génère un contexte textuel pour un compte précis.
     * Appelé quand l'utilisateur fournit un identifiant de compte.
     */
    public String getAccountContext(String accountId) {
        try {
            BankAccountDTO dto = bankAccountService.getBankAccount(accountId);

            if (dto instanceof CurrentBankAccountDTO curr) {
                return String.format("""
                        [Données en temps réel — Compte Courant]
                        Identifiant : %s
                        Titulaire   : %s
                        Solde       : %.2f DH
                        Découvert autorisé : %.2f DH
                        Solde utilisable total : %.2f DH
                        Statut      : %s
                        Créé le     : %s
                        """,
                        curr.getId(),
                        curr.getCustomerDTO() != null ? curr.getCustomerDTO().getName() : "N/A",
                        curr.getBalance(),
                        curr.getOverDraft(),
                        curr.getBalance() + curr.getOverDraft(),
                        curr.getStatus(),
                        curr.getCreatedAt());
            }

            if (dto instanceof SavingBankAccountDTO sav) {
                return String.format("""
                        [Données en temps réel — Compte Épargne]
                        Identifiant : %s
                        Titulaire   : %s
                        Solde       : %.2f DH
                        Taux d'intérêt annuel : %.2f%%
                        Statut      : %s
                        Créé le     : %s
                        """,
                        sav.getId(),
                        sav.getCustomerDTO() != null ? sav.getCustomerDTO().getName() : "N/A",
                        sav.getBalance(),
                        sav.getInterestRate(),
                        sav.getStatus(),
                        sav.getCreatedAt());
            }

        } catch (BankAccountNotFoundException e) {
            log.warn("Compte introuvable : {}", accountId);
            return "Aucun compte trouvé avec l'identifiant : " + accountId;
        }
        return "";
    }

    /**
     * Génère un résumé global (statistiques anonymisées) de l'état du système.
     * Utilisé pour des questions générales sur le système.
     */
    public String getSystemSummary() {
        try {
            List<BankAccountDTO> allAccounts = bankAccountService.bankAccountList();
            long currentCount = allAccounts.stream().filter(a -> a instanceof CurrentBankAccountDTO).count();
            long savingCount  = allAccounts.stream().filter(a -> a instanceof SavingBankAccountDTO).count();
            long customers    = bankAccountService.listCustomers().size();

            return String.format("""
                    [Statistiques du système E-Banking]
                    Clients enregistrés    : %d
                    Comptes courants       : %d
                    Comptes épargne        : %d
                    Total comptes          : %d
                    """, customers, currentCount, savingCount, allAccounts.size());
        } catch (Exception e) {
            log.warn("Impossible de récupérer les statistiques système : {}", e.getMessage());
            return "";
        }
    }
}
