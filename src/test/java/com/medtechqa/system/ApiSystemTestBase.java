package com.medtechqa.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.FormData;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * System tests: the application runs on a random port in a real servlet container, and Playwright's
 * APIRequestContext talks to it over real HTTP. Browsers are not needed here, so their download is skipped.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class ApiSystemTestBase {

    static final String PATIENT_ID = "P-004711";
    private static final ObjectMapper JSON = new ObjectMapper();

    private static Playwright playwright;

    @LocalServerPort
    private int port;

    protected APIRequestContext api;

    @BeforeAll
    static void startPlaywright() {
        playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
    }

    @AfterAll
    static void stopPlaywright() {
        playwright.close();
    }

    @BeforeEach
    void openApiContext() {
        api = playwright.request().newContext(new APIRequest.NewContextOptions().setBaseURL("http://localhost:" + port));
    }

    @AfterEach
    void closeApiContext() {
        api.dispose();
    }

    protected JsonNode json(APIResponse response) throws IOException {
        return JSON.readTree(response.text());
    }

    protected String createCase(String user) throws IOException {
        APIResponse response = api.post("/api/cases", RequestOptions.create()
                .setHeader("X-User", user)
                .setData(Map.of("patientId", PATIENT_ID, "interventionType", "KNEE")));
        assertEquals(201, response.status(), response.text());
        return json(response).get("id").asText();
    }

    protected APIResponse upload(String caseId, String user, String fileName, byte[] content) {
        return api.post("/api/cases/" + caseId + "/files", RequestOptions.create()
                .setHeader("X-User", user)
                .setMultipart(FormData.create().set("file",
                        new com.microsoft.playwright.options.FilePayload(fileName, "application/octet-stream", content))));
    }

    protected APIResponse submit(String caseId, String user) {
        return api.post("/api/cases/" + caseId + "/submit", RequestOptions.create().setHeader("X-User", user));
    }

    protected APIResponse approve(String caseId, String user, String confirmation) {
        Map<String, Object> body = new HashMap<>();
        if (confirmation != null) {
            body.put("confirmPatientId", confirmation);
        }
        return api.post("/api/cases/" + caseId + "/approve", RequestOptions.create()
                .setHeader("X-User", user).setData(body));
    }

    protected JsonNode getCase(String caseId) throws IOException {
        return json(api.get("/api/cases/" + caseId));
    }

    protected JsonNode getAudit(String caseId) throws IOException {
        return json(api.get("/api/cases/" + caseId + "/audit"));
    }

    protected String caseInReview(String creator) throws IOException {
        String id = createCase(creator);
        assertEquals(201, upload(id, creator, "plan.stl", new byte[]{1, 2, 3}).status());
        assertEquals(200, submit(id, creator).status());
        return id;
    }
}
