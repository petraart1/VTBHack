package com.bank.dto.mapper;

import com.bank.dto.response.AccountDto;
import com.bank.model.BankAccount;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountDto toDto(BankAccount entity) {
        if (entity == null) {
            return null;
        }

        return new AccountDto(
                entity.getId(),
                entity.getBankId(),
                entity.getExternalAccountId(),
                entity.getAccountNumberMasked(),
                entity.getIban(),
                entity.getAccountType(),
                entity.getCurrency(),
                entity.getNickname(),
                entity.getAvailableBalance(),
                entity.getBookedBalance(),
                entity.getCreditLimit(),
                entity.getStatus(),
                entity.getLastSyncAt(),
                entity.getCreatedAt()
        );
    }
}
