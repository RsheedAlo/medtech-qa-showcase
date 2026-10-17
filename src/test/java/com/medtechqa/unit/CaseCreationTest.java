package com.medtechqa.unit;

import com.medtechqa.domain.CaseStatus;
import com.medtechqa.domain.DomainException;
import com.medtechqa.domain.InterventionType;
import com.medtechqa.domain.SurgicalCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaseCreationTest {

    @Test
    @Tag("REQ-001")
    @DisplayName("REQ-001 a valid case starts in DRAFT and remembers its creator")
    void validCaseStartsInDraft() {
        SurgicalCase c = SurgicalCase.create("id-1", "P-004711", "HIP", "  Mueller ", Clock.systemUTC());

        assertEquals(CaseStatus.DRAFT, c.getStatus());
        assertEquals(InterventionType.HIP, c.getInterventionType());
        assertEquals("Mueller", c.getCreatedBy());
        assertTrue(c.getFiles().isEmpty());
    }

    // Technique: equivalence partitioning. One valid class (P- + exactly 6 digits, incl. both ends of the
    // digit range) and several invalid classes: too short, too long, wrong prefix, non-digit, wrong case, whitespace.
    @ParameterizedTest(name = "[{index}] patient id ''{0}'' is accepted")
    @DisplayName("REQ-001 patient id: valid class is accepted")
    @Tag("REQ-001")
    @ValueSource(strings = {"P-004711", "P-000000", "P-999999"})
    void validPatientIdsAreAccepted(String patientId) {
        SurgicalCase c = SurgicalCase.create("id-1", patientId, "KNEE", "mueller", Clock.systemUTC());

        assertEquals(patientId, c.getPatientId());
    }

    @ParameterizedTest(name = "[{index}] patient id ''{0}'' is rejected")
    @DisplayName("REQ-001 patient id: invalid classes are rejected")
    @Tag("REQ-001")
    @NullSource
    @ValueSource(strings = {
            "", "P-12345", "P-1234567", "X-123456", "P-12345A", "p-004711", "P004711",
            " P-004711", "P-004711 ", "P-004711\n",
            "P-００４７１１" // full-width digits look like digits but are not 0-9
    })
    void invalidPatientIdsAreRejected(String patientId) {
        assertThrows(DomainException.InvalidInput.class,
                () -> SurgicalCase.create("id-1", patientId, "KNEE", "mueller", Clock.systemUTC()));
    }

    @ParameterizedTest(name = "[{index}] intervention type ''{0}'' is accepted")
    @DisplayName("REQ-001 intervention type: every value of the fixed list is accepted")
    @Tag("REQ-001")
    @ValueSource(strings = {"KNEE", "HIP", "CRANIAL"})
    void validInterventionTypesAreAccepted(String type) {
        SurgicalCase c = SurgicalCase.create("id-1", "P-004711", type, "mueller", Clock.systemUTC());

        assertEquals(type, c.getInterventionType().name());
    }

    @ParameterizedTest(name = "[{index}] intervention type ''{0}'' is rejected")
    @DisplayName("REQ-001 intervention type: values outside the fixed list are rejected")
    @Tag("REQ-001")
    @NullSource
    @ValueSource(strings = {"", "FOOT", "knee", " KNEE", "KNEE "})
    void invalidInterventionTypesAreRejected(String type) {
        assertThrows(DomainException.InvalidInput.class,
                () -> SurgicalCase.create("id-1", "P-004711", type, "mueller", Clock.systemUTC()));
    }

    @ParameterizedTest(name = "[{index}] creator ''{0}'' is rejected")
    @DisplayName("REQ-001 a case without a creator is rejected")
    @Tag("REQ-001")
    @NullSource
    @ValueSource(strings = {"", "   "})
    void missingCreatorIsRejected(String user) {
        assertThrows(DomainException.InvalidInput.class,
                () -> SurgicalCase.create("id-1", "P-004711", "KNEE", user, Clock.systemUTC()));
    }
}
