package com.xowl.spending.frameworks.exceptions;

public class CustomHttpClientException extends RuntimeException {
    public CustomHttpClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
