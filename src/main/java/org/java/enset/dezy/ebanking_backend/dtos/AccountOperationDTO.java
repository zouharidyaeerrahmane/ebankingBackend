package org.java.enset.dezy.ebanking_backend.dtos;

import lombok.Data;
import org.java.enset.dezy.ebanking_backend.enums.OperationType;

import java.util.Date;

@Data
public class AccountOperationDTO {
    private Long id;
    private Date operationDate;
    private double amount;
    private OperationType type;
    private String description;
}