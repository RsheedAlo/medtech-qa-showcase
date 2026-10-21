package com.medtechqa.api;

/** No case with that id (-&gt; 404). Lives in the api package because the domain does not look cases up. */
public class CaseNotFoundException extends RuntimeException {

    public CaseNotFoundException(String id) {
        super("Fall nicht gefunden: " + id);
    }
}
