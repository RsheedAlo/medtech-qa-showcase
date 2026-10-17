package com.medtechqa.api;

public class CaseNotFoundException extends RuntimeException {

    public CaseNotFoundException(String id) {
        super("Fall nicht gefunden: " + id);
    }
}
