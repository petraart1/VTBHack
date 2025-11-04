package com.bank.dto;

import com.bank.model.AccountBalance;
import org.springframework.stereotype.Component;

@Component
public class BalanceMapper {

    public BalanceDto toDto(AccountBalance entity) {
        if (entity == null) {
            return null;
        }

        return new BalanceDto(
                entity.getBalanceType(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getCreditLine(),
                entity.getAsOfDateTime()
        );
    }
}

