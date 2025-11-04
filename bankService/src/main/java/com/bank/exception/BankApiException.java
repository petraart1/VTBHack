package com.bank.exception;

import lombok.Getter;

/**
 * Исключение при работе с внешним Bank API
 * Содержит HTTP статус код для правильной обработки ошибок
 */
@Getter
public class BankApiException extends RuntimeException {
    private final int statusCode;
    private final String bankId;
    
    public BankApiException(String message, int statusCode, String bankId) {
        super(message);
        this.statusCode = statusCode;
        this.bankId = bankId;
    }
    
    public BankApiException(String message, Throwable cause, int statusCode, String bankId) {
        super(message, cause);
        this.statusCode = statusCode;
        this.bankId = bankId;
    }
}
