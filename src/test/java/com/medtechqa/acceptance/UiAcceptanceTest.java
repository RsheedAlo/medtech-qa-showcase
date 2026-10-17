package com.medtechqa.acceptance;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.options.FilePayload;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Acceptance tests: replay acceptance criteria from requirements.md in a real browser.
 * Playwright's assertions wait for the expected state on their own, so there are no fixed sleeps.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UiAcceptanceTest {

    private static final AtomicInteger PATIENT_COUNTER = new AtomicInteger((int) (System.nanoTime() % 900_000));

    private static Playwright playwright;
    private static Browser browser;

    @LocalServerPort
    private int port;

    private BrowserContext context;
    private Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @AfterAll
    static void closeBrowser() {
        browser.close();
        playwright.close();
    }

    @BeforeEach
    void openPage() {
        context = browser.newContext();
        context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
        page = context.newPage();
        page.navigate("http://localhost:" + port + "/");
    }

    /** The trace (screenshots, DOM, network) is kept as CI artifact; open it with 'playwright show-trace'. */
    @AfterEach
    void closePage(TestInfo testInfo) {
        context.tracing().stop(new Tracing.StopOptions()
                .setPath(Paths.get("target", "playwright-traces", testInfo.getTestMethod().orElseThrow().getName() + ".zip")));
        context.close();
    }

    /** Every test uses its own patient id, so it finds exactly its own row in the shared case table. */
    private static String uniquePatientId() {
        return String.format("P-%06d", PATIENT_COUNTER.incrementAndGet() % 1_000_000);
    }

    private Locator rowOf(String patientId) {
        return page.locator("#cases tbody tr", new Page.LocatorOptions().setHasText(patientId));
    }

    private void createCaseAs(String user, String patientId) {
        page.fill("#user", user);
        page.fill("#patientId", patientId);
        page.click("#createBtn");
        assertThat(rowOf(patientId)).containsText("DRAFT");
    }

    private void uploadFile(String name, byte[] content) {
        page.setInputFiles("#fileInput", new FilePayload(name, "application/octet-stream", content));
        page.click("#uploadBtn");
    }

    @Test
    @Tag("REQ-006")
    @Tag("REQ-005")
    @Tag("REQ-010")
    @DisplayName("REQ-006 REQ-005 REQ-010 four-eyes principle in the browser: creator is refused, a second person approves")
    void fourEyesPrincipleInBrowser() {
        String patientId = uniquePatientId();
        createCaseAs("mueller", patientId);
        uploadFile("plan.stl", new byte[]{1, 2, 3});
        assertThat(rowOf(patientId)).containsText("plan.stl");
        page.click("#submitBtn");
        assertThat(rowOf(patientId)).containsText("IN_REVIEW");

        // The creator tries to approve own case
        page.fill("#confirmId", patientId);
        page.click("#approveBtn");
        assertThat(page.locator("#message")).containsText("Vier-Augen");
        assertThat(rowOf(patientId)).containsText("IN_REVIEW");
        assertThat(page.locator("#audit tbody")).containsText("APPROVAL_REJECTED");

        // A second person approves with the correct patient id
        page.fill("#user", "chirurg");
        page.click("#approveBtn");
        assertThat(page.locator("#message")).containsText("freigegeben");
        assertThat(rowOf(patientId)).containsText("APPROVED");
        assertThat(rowOf(patientId)).containsText("chirurg");
        assertThat(page.locator("#audit tbody")).containsText("APPROVED");
    }

    @Test
    @Tag("REQ-002")
    @Tag("REQ-001")
    @DisplayName("REQ-002 REQ-001 invalid patient id and wrong file type are refused with a clear message in the browser")
    void invalidInputIsRefusedInBrowser() {
        // REQ-001: invalid patient id
        page.fill("#patientId", "P-12345");
        page.click("#createBtn");
        assertThat(page.locator("#message")).containsText("Patient-ID");

        // REQ-002: .exe is not accepted and not attached to the case
        String patientId = uniquePatientId();
        createCaseAs("mueller", patientId);
        uploadFile("virus.exe", new byte[]{1});
        assertThat(page.locator("#message")).containsText("Dateityp nicht erlaubt");
        assertThat(rowOf(patientId)).not().containsText("virus.exe");
    }
}
