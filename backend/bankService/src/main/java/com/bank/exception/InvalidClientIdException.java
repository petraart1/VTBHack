package com.bank.exception;

import com.bank.config.BankingConstants;

/**
 * Исключение, выбрасываемое при некорректном формате clientId
 */
public class InvalidClientIdException extends RuntimeException {

    public InvalidClientIdException(String message) {
        super(message);
    }

    public InvalidClientIdException(String clientId, String pattern) {
        super(String.format(BankingConstants.INVALID_CLIENT_ID_FORMAT_MESSAGE, clientId, pattern));
    }
}
