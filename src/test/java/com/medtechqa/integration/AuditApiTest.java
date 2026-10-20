package com.medtechqa.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditApiTest extends ApiTestSupport {

    private String auditJson(String caseId) throws Exception {
        return mvc.perform(get("/api/cases/{id}/audit", caseId)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @Tag("REQ-008")
    @DisplayName("REQ-008 the audit trail has one entry per change, in order, with user and timestamp")
    void lifecycleIsLogged() throws Exception {
        String id = caseInReview("mueller");
        approve(id, "chirurg", PATIENT_ID).andExpect(status().isOk());

        String body = auditJson(id);

        assertEquals(List.of("CASE_CREATED", "FILE_UPLOADED", "SUBMITTED_FOR_REVIEW", "APPROVED"),
                JsonPath.<List<String>>read(body, "$[*].action"));
        assertEquals(List.of("mueller", "mueller", "mueller", "chirurg"),
                JsonPath.<List<String>>read(body, "$[*].user"));
        List<String> timestamps = JsonPath.read(body, "$[*].timestamp");
        for (int i = 1; i < timestamps.size(); i++) {
            assertFalse(Instant.parse(timestamps.get(i)).isBefore(Instant.parse(timestamps.get(i - 1))),
                    "timestamps must not go backwards");
        }
    }

    @Test
    @Tag("REQ-008")
    @DisplayName("REQ-008 a rejected approval is logged with user, action and reason")
    void rejectedApprovalIsLogged() throws Exception {
        String id = caseInReview("mueller");
        approve(id, "mueller", PATIENT_ID).andExpect(status().isForbidden());

        String body = auditJson(id);

        List<String> actions = JsonPath.read(body, "$[*].action");
        List<String> users = JsonPath.read(body, "$[*].user");
        List<String> details = JsonPath.read(body, "$[*].details");
        int last = actions.size() - 1;
        assertEquals("APPROVAL_REJECTED", actions.get(last));
        assertEquals("mueller", users.get(last));
        assertTrue(details.get(last).contains("Vier-Augen"));
    }

    @Test
    @Tag("REQ-008")
    @DisplayName("REQ-008 a rejected upload (wrong type) leaves no audit entry")
    void rejectedUploadIsNotLogged() throws Exception {
        String id = createCase("mueller");
        int before = auditActions(id).size();

        upload(id, "mueller", "plan.exe", new byte[]{1}).andExpect(status().isUnsupportedMediaType());
        upload(id, "mueller", "plan.stl", new byte[10 * 1024 * 1024 + 1]).andExpect(status().isPayloadTooLarge());

        assertEquals(before, auditActions(id).size());
    }

    @Test
    @Tag("REQ-008")
    @DisplayName("REQ-008 the audit trail of a case contains only entries of that case")
    void auditTrailsAreSeparatedPerCase() throws Exception {
        String first = createCase("anna");
        String second = createCase("bernd");
        upload(second, "bernd", "plan.stl", new byte[]{1}).andExpect(status().isCreated());

        assertEquals(List.of("CASE_CREATED"), auditActions(first));
        assertEquals(List.of("CASE_CREATED", "FILE_UPLOADED"), auditActions(second));
        assertEquals(List.of("anna"), JsonPath.<List<String>>read(auditJson(first), "$[*].user"));
    }

    // Origin: AI proposal KI-13 (see docs/AI-TESTING.md), adopted after review.
    @Test
    @Tag("REQ-008")
    @DisplayName("REQ-008 a timestamp sent by the client is ignored, the system sets the audit timestamp")
    void clientTimestampIsIgnored() throws Exception {
        Instant before = Instant.now();

        String body = mvc.perform(post("/api/cases").header(USER, "mueller")
                        .contentType("application/json")
                        .content("{\"patientId\":\"P-004711\",\"interventionType\":\"KNEE\",\"timestamp\":\"2000-01-01T00:00:00Z\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        Instant after = Instant.now();
        Instant logged = Instant.parse(JsonPath.read(auditJson(id), "$[0].timestamp"));
        assertFalse(logged.isBefore(before), "timestamp must come from the system, not from the request");
        assertFalse(logged.isAfter(after));
    }

    @Test
    @Tag("REQ-009")
    @DisplayName("REQ-009 changing or deleting audit entries via the API is rejected and the trail stays")
    void auditEntriesCannotBeChangedViaApi() throws Exception {
        String id = caseInReview("mueller");
        String before = auditJson(id);

        mvc.perform(put("/api/cases/{id}/audit", id).contentType("application/json").content("[]"))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(delete("/api/cases/{id}/audit", id)).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/cases/{id}/audit", id).contentType("application/json").content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(delete("/api/cases/{id}/audit/0", id)).andExpect(status().is4xxClientError());
        mvc.perform(put("/api/cases/{id}/audit/0", id).contentType("application/json").content("{}"))
                .andExpect(status().is4xxClientError());

        assertEquals(before, auditJson(id));
    }

    @Test
    @Tag("REQ-009")
    @DisplayName("REQ-009 an audit entry is identical when the trail is fetched again later")
    void auditEntryStaysIdentical() throws Exception {
        String id = caseInReview("mueller");
        String firstEntry = JsonPath.<Object>read(auditJson(id), "$[0]").toString();

        approve(id, "chirurg", PATIENT_ID).andExpect(status().isOk());

        assertEquals(firstEntry, JsonPath.<Object>read(auditJson(id), "$[0]").toString());
    }

    @Test
    @Tag("REQ-008")
    @DisplayName("REQ-008 the audit trail of an unknown case gives 404")
    void auditOfUnknownCaseGives404() throws Exception {
        mvc.perform(get("/api/cases/{id}/audit", "does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("nicht gefunden")));
    }
}
