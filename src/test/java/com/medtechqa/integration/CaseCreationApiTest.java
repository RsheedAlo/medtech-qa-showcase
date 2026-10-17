package com.medtechqa.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CaseCreationApiTest extends ApiTestSupport {

    @Test
    @Tag("REQ-001")
    @DisplayName("REQ-001 POST /api/cases with valid data returns 201, DRAFT and the creator")
    void validCaseIsCreated() throws Exception {
        createCaseRaw("Mueller", "P-004711", "CRANIAL")
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.createdBy").value("Mueller"))
                .andExpect(jsonPath("$.patientId").value("P-004711"))
                .andExpect(jsonPath("$.interventionType").value("CRANIAL"));
    }

    @ParameterizedTest(name = "[{index}] patient id ''{0}''")
    @DisplayName("REQ-001 invalid patient ids give 400 with a message and create no case")
    @Tag("REQ-001")
    @ValueSource(strings = {"P-12345", "P-1234567", "X-123456", "P-12345A", ""})
    void invalidPatientIdIsRejected(String patientId) throws Exception {
        int before = caseCount();

        createCaseRaw("mueller", patientId, "KNEE")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Patient-ID")));

        assertEquals(before, caseCount());
    }

    @Test
    @Tag("REQ-001")
    @DisplayName("REQ-001 an intervention type outside the fixed list gives 400")
    void invalidInterventionTypeIsRejected() throws Exception {
        createCaseRaw("mueller", "P-004711", "FOOT")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Eingriffstyp")));
    }

    @Test
    @Tag("REQ-001")
    @DisplayName("REQ-001 creating a case without the X-User header gives 400")
    void missingUserIsRejected() throws Exception {
        int before = caseCount();

        createCaseRaw(null, "P-004711", "KNEE").andExpect(status().isBadRequest());

        assertEquals(before, caseCount());
    }
}
