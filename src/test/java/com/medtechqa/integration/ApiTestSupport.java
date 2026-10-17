package com.medtechqa.integration;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared helpers for the integration tests: controller + service + domain run together, without a real server.
 * Every test creates its own cases, so tests do not depend on each other or on the order.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class ApiTestSupport {

    static final String USER = "X-User";
    static final String PATIENT_ID = "P-004711";

    @Autowired
    protected MockMvc mvc;

    protected ResultActions createCaseRaw(String user, String patientId, String type) throws Exception {
        var request = post("/api/cases").contentType(MediaType.APPLICATION_JSON)
                .content("{\"patientId\":" + quote(patientId) + ",\"interventionType\":" + quote(type) + "}");
        if (user != null) {
            request.header(USER, user);
        }
        return mvc.perform(request);
    }

    protected String createCase(String user) throws Exception {
        String body = createCaseRaw(user, PATIENT_ID, "KNEE").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    protected ResultActions upload(String caseId, String user, String fileName, byte[] content) throws Exception {
        var request = multipart("/api/cases/{id}/files", caseId)
                .file(new MockMultipartFile("file", fileName, "application/octet-stream", content));
        if (user != null) {
            request.header(USER, user);
        }
        return mvc.perform(request);
    }

    protected ResultActions submit(String caseId, String user) throws Exception {
        var request = post("/api/cases/{id}/submit", caseId);
        if (user != null) {
            request.header(USER, user);
        }
        return mvc.perform(request);
    }

    protected ResultActions approve(String caseId, String user, String confirmPatientId) throws Exception {
        var request = post("/api/cases/{id}/approve", caseId).contentType(MediaType.APPLICATION_JSON)
                .content(confirmPatientId == null ? "{}" : "{\"confirmPatientId\":" + quote(confirmPatientId) + "}");
        if (user != null) {
            request.header(USER, user);
        }
        return mvc.perform(request);
    }

    /** A case in IN_REVIEW with one accepted file. */
    protected String caseInReview(String creator) throws Exception {
        String id = createCase(creator);
        upload(id, creator, "plan.stl", new byte[]{1, 2, 3}).andExpect(status().isCreated());
        submit(id, creator).andExpect(status().isOk());
        return id;
    }

    protected String statusOf(String caseId) throws Exception {
        return JsonPath.read(getCaseJson(caseId), "$.status");
    }

    protected String getCaseJson(String caseId) throws Exception {
        return mvc.perform(get("/api/cases/{id}", caseId)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    protected List<String> auditActions(String caseId) throws Exception {
        String body = mvc.perform(get("/api/cases/{id}/audit", caseId)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[*].action");
    }

    protected int caseCount() throws Exception {
        String body = mvc.perform(get("/api/cases")).andReturn().getResponse().getContentAsString();
        return JsonPath.<List<Object>>read(body, "$").size();
    }

    private static String quote(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
