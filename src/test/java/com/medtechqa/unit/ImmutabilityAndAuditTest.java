package com.medtechqa.unit;

import com.medtechqa.domain.AuditAction;
import com.medtechqa.domain.AuditEntry;
import com.medtechqa.domain.DomainException;
import com.medtechqa.domain.PlanningFile;
import com.medtechqa.domain.SurgicalCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImmutabilityAndAuditTest {

    // ---- REQ-007 -------------------------------------------------------------------------------

    @Test
    @Tag("REQ-007")
    @DisplayName("REQ-007 an approved case rejects uploads and keeps all its data")
    void approvedCaseRejectsUploadsAndStaysUnchanged() {
        SurgicalCase c = Fixtures.approved();
        List<PlanningFile> filesBefore = c.getFiles();

        assertThrows(DomainException.StateConflict.class, () -> c.addFile("more.stl", 100, Fixtures.CREATOR));
        assertThrows(DomainException.StateConflict.class, () -> c.addFile("more.stl", 100, Fixtures.REVIEWER));

        assertEquals(filesBefore, c.getFiles());
        assertEquals(Fixtures.CREATOR, c.getCreatedBy());
        assertEquals(Fixtures.REVIEWER, c.getApprovedBy());
    }

    @Test
    @Tag("REQ-007")
    @DisplayName("REQ-007 the returned file list cannot be used to change the case")
    void fileListIsACopy() {
        SurgicalCase c = Fixtures.draftWithFile();

        assertThrows(UnsupportedOperationException.class, () -> c.getFiles().clear());

        assertEquals(1, c.getFiles().size());
    }

    // ---- REQ-008 -------------------------------------------------------------------------------

    @Test
    @Tag("REQ-008")
    @DisplayName("REQ-008 the full lifecycle is logged in order with system timestamps")
    void lifecycleIsLoggedInOrder() {
        SurgicalCase c = SurgicalCase.create("id-1", Fixtures.PATIENT_ID, "KNEE", Fixtures.CREATOR, Fixtures.tickingClock());
        c.addFile("plan.stl", 100, Fixtures.CREATOR);
        c.submitForReview(Fixtures.CREATOR);
        c.approve(Fixtures.REVIEWER, Fixtures.PATIENT_ID);

        List<AuditEntry> trail = c.getAuditTrail();

        assertEquals(List.of(AuditAction.CASE_CREATED, AuditAction.FILE_UPLOADED,
                        AuditAction.SUBMITTED_FOR_REVIEW, AuditAction.APPROVED),
                trail.stream().map(AuditEntry::action).toList());
        assertEquals(List.of("mueller", "mueller", "mueller", "chirurg"),
                trail.stream().map(AuditEntry::user).toList());
        for (int i = 1; i < trail.size(); i++) {
            assertTrue(trail.get(i).timestamp().isAfter(trail.get(i - 1).timestamp()), "entry " + i + " must be later");
        }
    }

    static Stream<Arguments> rejectedApprovals() {
        return Stream.of(
                Arguments.of("wrong status", (Supplier<SurgicalCase>) Fixtures::draftWithFile, "chirurg", Fixtures.PATIENT_ID),
                Arguments.of("same user", (Supplier<SurgicalCase>) Fixtures::inReview, "mueller", Fixtures.PATIENT_ID),
                Arguments.of("no file", (Supplier<SurgicalCase>) Fixtures::inReviewWithoutFile, "chirurg", Fixtures.PATIENT_ID),
                Arguments.of("wrong confirmation", (Supplier<SurgicalCase>) Fixtures::inReview, "chirurg", "P-000000"),
                Arguments.of("missing confirmation", (Supplier<SurgicalCase>) Fixtures::inReview, "chirurg", null));
    }

    @ParameterizedTest(name = "[{index}] rejected approval ({0}) is logged")
    @DisplayName("REQ-008 every rejected approval attempt is logged with user and reason")
    @Tag("REQ-008")
    @MethodSource("rejectedApprovals")
    void rejectedApprovalIsLogged(String reason, Supplier<SurgicalCase> caseSupplier, String user, String confirmation) {
        SurgicalCase c = caseSupplier.get();
        int entriesBefore = c.getAuditTrail().size();

        assertThrows(DomainException.class, () -> c.approve(user, confirmation));

        List<AuditEntry> trail = c.getAuditTrail();
        assertEquals(entriesBefore + 1, trail.size());
        AuditEntry last = trail.get(trail.size() - 1);
        assertEquals(AuditAction.APPROVAL_REJECTED, last.action());
        assertEquals(user, last.user());
        assertFalse(last.details().isBlank(), "the reason must be logged");
    }

    // Origin: AI proposal KI-10 (see docs/AI-TESTING.md), adopted after review.
    @Test
    @Tag("REQ-006")
    @Tag("REQ-008")
    @Tag("REQ-010")
    @DisplayName("REQ-006 REQ-008 REQ-010 two violations at once (creator, wrong confirmation) give exactly one audit entry")
    void twoViolationsAtOnceAreLoggedOnce() {
        SurgicalCase c = Fixtures.inReview();
        int entriesBefore = c.getAuditTrail().size();

        assertThrows(DomainException.NotPermitted.class, () -> c.approve(Fixtures.CREATOR, "P-000000"));

        List<AuditEntry> trail = c.getAuditTrail();
        assertEquals(entriesBefore + 1, trail.size());
        assertEquals(AuditAction.APPROVAL_REJECTED, trail.get(trail.size() - 1).action());
    }

    @Test
    @Tag("REQ-008")
    @DisplayName("REQ-008 rejected uploads (wrong type, too large, empty) are not logged")
    void rejectedUploadsAreNotLogged() {
        SurgicalCase c = Fixtures.draft();
        int entriesBefore = c.getAuditTrail().size();

        assertThrows(DomainException.class, () -> c.addFile("plan.exe", 100, Fixtures.CREATOR));
        assertThrows(DomainException.class, () -> c.addFile("plan.stl", SurgicalCase.MAX_FILE_SIZE_BYTES + 1, Fixtures.CREATOR));
        assertThrows(DomainException.class, () -> c.addFile("plan.stl", 0, Fixtures.CREATOR));

        assertEquals(entriesBefore, c.getAuditTrail().size());
    }

    @Test
    @Tag("REQ-008")
    @DisplayName("REQ-008 a rejected action without a user is not logged, nobody could be named")
    void actionWithoutUserIsNotLogged() {
        SurgicalCase c = Fixtures.inReview();
        int entriesBefore = c.getAuditTrail().size();

        assertThrows(DomainException.InvalidInput.class, () -> c.approve(null, Fixtures.PATIENT_ID));

        assertEquals(entriesBefore, c.getAuditTrail().size());
    }

    // ---- REQ-009 -------------------------------------------------------------------------------

    @Test
    @Tag("REQ-009")
    @DisplayName("REQ-009 changing the returned audit list has no effect on the stored trail")
    void returnedAuditListIsACopy() {
        SurgicalCase c = Fixtures.inReview();
        List<AuditEntry> before = c.getAuditTrail();
        AuditEntry first = before.get(0);

        assertThrows(UnsupportedOperationException.class, () -> c.getAuditTrail().clear());
        assertThrows(UnsupportedOperationException.class, () -> c.getAuditTrail().remove(0));
        assertThrows(UnsupportedOperationException.class,
                () -> c.getAuditTrail().add(new AuditEntry(first.timestamp(), "x", AuditAction.APPROVED, "forged")));
        assertThrows(UnsupportedOperationException.class, () -> c.getAuditTrail().set(0, first));

        assertEquals(before, c.getAuditTrail());
    }

    @Test
    @Tag("REQ-009")
    @DisplayName("REQ-009 an audit entry keeps timestamp, user and action when read again later")
    void auditEntryStaysTheSame() {
        SurgicalCase c = Fixtures.inReview();
        AuditEntry firstRead = c.getAuditTrail().get(0);

        c.approve(Fixtures.REVIEWER, Fixtures.PATIENT_ID);
        AuditEntry secondRead = c.getAuditTrail().get(0);

        assertEquals(firstRead, secondRead);
        assertEquals(AuditAction.CASE_CREATED, secondRead.action());
    }
}
