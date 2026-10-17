package com.medtechqa.api;

import com.medtechqa.domain.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Map;

/** Translates exceptions into HTTP status codes with a readable message: {"error": "..."} */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DomainException.InvalidInput.class)
    ResponseEntity<Map<String, String>> invalidInput(DomainException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(DomainException.NotPermitted.class)
    ResponseEntity<Map<String, String>> notPermitted(DomainException e) {
        return body(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(CaseNotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(CaseNotFoundException e) {
        return body(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(DomainException.StateConflict.class)
    ResponseEntity<Map<String, String>> conflict(DomainException e) {
        return body(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(DomainException.FileTooLarge.class)
    ResponseEntity<Map<String, String>> tooLarge(DomainException e) {
        return body(HttpStatus.PAYLOAD_TOO_LARGE, e.getMessage());
    }

    /** Safety net for the framework's own upload limit; the domain limit is exact, this one is a coarse guard. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, String>> frameworkTooLarge(MaxUploadSizeExceededException e) {
        return body(HttpStatus.PAYLOAD_TOO_LARGE, "Datei zu groß. Das Limit liegt bei 10 MB.");
    }

    @ExceptionHandler(DomainException.UnsupportedFileType.class)
    ResponseEntity<Map<String, String>> unsupportedType(DomainException e) {
        return body(HttpStatus.UNSUPPORTED_MEDIA_TYPE, e.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestPartException.class})
    ResponseEntity<Map<String, String>> malformed(Exception e) {
        return body(HttpStatus.BAD_REQUEST, "Anfrage unvollständig oder nicht lesbar.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Map<String, String>> methodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return body(HttpStatus.METHOD_NOT_ALLOWED, "Diese Aktion ist nicht erlaubt (" + e.getMethod() + ").");
    }

    private static ResponseEntity<Map<String, String>> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
