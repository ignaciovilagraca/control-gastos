package com.xowl.spending.usecases.exceptions;

public class JobExecutionException extends RuntimeException {
    public JobExecutionException(Throwable cause) {
        super(cause);
    }
}
