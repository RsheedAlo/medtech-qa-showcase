package com.medtechqa.unit;

import com.medtechqa.domain.CaseStatus;
import com.medtechqa.domain.DomainException;
import com.medtechqa.domain.SurgicalCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The three approval conditions (file, other person, confirmed patient id) each get their own tests. */
class ApprovalRulesTest {

    @Test
    @Tag("REQ-005")
    @DisplayName("REQ-005 approval without a planning file is rejected")
    void approvalWithoutFileIsRejected() {
        SurgicalCase c = Fixtures.inReviewWithoutFile();

        assertThrows(DomainException.StateConflict.class, () -> c.approve(Fixtures.REVIEWER, Fixtures.PATIENT_ID));

        assertEquals(CaseStatus.IN_REVIEW, c.getStatus());
    }

    @Test
    @Tag("REQ-005")
    @DisplayName("REQ-005 a rejected upload does not count as planning file")
    void rejectedUploadDoesNotCountAsFile() {
        SurgicalCase c = Fixtures.draft();
        assertThrows(DomainException.UnsupportedFileType.class, () -> c.addFile("plan.exe", 100, Fixtures.CREATOR));
        c.submitForReview(Fixtures.CREATOR);

        assertThrows(DomainException.StateConflict.class, () -> c.approve(Fixtures.REVIEWER, Fixtures.PATIENT_ID));

        assertEquals(CaseStatus.IN_REVIEW, c.getStatus());
    }

    @Test
    @Tag("REQ-005")
    @DisplayName("REQ-005 approval with a planning file succeeds")
    void approvalWithFileSucceeds() {
        SurgicalCase c = Fixtures.inReview();

        c.approve(Fixtures.REVIEWER, Fixtures.PATIENT_ID);

        assertEquals(CaseStatus.APPROVED, c.getStatus());
    }

    @Test
    @Tag("REQ-006")
    @DisplayName("REQ-006 the creator cannot approve their own case")
    void creatorCannotApproveOwnCase() {
        SurgicalCase c = Fixtures.inReview();

        assertThrows(DomainException.NotPermitted.class, () -> c.approve(Fixtures.CREATOR, Fixtures.PATIENT_ID));

        assertEquals(CaseStatus.IN_REVIEW, c.getStatus());
        assertNull(c.getApprovedBy());
    }

    @ParameterizedTest(name = "[{index}] ''{0}'' counts as the creator")
    @DisplayName("REQ-006 the same person under a different spelling is still the creator")
    @Tag("REQ-006")
    @ValueSource(strings = {"Mueller", "MUELLER", " mueller ", "\tmueller"})
    void sameNameInOtherSpellingIsRejected(String spelling) {
        SurgicalCase c = Fixtures.inReview();

        assertThrows(DomainException.NotPermitted.class, () -> c.approve(spelling, Fixtures.PATIENT_ID));

        assertEquals(CaseStatus.IN_REVIEW, c.getStatus());
    }

    @Test
    @Tag("REQ-006")
    @DisplayName("REQ-006 another user can approve and is stored as approver")
    void otherUserApprovesAndIsStored() {
        SurgicalCase c = Fixtures.inReview();

        c.approve(" Chirurg ", Fixtures.PATIENT_ID);

        assertEquals(CaseStatus.APPROVED, c.getStatus());
        assertEquals("Chirurg", c.getApprovedBy());
    }

    @ParameterizedTest(name = "[{index}] approver ''{0}'' is rejected")
    @DisplayName("REQ-006 approval without a user is rejected")
    @Tag("REQ-006")
    @NullSource
    @ValueSource(strings = {"", "  "})
    void approvalWithoutUserIsRejected(String user) {
        SurgicalCase c = Fixtures.inReview();

        assertThrows(DomainException.InvalidInput.class, () -> c.approve(user, Fixtures.PATIENT_ID));

        assertEquals(CaseStatus.IN_REVIEW, c.getStatus());
    }

    @ParameterizedTest(name = "[{index}] confirmation ''{0}'' is rejected")
    @DisplayName("REQ-010 a missing or wrong patient id confirmation rejects the approval")
    @Tag("REQ-010")
    @NullSource
    @ValueSource(strings = {"", "P-000000", "P-004712", "p-004711", " P-004711", "P-004711 "})
    void wrongConfirmationIsRejected(String confirmation) {
        SurgicalCase c = Fixtures.inReview();

        assertThrows(DomainException.StateConflict.class, () -> c.approve(Fixtures.REVIEWER, confirmation));

        assertEquals(CaseStatus.IN_REVIEW, c.getStatus());
    }

    @Test
    @Tag("REQ-010")
    @DisplayName("REQ-010 the correct confirmation by another user approves the case")
    void correctConfirmationApproves() {
        SurgicalCase c = Fixtures.inReview();

        c.approve(Fixtures.REVIEWER, Fixtures.PATIENT_ID);

        assertEquals(CaseStatus.APPROVED, c.getStatus());
    }
}
