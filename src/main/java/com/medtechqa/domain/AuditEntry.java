package com.medtechqa.domain;

import java.time.Instant;

/** Immutable by construction (record). The timestamp is set by the system, never by the caller. */
public record AuditEntry(Instant timestamp, String user, AuditAction action, String details) {
}
