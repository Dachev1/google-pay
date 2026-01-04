package io.briklabs.sample.exceptions;

/**
 * Exception thrown when a security-related error occurs during cryptographic operations.
 */
public class SecurityException extends RuntimeException {

    public SecurityException(String message) {
        super(message);
    }

    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}
