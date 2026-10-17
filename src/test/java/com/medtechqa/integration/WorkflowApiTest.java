package com.medtechqa.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkflowApiTest extends ApiTestSupport {

    // ---- REQ-004: status codes of the state transitions ------------------------------------------

    @Test
    @Tag("REQ-004")
    @DisplayName("REQ-004 submit moves DRAFT to IN_REVIEW")
    void submitMovesToReview() throws Exception {
        String id = createCase("mueller");

        submit(id, "mueller").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_REVIEW"));
    }

    @Test
    @Tag("REQ-004")
    @DisplayName("REQ-004 approving a DRAFT case gives 409 and the status stays DRAFT")
    void approveInDraftIsRejected() throws Exception {
        String id = createCase("mueller");
        upload(id, "mueller", "plan.stl", new byte[]{1}).andExpect(status().isCreated());

        approve(id, "chirurg", PATIENT_ID).andExpect(status().isConflict());

        assertEquals("DRAFT", statusOf(id));
    }

    @Test
    @Tag("REQ-004")
    @DisplayName("REQ-004 submitting twice gives 409 and the status stays IN_REVIEW")
    void secondSubmitIsRejected() throws Exception {
        String id = caseInReview("mueller");

        submit(id, "mueller").andExpect(status().isConflict());

        assertEquals("IN_REVIEW", statusOf(id));
    }

    @Test
    @Tag("REQ-004")
    @DisplayName("REQ-004 every status change of an APPROVED case gives 409")
    void approvedCaseRejectsEveryTransition() throws Exception {
        String id = caseInReview("mueller");
        approve(id, "chirurg", PATIENT_ID).andExpect(status().isOk());

        submit(id, "mueller").andExpect(status().isConflict());
        approve(id, "other", PATIENT_ID).andExpect(status().isConflict());

        assertEquals("APPROVED", statusOf(id));
    }

    @Test
    @Tag("REQ-004")
    @DisplayName("REQ-004 submitting without a user gives 400")
    void submitWithoutUserIsRejected() throws Exception {
        String id = createCase("mueller");

        submit(id, null).andExpect(status().isBadRequest());

        assertEquals("DRAFT", statusOf(id));
    }

    // ---- REQ-005 ---------------------------------------------------------------------------------

    @Test
    @Tag("REQ-005")
    @DisplayName("REQ-005 approval without a planning file gives 409")
    void approvalWithoutFileIsRejected() throws Exception {
        String id = createCase("mueller");
        submit(id, "mueller").andExpect(status().isOk());

        approve(id, "chirurg", PATIENT_ID).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(containsString("Planungsdatei")));

        assertEquals("IN_REVIEW", statusOf(id));
    }

    @Test
    @Tag("REQ-005")
    @DisplayName("REQ-005 a rejected upload does not make the approval possible")
    void rejectedUploadDoesNotEnableApproval() throws Exception {
        String id = createCase("mueller");
        upload(id, "mueller", "plan.exe", new byte[]{1}).andExpect(status().isUnsupportedMediaType());
        submit(id, "mueller").andExpect(status().isOk());

        approve(id, "chirurg", PATIENT_ID).andExpect(status().isConflict());

        assertEquals("IN_REVIEW", statusOf(id));
    }

    @Test
    @Tag("REQ-005")
    @DisplayName("REQ-005 approval with a planning file gives 200 and APPROVED")
    void approvalWithFileSucceeds() throws Exception {
        String id = caseInReview("mueller");

        approve(id, "chirurg", PATIENT_ID).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    // ---- REQ-006 ---------------------------------------------------------------------------------

    @ParameterizedTest(name = "[{index}] approver ''{0}'' is the creator")
    @DisplayName("REQ-006 the creator (in any spelling) cannot approve: 403")
    @Tag("REQ-006")
    @ValueSource(strings = {"mueller", "Mueller", " mueller "})
    void creatorCannotApprove(String approver) throws Exception {
        String id = caseInReview("mueller");

        approve(id, approver, PATIENT_ID).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(containsString("Vier-Augen")));

        assertEquals("IN_REVIEW", statusOf(id));
    }

    @Test
    @Tag("REQ-006")
    @DisplayName("REQ-006 another user approves and is stored as approver")
    void otherUserApproves() throws Exception {
        String id = caseInReview("mueller");

        approve(id, "chirurg", PATIENT_ID).andExpect(status().isOk())
                .andExpect(jsonPath("$.approvedBy").value("chirurg"));
    }

    @Test
    @Tag("REQ-006")
    @DisplayName("REQ-006 approval without a user gives 400")
    void approvalWithoutUserIsRejected() throws Exception {
        String id = caseInReview("mueller");

        approve(id, null, PATIENT_ID).andExpect(status().isBadRequest());

        assertEquals("IN_REVIEW", statusOf(id));
        assertNull(com.jayway.jsonpath.JsonPath.read(getCaseJson(id), "$.approvedBy"));
    }

    // ---- REQ-007 ---------------------------------------------------------------------------------

    @Test
    @Tag("REQ-007")
    @DisplayName("REQ-007 after rejected changes an approved case is unchanged")
    void approvedCaseStaysUnchanged() throws Exception {
        String id = caseInReview("mueller");
        approve(id, "chirurg", PATIENT_ID).andExpect(status().isOk());
        String before = getCaseJson(id);

        upload(id, "mueller", "later.stl", new byte[]{1}).andExpect(status().isConflict());
        submit(id, "mueller").andExpect(status().isConflict());
        approve(id, "anderer", PATIENT_ID).andExpect(status().isConflict());

        assertEquals(before, getCaseJson(id));
    }

    // ---- REQ-010 ---------------------------------------------------------------------------------

    @ParameterizedTest(name = "[{index}] confirmation ''{0}''")
    @DisplayName("REQ-010 a wrong patient id confirmation gives 409 and the status stays")
    @Tag("REQ-010")
    @ValueSource(strings = {"P-000000", "P-004712", "p-004711"})
    void wrongConfirmationIsRejected(String confirmation) throws Exception {
        String id = caseInReview("mueller");

        approve(id, "chirurg", confirmation).andExpect(status().isConflict());

        assertEquals("IN_REVIEW", statusOf(id));
    }

    @Test
    @Tag("REQ-010")
    @DisplayName("REQ-010 approval without any confirmation gives 409")
    void missingConfirmationIsRejected() throws Exception {
        String id = caseInReview("mueller");

        approve(id, "chirurg", null).andExpect(status().isConflict());

        assertEquals("IN_REVIEW", statusOf(id));
    }

    @Test
    @Tag("REQ-010")
    @DisplayName("REQ-010 an unknown case id gives 404 for read, upload and approval and changes no other case")
    void unknownCaseIdGives404() throws Exception {
        String other = caseInReview("mueller");
        int auditBefore = auditActions(other).size();

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/cases/{id}", "does-not-exist"))
                .andExpect(status().isNotFound());
        upload("does-not-exist", "mueller", "plan.stl", new byte[]{1}).andExpect(status().isNotFound());
        approve("does-not-exist", "chirurg", PATIENT_ID).andExpect(status().isNotFound());

        assertEquals(auditBefore, auditActions(other).size());
        assertEquals("IN_REVIEW", statusOf(other));
    }
}
