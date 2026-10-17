package com.medtechqa.system;

import com.microsoft.playwright.APIResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Uploads over a real HTTP connection: here the real multipart handling of the server is involved. */
class UploadSystemTest extends ApiSystemTestBase {

    private static final int TEN_MB = 10 * 1024 * 1024;

    @Test
    @Tag("REQ-003")
    @DisplayName("REQ-003 a file of exactly 10 MB is accepted over HTTP")
    void exactlyTenMegabytesIsAccepted() throws IOException {
        String id = createCase("mueller");

        APIResponse response = upload(id, "mueller", "plan.stl", new byte[TEN_MB]);

        assertEquals(201, response.status(), response.text());
        assertEquals(TEN_MB, getCase(id).get("files").get(0).get("sizeBytes").asLong());
    }

    @Test
    @Tag("REQ-003")
    @DisplayName("REQ-003 a file of 10 MB + 1 byte is rejected over HTTP with 413 and the limit in the message")
    void tenMegabytesPlusOneIsRejected() throws IOException {
        String id = createCase("mueller");

        APIResponse response = upload(id, "mueller", "plan.stl", new byte[TEN_MB + 1]);

        assertEquals(413, response.status());
        assertTrue(json(response).get("error").asText().contains("10 MB"));
        assertEquals(0, getCase(id).get("files").size());
    }

    @Test
    @Tag("REQ-003")
    @DisplayName("REQ-003 a very large file (30 MB) is stopped by the server guard with 413")
    void veryLargeFileIsStoppedByServer() throws IOException {
        String id = createCase("mueller");

        APIResponse response = upload(id, "mueller", "plan.stl", new byte[30 * 1024 * 1024]);

        assertEquals(413, response.status());
        assertEquals(0, getCase(id).get("files").size());
    }

    @Test
    @Tag("REQ-003")
    @DisplayName("REQ-003 an empty file is rejected over HTTP with 400")
    void emptyFileIsRejected() throws IOException {
        String id = createCase("mueller");

        assertEquals(400, upload(id, "mueller", "plan.stl", new byte[0]).status());
        assertEquals(0, getCase(id).get("files").size());
    }

    @ParameterizedTest(name = "[{index}] {0} is accepted")
    @DisplayName("REQ-002 allowed file types are accepted over HTTP")
    @Tag("REQ-002")
    @ValueSource(strings = {"plan.stl", "plan.pdf", "plan.png", "PLAN.PNG"})
    void allowedTypesAreAccepted(String fileName) throws IOException {
        String id = createCase("mueller");

        assertEquals(201, upload(id, "mueller", fileName, new byte[]{1}).status());
        assertEquals(1, getCase(id).get("files").size());
    }

    @ParameterizedTest(name = "[{index}] {0} is rejected")
    @DisplayName("REQ-002 other file types are rejected over HTTP with 415")
    @Tag("REQ-002")
    @ValueSource(strings = {"plan.exe", "plan.jpg", "plan.txt", "plan.stl.exe"})
    void otherTypesAreRejected(String fileName) throws IOException {
        String id = createCase("mueller");

        assertEquals(415, upload(id, "mueller", fileName, new byte[]{1}).status());
        assertEquals(0, getCase(id).get("files").size());
    }
}
