package com.bank.exception;

/**
 * Исключение, выбрасываемое при попытке получить несуществующую транзакцию
 * Обрабатывается в GlobalExceptionHandler
 */
public class TransactionNotFoundException extends RuntimeException {
    
    public TransactionNotFoundException(String message) {
        super(message);
    }
    
    public TransactionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

