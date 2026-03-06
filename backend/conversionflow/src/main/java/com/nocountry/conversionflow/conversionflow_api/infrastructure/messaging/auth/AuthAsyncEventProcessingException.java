package com.nocountry.conversionflow.conversionflow_api.infrastructure.messaging.auth;

public class AuthAsyncEventProcessingException extends RuntimeException {

    public AuthAsyncEventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
