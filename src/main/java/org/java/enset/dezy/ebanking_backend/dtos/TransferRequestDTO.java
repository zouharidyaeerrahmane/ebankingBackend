package org.java.enset.dezy.ebanking_backend.dtos;


import lombok.Data;

@Data
public class TransferRequestDTO {
    private String accountSource;
    private String accountDestination;
    private double amount;
    private String description;
}