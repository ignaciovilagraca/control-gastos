package com.xowl.spending.frameworks.exceptions;

public class TelegramApiException extends RuntimeException {
    public TelegramApiException(String message) {
        super(message);
    }
}
