package com.ayudatrace.dao;

/** Unchecked wrapper so the UI layer does not need to handle SQLException everywhere. */
public class DataAccessException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
