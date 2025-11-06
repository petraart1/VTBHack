package com.bank.dto.mapper;

import com.bank.dto.response.BalanceDto;
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
