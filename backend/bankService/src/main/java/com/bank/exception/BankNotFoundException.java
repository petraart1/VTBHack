package com.bank.exception;

/**
 * Исключение, выбрасываемое при попытке использования неизвестного банка
 */
public class BankNotFoundException extends RuntimeException {

    public BankNotFoundException(String bankId) {
        super("Unknown bank: " + bankId);
    }

    public BankNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
