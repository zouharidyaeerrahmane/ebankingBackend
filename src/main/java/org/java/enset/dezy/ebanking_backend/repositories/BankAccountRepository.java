package org.java.enset.dezy.ebanking_backend.repositories;

import org.java.enset.dezy.ebanking_backend.entities.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAccountRepository extends JpaRepository<BankAccount,String> {
}
