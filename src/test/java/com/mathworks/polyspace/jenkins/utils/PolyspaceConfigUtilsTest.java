package com.mathworks.polyspace.jenkins.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import hudson.Functions;
import hudson.util.FormValidation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import com.mathworks.polyspace.jenkins.config.Messages;

@ExtendWith(MockitoExtension.class)
class PolyspaceConfigUtilsTest {

    @Spy
    @InjectMocks
    private PolyspaceConfigUtils polyspaceConfigUtilsSpy;

    @BeforeEach
    void setUp() {
        // Initialization if needed for each test
    }

    // Test methods will be added here

    @Test
    void testExeSuffix_Windows() {
        try (MockedStatic<Functions> mockedFunctions = mockStatic(Functions.class)) {
            mockedFunctions.when(Functions::isWindows).thenReturn(true);
            assertEquals(".exe", polyspaceConfigUtilsSpy.exeSuffix());
        }
    }

    @Test
    void testExeSuffix_NonWindows() {
        try (MockedStatic<Functions> mockedFunctions = mockStatic(Functions.class)) {
            mockedFunctions.when(Functions::isWindows).thenReturn(false);
            assertEquals("", polyspaceConfigUtilsSpy.exeSuffix());
        }
    }

    @Test
    void testDoCheckProtocol_Valid() {
        assertEquals(FormValidation.Kind.OK, polyspaceConfigUtilsSpy.doCheckProtocol("http").kind);
        assertEquals(FormValidation.Kind.OK, polyspaceConfigUtilsSpy.doCheckProtocol("https").kind);
    }

    @Test
    void testDoCheckProtocol_Invalid() {
        // Test with "ftp"
        FormValidation fvFtp = polyspaceConfigUtilsSpy.doCheckProtocol("ftp");
        assertEquals(Messages.wrongProtocol(), fvFtp.renderHtml(),
                "FormValidation message for 'ftp' protocol does not match.");
        assertEquals(FormValidation.Kind.ERROR, fvFtp.kind,
                "FormValidation kind for 'ftp' protocol should be ERROR.");

        // Test with empty string
        FormValidation fvEmpty = polyspaceConfigUtilsSpy.doCheckProtocol("");
        assertEquals(Messages.wrongProtocol(), fvEmpty.renderHtml(),
                "FormValidation message for empty protocol does not match.");
        assertEquals(FormValidation.Kind.ERROR, fvEmpty.kind,
                "FormValidation kind for empty protocol should be ERROR.");
    }

    @Test
    void testDoCheckPort_Valid() {
        assertEquals(FormValidation.Kind.OK, polyspaceConfigUtilsSpy.doCheckPort("8080").kind);
    }

    @Test
    void testDoCheckPort_Invalid() {
        // Test with "not-a-number"
        FormValidation fvNonNumeric = polyspaceConfigUtilsSpy.doCheckPort("not-a-number");
        assertEquals(Messages.portMustBeANumber(), fvNonNumeric.renderHtml(),
                "FormValidation message for 'not-a-number' port does not match.");
        assertEquals(FormValidation.Kind.ERROR, fvNonNumeric.kind,
                "FormValidation kind for 'not-a-number' port should be ERROR.");

        // Test with empty string
        FormValidation fvEmpty = polyspaceConfigUtilsSpy.doCheckPort("");
        assertEquals(Messages.portMustBeANumber(), fvEmpty.renderHtml(),
                "FormValidation message for empty port does not match.");
        assertEquals(FormValidation.Kind.ERROR, fvEmpty.kind,
                "FormValidation kind for empty port should be ERROR.");
    }

    @Test
    void testDoCheckFilename_Valid() {
        assertEquals(FormValidation.Kind.OK, polyspaceConfigUtilsSpy.doCheckFilename("relative/path").kind);
        assertEquals(FormValidation.Kind.OK, polyspaceConfigUtilsSpy.doCheckFilename("filename").kind);
        assertEquals(FormValidation.Kind.OK, polyspaceConfigUtilsSpy.doCheckFilename("").kind);
    }

    @Test
    void testDoCheckFilename_Invalid() {
        // Test absolute path starting with /
        FormValidation fvAbsSlash = polyspaceConfigUtilsSpy.doCheckFilename("/absolute/path");
        assertEquals(Messages.absoluteDirectoryForbidden(), fvAbsSlash.renderHtml(),
                "FormValidation message for absolute path (/) does not match.");
        assertEquals(FormValidation.Kind.ERROR, fvAbsSlash.kind,
                "FormValidation kind for absolute path (/) should be ERROR.");

        // Test absolute path starting with \
        FormValidation fvAbsBackslash = polyspaceConfigUtilsSpy.doCheckFilename("\\absolute\\path"); // Escaped backslashes
        assertEquals(Messages.absoluteDirectoryForbidden(), fvAbsBackslash.renderHtml(),
                "FormValidation message for absolute path (\\) does not match.");
        assertEquals(FormValidation.Kind.ERROR, fvAbsBackslash.kind,
                "FormValidation kind for absolute path (\\) should be ERROR.");

        // Test path containing .. (simple)
        FormValidation fvPrevDirSimple = polyspaceConfigUtilsSpy.doCheckFilename("../relative/path");
        assertEquals(Messages.previousDirectoryForbidden(), fvPrevDirSimple.renderHtml(),
                "FormValidation message for path with '..' (simple) does not match.");
        assertEquals(FormValidation.Kind.ERROR, fvPrevDirSimple.kind,
                "FormValidation kind for path with '..' (simple) should be ERROR.");

        // Test path containing .. (complex)
        FormValidation fvPrevDirComplex = polyspaceConfigUtilsSpy.doCheckFilename("filename/../..");
        assertEquals(Messages.previousDirectoryForbidden(), fvPrevDirComplex.renderHtml(),
                "FormValidation message for path with '..' (complex) does not match.");
        assertEquals(FormValidation.Kind.ERROR, fvPrevDirComplex.kind,
                "FormValidation kind for path with '..' (complex) should be ERROR.");
    }

    @Test
    void testCheckPolyspaceBinFolderExists_InvalidPath() {
        // This path is unlikely to exist or be a directory
        String invalidPath = "this/path/should/not/exist/as/a/directory/for/bin/folder/check"; // Made path more specific
        PolyspaceConfigUtils utils = new PolyspaceConfigUtils(); // Use a local instance as before
        try {
            utils.checkPolyspaceBinFolderExists(invalidPath);
            fail("FormValidation warning was expected for non-existent directory");
        } catch (FormValidation fv) {
            // First, check the message content using the pattern from PolyspaceBuildTest.java
            // Assuming Messages.polyspaceBinNotFound() returns the exact string expected by renderHtml() for warnings
            // or that renderHtml() for warnings simply returns the message string.
            assertEquals(Messages.polyspaceBinNotFound(), fv.renderHtml(),
                    "FormValidation message for non-existent bin folder does not match.");
            // Then, check the kind
            assertEquals(FormValidation.Kind.WARNING, fv.kind,
                    "FormValidation kind for non-existent bin folder should be WARNING.");
        }
    }

    @Test
    void testCheckPolyspaceBinFolderExists_ValidPath(@TempDir Path tempDir) {
        PolyspaceConfigUtils utils = new PolyspaceConfigUtils();
        // tempDir provides a path to an existing directory
        assertDoesNotThrow(() -> utils.checkPolyspaceBinFolderExists(tempDir.toString()));
    }

    @Test
    void testCheckPolyspaceBinCommandExists_InvalidCommand() {
        // This path is unlikely to exist or be a file
        String invalidCommandPath = "this/path/should/not/exist/as/a/file/command/for/bin/command/check"; // Made path more specific
        PolyspaceConfigUtils utils = new PolyspaceConfigUtils(); // Use a local instance
        try {
            utils.checkPolyspaceBinCommandExists(invalidCommandPath);
            fail("FormValidation warning was expected for non-existent command file");
        } catch (FormValidation fv) {
            // First, check the message content
            assertEquals(Messages.polyspaceBinNotValid(), fv.renderHtml(),
                    "FormValidation message for non-existent command file does not match.");
            // Then, check the kind
            assertEquals(FormValidation.Kind.WARNING, fv.kind,
                    "FormValidation kind for non-existent command file should be WARNING.");
        }
    }

    @Test
    void testCheckPolyspaceBinCommandExists_ValidCommand(@TempDir Path tempDir) throws java.io.IOException {
        PolyspaceConfigUtils utils = new PolyspaceConfigUtils();
        Path tempFile = Files.createFile(tempDir.resolve("testCommand.exe"));
        // tempFile provides a path to an existing file
        assertDoesNotThrow(() -> utils.checkPolyspaceBinCommandExists(tempFile.toString()));
    }

    @Test
    void testCheckPolyspaceAccess_SuccessfulCommand(@TempDir Path tempDir) throws java.io.IOException {
        Path fakeBinDir = tempDir.resolve("polyspace_bin");
        Files.createDirectories(fakeBinDir);
        String exeSuffix = polyspaceConfigUtilsSpy.exeSuffix();
        Path fakeCmd = fakeBinDir.resolve("polyspace-access" + exeSuffix);
        Files.createFile(fakeCmd);

        doReturn(true).when(polyspaceConfigUtilsSpy).checkPolyspaceCommand(anyList());

        FormValidation result = polyspaceConfigUtilsSpy.checkPolyspaceAccess(
                fakeBinDir.toString(), "testUser", "testPass", "http", "testHost", "1234");

        assertEquals(FormValidation.Kind.OK, result.kind, "FormValidation kind should be OK for successful command.");
        verify(polyspaceConfigUtilsSpy).checkPolyspaceCommand(anyList());
    }

    @Test
    void testCheckPolyspaceAccess_FailedCommand(@TempDir Path tempDir) throws java.io.IOException {
        Path fakeBinDir = tempDir.resolve("polyspace_bin_fail");
        Files.createDirectories(fakeBinDir);
        String exeSuffix = polyspaceConfigUtilsSpy.exeSuffix();
        Path fakeCmd = fakeBinDir.resolve("polyspace-access" + exeSuffix);
        Files.createFile(fakeCmd);

        doReturn(false).when(polyspaceConfigUtilsSpy).checkPolyspaceCommand(anyList());

        FormValidation result = polyspaceConfigUtilsSpy.checkPolyspaceAccess(
                fakeBinDir.toString(), "testUser", "testPass", "http", "testHost", "1234");

        assertEquals(FormValidation.Kind.ERROR, result.kind, "FormValidation kind should be ERROR for failed command.");
        verify(polyspaceConfigUtilsSpy).checkPolyspaceCommand(anyList());
    }

    @Test
    void testCheckPolyspaceAccess_BinFolderNotFound() {
        PolyspaceConfigUtils utils = new PolyspaceConfigUtils();
        String nonExistentPath = "this/path/絶対に/存在しない/folder/for/access/check"; // Unique path

        FormValidation result = utils.checkPolyspaceAccess(
                nonExistentPath, "testUser", "testPass", "http", "testHost", "1234");

        assertEquals(Messages.polyspaceBinNotFound(), result.renderHtml(),
                "FormValidation message for bin folder not found in checkPolyspaceAccess does not match.");
        assertEquals(FormValidation.Kind.WARNING, result.kind,
                "FormValidation kind should be WARNING for non-existent bin folder in checkPolyspaceAccess.");
    }

    @Test
    void testCheckPolyspaceAccess_BinCommandNotFound(@TempDir Path tempDir) throws java.io.IOException {
        PolyspaceConfigUtils utils = new PolyspaceConfigUtils();
        Path fakeBinDir = tempDir.resolve("polyspace_bin_no_cmd_for_access_check"); // Unique path
        Files.createDirectories(fakeBinDir);
        // The command "polyspace-access" will not be created in this directory

        FormValidation result = utils.checkPolyspaceAccess(
                fakeBinDir.toString(), "testUser", "testPass", "http", "testHost", "1234");

        assertEquals(Messages.polyspaceBinNotValid(), result.renderHtml(),
                "FormValidation message for bin command not valid in checkPolyspaceAccess does not match.");
        assertEquals(FormValidation.Kind.WARNING, result.kind,
                "FormValidation kind should be WARNING for non-existent command file in checkPolyspaceAccess.");
    }

    @Test
    void testCheckPolyspaceAccess_CommandConstruction_AllParams(@TempDir Path tempDir) throws java.io.IOException {
        Path fakeBinDir = tempDir.resolve("polyspace_bin_construct_all");
        Files.createDirectories(fakeBinDir);
        String exeSuffix = polyspaceConfigUtilsSpy.exeSuffix();
        Path fakeCmd = fakeBinDir.resolve("polyspace-access" + exeSuffix);
        Files.createFile(fakeCmd);

        doReturn(true).when(polyspaceConfigUtilsSpy).checkPolyspaceCommand(anyList());
        ArgumentCaptor<List<String>> commandCaptor = ArgumentCaptor.forClass(List.class);

        polyspaceConfigUtilsSpy.checkPolyspaceAccess(fakeBinDir.toString(), "myUser", "myPass", "https", "myHost", "8080");

        verify(polyspaceConfigUtilsSpy).checkPolyspaceCommand(commandCaptor.capture());
        List<String> capturedCommand = commandCaptor.getValue();

        List<String> expectedCommandParts = Arrays.asList(
                fakeCmd.toString(),
                "-login", "myUser",
                "-encrypted-password", "myPass",
                "-protocol", "https",
                "-host", "myHost",
                "-port", "8080",
                "-list-project"
        );
        assertTrue(capturedCommand.containsAll(expectedCommandParts) && expectedCommandParts.size() == capturedCommand.size(),
                "Command construction (all params) failed. Expected: " + expectedCommandParts + " Got: " + capturedCommand);
    }

    @Test
    void testCheckPolyspaceAccess_CommandConstruction_MinimalParams(@TempDir Path tempDir) throws java.io.IOException {
        Path fakeBinDir = tempDir.resolve("polyspace_bin_construct_min");
        Files.createDirectories(fakeBinDir);
        String exeSuffix = polyspaceConfigUtilsSpy.exeSuffix();
        Path fakeCmd = fakeBinDir.resolve("polyspace-access" + exeSuffix);
        Files.createFile(fakeCmd);

        doReturn(true).when(polyspaceConfigUtilsSpy).checkPolyspaceCommand(anyList());
        ArgumentCaptor<List<String>> commandCaptor = ArgumentCaptor.forClass(List.class);

        polyspaceConfigUtilsSpy.checkPolyspaceAccess(fakeBinDir.toString(), "minUser", "minPass", "", "", "");

        verify(polyspaceConfigUtilsSpy).checkPolyspaceCommand(commandCaptor.capture());
        List<String> capturedCommand = commandCaptor.getValue();

        List<String> expectedCommandParts = Arrays.asList(
                fakeCmd.toString(),
                "-login", "minUser",
                "-encrypted-password", "minPass",
                "-list-project"
        );
        assertTrue(capturedCommand.containsAll(expectedCommandParts) && expectedCommandParts.size() == capturedCommand.size(),
                "Command construction (min params) failed. Expected: " + expectedCommandParts + " Got: " + capturedCommand);
        assertFalse(capturedCommand.contains("-protocol"), "Command should not contain -protocol for minimal params.");
        assertFalse(capturedCommand.contains("-host"), "Command should not contain -host for minimal params.");
        assertFalse(capturedCommand.contains("-port"), "Command should not contain -port for minimal params.");
    }

    @Test
    void testCheckPolyspaceCommand_SuccessfulExecution() {
        PolyspaceConfigUtils utils = new PolyspaceConfigUtils();
        List<String> successfulCommand;
        // Determine OS and use a command that is known to succeed and return 0
        if (hudson.Functions.isWindows()) {
            successfulCommand = Arrays.asList("cmd.exe", "/c", "echo success");
        } else {
            // 'true' command exists on Unix-like systems and its purpose is to return 0
            successfulCommand = Arrays.asList("true");
        }
        assertTrue(utils.checkPolyspaceCommand(successfulCommand),
                "checkPolyspaceCommand should return true for a known successful command: " + String.join(" ", successfulCommand));
    }

    @Test
    void testCheckPolyspaceCommand_FailedExecution() {
        PolyspaceConfigUtils utils = new PolyspaceConfigUtils();
        List<String> failedCommand;
        // Determine OS and use a command that is known to fail and return non-zero
        if (hudson.Functions.isWindows()) {
            // 'exit 1' makes cmd.exe return 1
            failedCommand = Arrays.asList("cmd.exe", "/c", "exit 1");
        } else {
            // 'false' command exists on Unix-like systems and its purpose is to return 1
            failedCommand = Arrays.asList("false");
        }
        assertFalse(utils.checkPolyspaceCommand(failedCommand),
                "checkPolyspaceCommand should return false for a known failing command: " + String.join(" ", failedCommand));
    }

    @Test
    void testCheckPolyspaceCommand_CommandNotFound() {
        PolyspaceConfigUtils utils = new PolyspaceConfigUtils();
        // This command should ideally not exist on any system.
        List<String> nonExistentCommand = Arrays.asList("aCommandThatTrulyShouldNotExist123xyz");

        // ProcessBuilder.start() for a non-existent command typically throws IOException.
        // The checkPolyspaceCommand method catches IOException and returns false.
        assertFalse(utils.checkPolyspaceCommand(nonExistentCommand),
                "checkPolyspaceCommand should return false when the command is not found (expecting IOException to be caught).");
    }
}
