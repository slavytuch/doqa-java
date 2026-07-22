package app.doqa.e2e;

import app.doqa.Doqa;
import app.doqa.annotations.DoqaCaseIds;
import app.doqa.annotations.DoqaId;
import app.doqa.annotations.DoqaLabels;
import app.doqa.annotations.DoqaLink;
import app.doqa.annotations.DoqaLinks;
import app.doqa.annotations.DoqaTitle;
import app.doqa.annotations.Step;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * E2E demo suite executed by {@code LauncherEndToEndTest} through a nested JUnit Platform launcher
 * against a fake DoQA backend. Deliberately outside {@code app.doqa.junit5} so the AspectJ weaver
 * (which excludes adapter internals) weaves the {@code @Step} helper like real host test code.
 * The class name intentionally does not match surefire's {@code *Test} patterns - it must only run
 * inside the nested launcher.
 */
@DoqaLabels({"e2e-class"})
public class DemoLoginScenario {

    @BeforeAll
    static void beforeAllInit() {
        Doqa.step("prepare db", () -> { });
        Doqa.addAttachment("init.log", "class init ok");
    }

    @AfterAll
    static void afterAllCleanup() {
    }

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    @Tag("native-tag")
    @DoqaId("E2E-LOGIN-1")
    @DoqaTitle("Login works")
    @DoqaLabels({"smoke"})
    @DoqaLinks({@DoqaLink(url = "http://tracker/BUG-1", type = "defect", title = "known bug")})
    @DoqaCaseIds({901})
    void loginHappyPath() throws Exception {
        Doqa.addParameter("browser", "chrome");
        Doqa.addMessage("hello from runtime");
        Doqa.step("open login page", () -> {
            annotatedHelper();
            openPage("home");
        });
        Path shot = Files.createTempFile("doqa-e2e-shot", ".png");
        Files.write(shot, new byte[]{1, 2, 3});
        Doqa.addAttachments(shot.toString());
    }

    @Step("annotated helper")
    void annotatedHelper() {
    }

    @Step("open {page} page")
    void openPage(String page) {
    }

    @Test
    void assertionFails() {
        Assertions.fail("boom");
    }

    @Test
    void infrastructureBreaks() {
        throw new IllegalStateException("io error");
    }

    @Disabled("not today")
    @Test
    void skippedByAnnotation() {
    }

    @ParameterizedTest
    @ValueSource(strings = {"chrome", "firefox"})
    void worksInBrowser(String browser) {
        Assertions.assertNotNull(browser);
    }

    /** {param} placeholder: each invocation becomes a separate autotest. */
    @ParameterizedTest
    @ValueSource(strings = {"chrome", "firefox"})
    @DoqaId("E2E-BROWSER-{browser}")
    @DoqaTitle("Login in {browser}")
    void placeholderBrowser(String browser) {
        Assertions.assertNotNull(browser);
    }
}
