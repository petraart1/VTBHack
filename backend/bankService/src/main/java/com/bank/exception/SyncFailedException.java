package com.bank.exception;

/**
 * Исключение, выбрасываемое при ошибках синхронизации данных с банком
 */
public class SyncFailedException extends RuntimeException {

    public SyncFailedException(String message) {
        super("Sync failed: " + message);
    }

    public SyncFailedException(String message, Throwable cause) {
        super("Sync failed: " + message, cause);
    }
}
