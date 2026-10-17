package com.medtechqa.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UploadApiTest extends ApiTestSupport {

    private static final int TEN_MB = 10 * 1024 * 1024;

    @ParameterizedTest(name = "[{index}] {0} is accepted")
    @DisplayName("REQ-002 allowed file types give 201 and appear in the case")
    @Tag("REQ-002")
    @ValueSource(strings = {"plan.stl", "plan.pdf", "plan.png", "PLAN.STL"})
    void allowedTypesAreAccepted(String fileName) throws Exception {
        String id = createCase("mueller");

        upload(id, "mueller", fileName, new byte[]{1}).andExpect(status().isCreated())
                .andExpect(jsonPath("$.files", hasSize(1)))
                .andExpect(jsonPath("$.files[0].name").value(fileName));
    }

    @ParameterizedTest(name = "[{index}] {0} is rejected")
    @DisplayName("REQ-002 other file types give 415 and are not attached")
    @Tag("REQ-002")
    @ValueSource(strings = {"plan.exe", "plan.jpg", "plan.txt", "plan", "plan.stl.exe"})
    void otherTypesAreRejected(String fileName) throws Exception {
        String id = createCase("mueller");

        upload(id, "mueller", fileName, new byte[]{1}).andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value(containsString(".stl")));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/cases/{id}", id))
                .andExpect(jsonPath("$.files", hasSize(0)));
    }

    @Test
    @Tag("REQ-002")
    @DisplayName("REQ-002 an upload request without a file part gives 400")
    void missingFilePartIsRejected() throws Exception {
        String id = createCase("mueller");

        mvc.perform(multipart("/api/cases/{id}/files", id).header(USER, "mueller"))
                .andExpect(status().isBadRequest());
    }

    // Technique: boundary value analysis. 10 MB = 10_485_760 byte is the last valid size.
    @Test
    @Tag("REQ-003")
    @DisplayName("REQ-003 a file of exactly 10 MB is accepted")
    void exactlyTenMegabytesIsAccepted() throws Exception {
        String id = createCase("mueller");

        upload(id, "mueller", "plan.stl", new byte[TEN_MB]).andExpect(status().isCreated())
                .andExpect(jsonPath("$.files[0].sizeBytes").value(TEN_MB));
    }

    @Test
    @Tag("REQ-003")
    @DisplayName("REQ-003 a file of 10 MB + 1 byte gives 413 and the message names the limit")
    void tenMegabytesPlusOneByteIsRejected() throws Exception {
        String id = createCase("mueller");

        upload(id, "mueller", "plan.stl", new byte[TEN_MB + 1]).andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.error").value(containsString("10 MB")));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/cases/{id}", id))
                .andExpect(jsonPath("$.files", hasSize(0)));
    }

    @Test
    @Tag("REQ-003")
    @DisplayName("REQ-003 an empty file gives 400")
    void emptyFileIsRejected() throws Exception {
        String id = createCase("mueller");

        mvc.perform(multipart("/api/cases/{id}/files", id)
                        .file(new MockMultipartFile("file", "plan.stl", "application/octet-stream", new byte[0]))
                        .header(USER, "mueller"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Tag("REQ-007")
    @DisplayName("REQ-007 an upload to an approved case gives 409 and the file list stays")
    void uploadToApprovedCaseIsRejected() throws Exception {
        String id = caseInReview("mueller");
        approve(id, "chirurg", PATIENT_ID).andExpect(status().isOk());

        upload(id, "mueller", "later.stl", new byte[]{1}).andExpect(status().isConflict());

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/cases/{id}", id))
                .andExpect(jsonPath("$.files", hasSize(1)))
                .andExpect(jsonPath("$.files[0].name").value("plan.stl"));
    }
}
