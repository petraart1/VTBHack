package com.bank.dto.mapper;

import com.bank.dto.response.TransactionDto;
import com.bank.model.AccountTransaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionDto toDto(AccountTransaction entity) {
        if (entity == null) {
            return null;
        }

        return new TransactionDto(
                entity.getId(),
                entity.getAccountId(),
                entity.getExternalTransactionId(),
                entity.getBookingDateTime(),
                entity.getValueDateTime(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getDebitCreditIndicator(),
                entity.getStatus(),
                entity.getDescription(),
                entity.getMerchantName(),
                entity.getMerchantCategoryCode(),
                entity.getRunningBalance(),
                entity.getCreatedAt()
        );
    }
}
