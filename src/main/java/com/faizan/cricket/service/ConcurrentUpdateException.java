package com.faizan.cricket.service;

/** Thrown when optimistic-lock retries were exhausted. The client may safely retry with the same eventId. */
public class ConcurrentUpdateException extends RuntimeException {
    public ConcurrentUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
