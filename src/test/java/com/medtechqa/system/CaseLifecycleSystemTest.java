package com.medtechqa.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaseLifecycleSystemTest extends ApiSystemTestBase {

    @Test
    @Tag("REQ-001")
    @Tag("REQ-004")
    @Tag("REQ-005")
    @Tag("REQ-006")
    @Tag("REQ-008")
    @Tag("REQ-010")
    @DisplayName("REQ-001 REQ-004 REQ-005 REQ-006 REQ-008 REQ-010 complete case lifecycle over HTTP, audit trail included")
    void completeLifecycle() throws IOException {
        String id = createCase("mueller");
        assertEquals("DRAFT", getCase(id).get("status").asText());

        assertEquals(201, upload(id, "mueller", "plan.stl", new byte[]{1, 2, 3}).status());
        assertEquals(200, submit(id, "mueller").status());
        assertEquals("IN_REVIEW", getCase(id).get("status").asText());

        APIResponse approved = approve(id, "chirurg", PATIENT_ID);
        assertEquals(200, approved.status());
        JsonNode result = json(approved);
        assertEquals("APPROVED", result.get("status").asText());
        assertEquals("chirurg", result.get("approvedBy").asText());

        List<String> actions = new ArrayList<>();
        getAudit(id).forEach(entry -> actions.add(entry.get("action").asText()));
        assertEquals(List.of("CASE_CREATED", "FILE_UPLOADED", "SUBMITTED_FOR_REVIEW", "APPROVED"), actions);
    }

    @Test
    @Tag("REQ-006")
    @Tag("REQ-008")
    @DisplayName("REQ-006 REQ-008 self-approval gives 403, the case stays IN_REVIEW and the attempt is logged")
    void selfApprovalIsRejectedAndLogged() throws IOException {
        String id = caseInReview("mueller");

        APIResponse response = approve(id, "MUELLER", PATIENT_ID);

        assertEquals(403, response.status());
        assertTrue(json(response).get("error").asText().contains("Vier-Augen"));
        assertEquals("IN_REVIEW", getCase(id).get("status").asText());
        JsonNode audit = getAudit(id);
        assertEquals("APPROVAL_REJECTED", audit.get(audit.size() - 1).get("action").asText());
    }

    @Test
    @Tag("REQ-005")
    @DisplayName("REQ-005 approval of a case without planning file gives 409")
    void approvalWithoutFileIsRejected() throws IOException {
        String id = createCase("mueller");
        assertEquals(200, submit(id, "mueller").status());

        assertEquals(409, approve(id, "chirurg", PATIENT_ID).status());
        assertEquals("IN_REVIEW", getCase(id).get("status").asText());
    }

    @Test
    @Tag("REQ-010")
    @DisplayName("REQ-010 approval with a wrong patient id gives 409 and the case stays IN_REVIEW")
    void wrongConfirmationIsRejected() throws IOException {
        String id = caseInReview("mueller");

        assertEquals(409, approve(id, "chirurg", "P-000000").status());
        assertEquals(409, approve(id, "chirurg", null).status());
        assertEquals("IN_REVIEW", getCase(id).get("status").asText());
    }

    @Test
    @Tag("REQ-007")
    @DisplayName("REQ-007 an approved case rejects uploads with 409 and stays unchanged")
    void approvedCaseIsImmutable() throws IOException {
        String id = caseInReview("mueller");
        assertEquals(200, approve(id, "chirurg", PATIENT_ID).status());
        String before = getCase(id).toString();

        assertEquals(409, upload(id, "mueller", "later.pdf", new byte[]{1}).status());
        assertEquals(409, submit(id, "mueller").status());

        assertEquals(before, getCase(id).toString());
    }

    @Test
    @Tag("REQ-009")
    @DisplayName("REQ-009 changing or deleting the audit trail over HTTP gives 405")
    void auditTrailCannotBeChanged() throws IOException {
        String id = caseInReview("mueller");
        String before = getAudit(id).toString();

        assertEquals(405, api.put("/api/cases/" + id + "/audit", RequestOptions.create().setData("[]")).status());
        assertEquals(405, api.delete("/api/cases/" + id + "/audit").status());

        assertEquals(before, getAudit(id).toString());
    }

    @Test
    @Tag("REQ-008")
    @DisplayName("REQ-008 the audit trails of two cases do not mix")
    void auditTrailsAreSeparated() throws IOException {
        String first = createCase("anna");
        String second = createCase("bernd");
        upload(second, "bernd", "plan.png", new byte[]{1});

        assertEquals(1, getAudit(first).size());
        assertEquals(2, getAudit(second).size());
        assertEquals("anna", getAudit(first).get(0).get("user").asText());
    }

    @Test
    @Tag("REQ-010")
    @DisplayName("REQ-010 an unknown case id gives 404")
    void unknownCaseGives404() {
        assertEquals(404, api.get("/api/cases/does-not-exist").status());
        assertEquals(404, upload("does-not-exist", "mueller", "plan.stl", new byte[]{1}).status());
        assertEquals(404, approve("does-not-exist", "chirurg", PATIENT_ID).status());
    }
}
