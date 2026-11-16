package com.faizan.cricket.domain;

/** Thrown when a ball event breaks a cricket rule or the match is in the wrong state. */
public class InvalidEventException extends RuntimeException {
    public InvalidEventException(String message) {
        super(message);
    }
}
