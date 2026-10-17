package com.medtechqa.unit;

import com.medtechqa.domain.SurgicalCase;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Builds cases in a known state. Every test gets its own instance, so tests stay independent. */
final class Fixtures {

    static final String CREATOR = "mueller";
    static final String REVIEWER = "chirurg";
    static final String PATIENT_ID = "P-004711";

    private Fixtures() {
    }

    static SurgicalCase draft() {
        return SurgicalCase.create("case-1", PATIENT_ID, "KNEE", CREATOR, Clock.systemUTC());
    }

    static SurgicalCase draftWithFile() {
        SurgicalCase c = draft();
        c.addFile("plan.stl", 1024, CREATOR);
        return c;
    }

    static SurgicalCase inReview() {
        SurgicalCase c = draftWithFile();
        c.submitForReview(CREATOR);
        return c;
    }

    static SurgicalCase inReviewWithoutFile() {
        SurgicalCase c = draft();
        c.submitForReview(CREATOR);
        return c;
    }

    static SurgicalCase approved() {
        SurgicalCase c = inReview();
        c.approve(REVIEWER, PATIENT_ID);
        return c;
    }

    /** A clock that advances one second per call, so timestamps are predictable and strictly increasing. */
    static Clock tickingClock() {
        return new Clock() {
            private Instant now = Instant.parse("2026-01-01T10:00:00Z");

            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public synchronized Instant instant() {
                Instant current = now;
                now = now.plusSeconds(1);
                return current;
            }
        };
    }
}
