package com.bank.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO для ответа внешнего банка на запрос списка счетов
 * Open Banking Russia v2.1 compatible
 */
public record ExternalAccountResponseDto(
        @JsonProperty("data")
        Data data,

        @JsonProperty("links")
        Links links,

        @JsonProperty("meta")
        Meta meta
) {

    public List<ExternalAccountDto> accounts() {
        return data != null && data.account() != null ? data.account() : List.of();
    }

    public record Data(
            @JsonProperty("account")
            List<ExternalAccountDto> account
    ) {
    }

    public record Links(
            @JsonProperty("self")
            String self
    ) {
    }

    public record Meta(
            @JsonProperty("totalPages")
            Integer totalPages
    ) {
    }
    
    public record ExternalAccountDto(
            @JsonProperty("accountId")
            String accountId,
            
            @JsonProperty("status")
            String status,
            
            @JsonProperty("currency")
            String currency,
            
            @JsonProperty("accountType")
            String accountType,

            @JsonProperty("accountSubType")
            String accountSubType,
            
            @JsonProperty("nickname")
            String nickname,
            
            @JsonProperty("openingDate")
            String openingDate,
            
            @JsonProperty("account")
            List<AccountDetail> account
    ) {

        public String accountNumber() {
            return account != null && !account.isEmpty() ? account.get(0).identification() : null;
        }

        public String iban() {
            return null; // API не предоставляет IBAN
        }
    }

    public record AccountDetail(
            @JsonProperty("schemeName")
            String schemeName,

            @JsonProperty("identification")
            String identification,

            @JsonProperty("name")
            String name
    ) {
    }
    
    public record BalanceDto(
            @JsonProperty("balance_type")
            String balanceType,
            
            @JsonProperty("amount")
            BigDecimal amount,
            
            @JsonProperty("currency")
            String currency,
            
            @JsonProperty("credit_line")
            BigDecimal creditLine
    ) {
    }
}
