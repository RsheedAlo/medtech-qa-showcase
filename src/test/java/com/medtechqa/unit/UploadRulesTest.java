package com.medtechqa.unit;

import com.medtechqa.domain.DomainException;
import com.medtechqa.domain.SurgicalCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadRulesTest {

    private static final long MAX = SurgicalCase.MAX_FILE_SIZE_BYTES;

    // Technique: equivalence partitioning. Allowed class: .stl/.pdf/.png in any letter case.
    // Rejected classes: other extension, no extension, trailing dot, allowed extension followed by another one.
    @ParameterizedTest(name = "[{index}] file ''{0}'' is accepted")
    @DisplayName("REQ-002 allowed file types are accepted")
    @Tag("REQ-002")
    @ValueSource(strings = {"plan.stl", "plan.pdf", "plan.png", "PLAN.STL", "Plan.Pdf", "my.plan.v2.png"})
    void allowedFileTypesAreAccepted(String fileName) {
        SurgicalCase c = Fixtures.draft();

        c.addFile(fileName, 100, Fixtures.CREATOR);

        assertEquals(1, c.getFiles().size());
        assertEquals(fileName, c.getFiles().get(0).name());
    }

    @ParameterizedTest(name = "[{index}] file ''{0}'' is rejected")
    @DisplayName("REQ-002 other file types are rejected and not attached")
    @Tag("REQ-002")
    @NullSource
    @ValueSource(strings = {"plan.exe", "plan.jpg", "plan.txt", "plan", "plan.", "plan.stl.exe", "plan.stlx", ""})
    void otherFileTypesAreRejected(String fileName) {
        SurgicalCase c = Fixtures.draft();

        assertThrows(DomainException.class, () -> c.addFile(fileName, 100, Fixtures.CREATOR));

        assertTrue(c.getFiles().isEmpty());
    }

    @Test
    @Tag("REQ-002")
    @DisplayName("REQ-002 a path in the file name is dropped, only the file name is stored")
    void pathPartOfFileNameIsDropped() {
        SurgicalCase c = Fixtures.draft();

        c.addFile("..\\..\\secret\\evil.stl", 100, Fixtures.CREATOR);
        c.addFile("../../other/evil2.pdf", 100, Fixtures.CREATOR);

        assertEquals("evil.stl", c.getFiles().get(0).name());
        assertEquals("evil2.pdf", c.getFiles().get(1).name());
    }

    // Origin: AI proposal KI-04 (see docs/AI-TESTING.md), adopted after review.
    @Test
    @Tag("REQ-002")
    @DisplayName("REQ-002 only the last extension counts: plan.exe.stl is accepted")
    void onlyLastExtensionCounts() {
        SurgicalCase c = Fixtures.draft();

        c.addFile("plan.exe.stl", 100, Fixtures.CREATOR);

        assertEquals(1, c.getFiles().size());
    }

    // Technique: boundary value analysis around 10 MB = 10_485_760 byte, and around 0.
    @ParameterizedTest(name = "[{index}] {0} byte is accepted")
    @DisplayName("REQ-003 sizes up to and including exactly 10 MB are accepted")
    @Tag("REQ-003")
    @ValueSource(longs = {1, 2, MAX - 1, MAX})
    void sizesWithinLimitAreAccepted(long size) {
        SurgicalCase c = Fixtures.draft();

        c.addFile("plan.stl", size, Fixtures.CREATOR);

        assertEquals(size, c.getFiles().get(0).sizeBytes());
    }

    @ParameterizedTest(name = "[{index}] {0} byte is rejected as too large")
    @DisplayName("REQ-003 10 MB + 1 byte and more are rejected, message names the limit")
    @Tag("REQ-003")
    @ValueSource(longs = {MAX + 1, MAX + 2, Long.MAX_VALUE})
    void sizesAboveLimitAreRejected(long size) {
        SurgicalCase c = Fixtures.draft();

        DomainException.FileTooLarge e = assertThrows(DomainException.FileTooLarge.class,
                () -> c.addFile("plan.stl", size, Fixtures.CREATOR));

        assertTrue(e.getMessage().contains("10 MB"), "message should name the limit: " + e.getMessage());
        assertTrue(c.getFiles().isEmpty());
    }

    // Origin: AI proposal KI-09 (see docs/AI-TESTING.md), adopted after review.
    @Test
    @Tag("REQ-003")
    @DisplayName("REQ-003 the limit applies per file, a case may hold several files of 10 MB")
    void limitAppliesPerFileNotPerCase() {
        SurgicalCase c = Fixtures.draft();

        c.addFile("first.stl", MAX, Fixtures.CREATOR);
        c.addFile("second.stl", MAX, Fixtures.CREATOR);

        assertEquals(2, c.getFiles().size());
    }

    @ParameterizedTest(name = "[{index}] {0} byte is rejected as empty")
    @DisplayName("REQ-003 empty files (0 byte) are rejected")
    @Tag("REQ-003")
    @ValueSource(longs = {0, -1})
    void emptyFilesAreRejected(long size) {
        SurgicalCase c = Fixtures.draft();

        assertThrows(DomainException.InvalidInput.class, () -> c.addFile("plan.stl", size, Fixtures.CREATOR));

        assertTrue(c.getFiles().isEmpty());
    }

    @Test
    @Tag("REQ-002")
    @DisplayName("REQ-002 an upload without a user is rejected")
    void uploadWithoutUserIsRejected() {
        SurgicalCase c = Fixtures.draft();

        assertThrows(DomainException.InvalidInput.class, () -> c.addFile("plan.stl", 100, " "));

        assertTrue(c.getFiles().isEmpty());
    }
}
