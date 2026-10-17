package com.medtechqa.domain;

/**
 * A business rule was violated. The subclass tells the API layer which HTTP status fits;
 * the domain itself knows nothing about HTTP.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    /** Input is malformed or missing (-> 400). */
    public static class InvalidInput extends DomainException {
        public InvalidInput(String message) {
            super(message);
        }
    }

    /** The case is in a state that does not allow the action, or a precondition is not met (-> 409). */
    public static class StateConflict extends DomainException {
        public StateConflict(String message) {
            super(message);
        }
    }

    /** The acting user is not allowed to do this, e.g. approving own case (-> 403). */
    public static class NotPermitted extends DomainException {
        public NotPermitted(String message) {
            super(message);
        }
    }

    /** File type is not on the allow-list (-> 415). */
    public static class UnsupportedFileType extends DomainException {
        public UnsupportedFileType(String message) {
            super(message);
        }
    }

    /** File exceeds the size limit (-> 413). */
    public static class FileTooLarge extends DomainException {
        public FileTooLarge(String message) {
            super(message);
        }
    }
}
