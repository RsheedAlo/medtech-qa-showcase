package com.medtechqa.unit;

import com.medtechqa.domain.CaseStatus;
import com.medtechqa.domain.DomainException;
import com.medtechqa.domain.SurgicalCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Technique: state transition testing. 3 states x 2 actions = 6 cells of the transition table.
 * Two cells are valid, four are invalid. The approve cases start from an otherwise eligible case
 * (file present, other user, correct confirmation), so only the state decides.
 */
class WorkflowTest {

    private enum Action { SUBMIT, APPROVE }

    private static SurgicalCase caseIn(CaseStatus status) {
        return switch (status) {
            case DRAFT -> Fixtures.draftWithFile();
            case IN_REVIEW -> Fixtures.inReview();
            case APPROVED -> Fixtures.approved();
        };
    }

    private static void apply(SurgicalCase c, Action action) {
        switch (action) {
            case SUBMIT -> c.submitForReview(Fixtures.CREATOR);
            case APPROVE -> c.approve(Fixtures.REVIEWER, Fixtures.PATIENT_ID);
        }
    }

    static Stream<Arguments> validTransitions() {
        return Stream.of(
                Arguments.of(CaseStatus.DRAFT, Action.SUBMIT, CaseStatus.IN_REVIEW),
                Arguments.of(CaseStatus.IN_REVIEW, Action.APPROVE, CaseStatus.APPROVED));
    }

    static Stream<Arguments> invalidTransitions() {
        return Stream.of(
                Arguments.of(CaseStatus.DRAFT, Action.APPROVE),
                Arguments.of(CaseStatus.IN_REVIEW, Action.SUBMIT),
                Arguments.of(CaseStatus.APPROVED, Action.SUBMIT),
                Arguments.of(CaseStatus.APPROVED, Action.APPROVE));
    }

    @ParameterizedTest(name = "[{index}] {0} --{1}--> {2}")
    @DisplayName("REQ-004 valid transitions change the status")
    @Tag("REQ-004")
    @MethodSource("validTransitions")
    void validTransitionsChangeStatus(CaseStatus from, Action action, CaseStatus expected) {
        SurgicalCase c = caseIn(from);

        apply(c, action);

        assertEquals(expected, c.getStatus());
    }

    @ParameterizedTest(name = "[{index}] {0} --{1}--> rejected")
    @DisplayName("REQ-004 invalid transitions are rejected and the status stays")
    @Tag("REQ-004")
    @MethodSource("invalidTransitions")
    void invalidTransitionsAreRejected(CaseStatus from, Action action) {
        SurgicalCase c = caseIn(from);

        assertThrows(DomainException.StateConflict.class, () -> apply(c, action));

        assertEquals(from, c.getStatus());
    }
}
