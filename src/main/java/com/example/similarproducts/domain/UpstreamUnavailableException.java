package com.example.similarproducts.domain;

/**
 * The product API failed (5xx), timed out or could not be reached.
 */
public class UpstreamUnavailableException extends RuntimeException {

    public UpstreamUnavailableException(String message) {
        super(message);
    }

    public UpstreamUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
