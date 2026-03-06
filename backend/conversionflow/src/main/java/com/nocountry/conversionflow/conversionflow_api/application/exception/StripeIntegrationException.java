package com.nocountry.conversionflow.conversionflow_api.application.exception;

public class StripeIntegrationException extends RuntimeException {

    public StripeIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
