package com.nocountry.conversionflow.conversionflow_api.application.exception;

public class StripeWebhookException extends RuntimeException {

    public StripeWebhookException(String message, Throwable cause) {
        super(message, cause);
    }
}
