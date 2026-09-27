package com.codenoir.demo.service;

import com.codenoir.demo.model.OnboardingReport;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class AnalysisService {

	private static final int TIMEOUT_SECONDS = 180;

	private static final String REPORT_FILENAME = "codenoir-report.json";

	private static final String PROMPT =
        "Analyze this repository for developer onboarding. " +
        "You MUST create codenoir-report.json in the current repository root and do not modify any other file. " +
        "The file MUST be valid JSON with exactly these top-level fields: project, importantFolders, importantFiles, observations, readingOrder, starterTask, details. " +
        "project MUST contain name, summary, primaryLanguage, frameworks, buildTool, architecture. " +
        "importantFolders MUST be an array of objects containing path and purpose. " +
        "importantFiles MUST be an array of objects containing path, role and purpose. " +
        "observations MUST be an array of objects containing severity, title, message and file; never use plain strings. " +
        "readingOrder MUST be an array of strings. " +
        "details MUST be an object containing any stack-specific metadata you discover. " +
        "Never rename or omit the required fields. Use empty strings, arrays or objects when a value cannot be determined. " +
        "Do not return the analysis as prose; write the completed analysis to codenoir-report.json.";

	@Value("${bob.apiKey:}")
	private String bobApiKey;

	private final ObjectMapper objectMapper = new ObjectMapper();

	// -------------------------------------------------------------------------
	// ZIP extraction
	// -------------------------------------------------------------------------

	/**
	 * Extracts a ZIP upload into an isolated temporary directory.
	 *
	 * @param zip the uploaded ZIP file
	 * @return an {@link ExtractionResult} describing what was extracted
	 * @throws IOException if the file cannot be read or written
	 */
	public ExtractionResult extract(MultipartFile zip) throws IOException {
		Path targetDir = Path.of(System.getProperty("java.io.tmpdir"), "codenoir", UUID.randomUUID().toString());
		Files.createDirectories(targetDir);

		int fileCount = 0;
		String topLevelFolder = null;

		try (InputStream is = zip.getInputStream();
			 ZipInputStream zis = new ZipInputStream(is)) {

			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				Path resolved = targetDir.resolve(entry.getName()).normalize();

				// Zip Slip guard: reject any entry that escapes the target directory
				if (!resolved.startsWith(targetDir)) {
					zis.closeEntry();
					continue;
				}

				if (entry.isDirectory()) {
					Files.createDirectories(resolved);

					// Track the top-level folder name (first directory at depth 1)
					if (topLevelFolder == null) {
						Path relative = targetDir.relativize(resolved);
						if (relative.getNameCount() == 1) {
							topLevelFolder = relative.toString();
						}
					}
				} else {
					Files.createDirectories(resolved.getParent());
					Files.copy(zis, resolved);
					fileCount++;
				}

				zis.closeEntry();
			}
		}

		String projectName = (topLevelFolder != null) ? topLevelFolder : targetDir.getFileName().toString();
		return new ExtractionResult(targetDir, fileCount, projectName);
	}

	// -------------------------------------------------------------------------
	// Bob Shell invocation
	// -------------------------------------------------------------------------

	/**
	 * Runs Bob Shell in non-interactive mode against the given project directory.
	 * Bob is instructed to write codenoir-report.json into that directory.
	 * Stdout/stderr are captured to a temp file for debug logging only.
	 *
	 * @param projectDir the extracted project root Bob should analyse
	 * @throws AnalysisException if Bob exits non-zero, times out, or cannot be started
	 */
	public void analyze(Path projectDir) {
		// On Windows, bob is a .cmd script — use "cmd /c" so the shell resolves it.
		List<String> command = List.of("cmd", "/c", "bob", "run", PROMPT);

		// Write Bob's output to a temp file — avoids both pipe-buffer deadlock and
		// the loss of output that comes with inheritIO().
		Path outputFile;
		try {
			outputFile = Files.createTempFile("codenoir-bob-", ".txt");
		} catch (IOException e) {
			throw new AnalysisException("Could not create temp output file: " + e.getMessage());
		}

		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(projectDir.toFile());
		pb.redirectErrorStream(true);                          // merge stderr into stdout
		pb.redirectOutput(outputFile.toFile());                // write merged stream to file
		pb.redirectInput(ProcessBuilder.Redirect.INHERIT);     // inherit stdin so Bob can use the terminal

		if (!bobApiKey.isBlank()) {
			pb.environment().put("BOB_API_KEY", bobApiKey);
		}

		System.out.println("[CodeNoir] Starting Bob Shell in: " + projectDir);
		System.out.println("[CodeNoir] BOB_API_KEY present in env: " +
				pb.environment().containsKey("BOB_API_KEY"));
		System.out.println("[CodeNoir] Output file: " + outputFile);

		Process process;
		try {
			process = pb.start();
		} catch (IOException e) {
			throw new AnalysisException("Could not start Bob Shell. Is 'bob' on PATH? " + e.getMessage());
		}


		System.out.println("[CodeNoir] Bob process started (pid=" + process.pid() + ")");

		boolean finished;
		try {
			finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			process.destroyForcibly();
			System.out.println("[CodeNoir] Bob Shell interrupted");
			throw new AnalysisException("Bob Shell was interrupted.");
		}

		if (!finished) {
			process.destroyForcibly();
			System.out.println("[CodeNoir] Bob Shell timed out after " + TIMEOUT_SECONDS + "s");
			throw new AnalysisException("Bob Shell timed out after " + TIMEOUT_SECONDS + " seconds.");
		}

		int exitCode = process.exitValue();
		System.out.println("[CodeNoir] Bob Shell finished with exit code " + exitCode);

		// Log stdout for debugging; do not parse it
		try {
			String debugOutput = Files.readString(outputFile);
			System.out.println("[CodeNoir] Bob stdout (debug):\n" + debugOutput);
		} catch (IOException ignored) {
		} finally {
			try { Files.deleteIfExists(outputFile); } catch (IOException ignored) {}
		}

		if (exitCode != 0) {
			throw new AnalysisException("Bob Shell exited with code " + exitCode + ". Check the Spring Boot console for details.");
		}
	}

	// -------------------------------------------------------------------------
	// Report file reading
	// -------------------------------------------------------------------------

	/**
		* Reads codenoir-report.json from the project directory and deserialises it.
		*
		* @param projectDir directory where Bob was instructed to write the report
		* @return parsed {@link OnboardingReport}
		* @throws AnalysisException if the file is missing or contains invalid JSON
		*/
	public OnboardingReport readReport(Path projectDir) {
		Path reportFile = projectDir.resolve(REPORT_FILENAME);
		System.out.println("[CodeNoir] Reading report file: " + reportFile);

		if (!Files.exists(reportFile)) {
			throw new AnalysisException(
					"Bob did not create " + REPORT_FILENAME + " in the project directory. " +
					"Check the Spring Boot console for Bob's output.");
		}

		OnboardingReport report;

try {

    System.out.println("===== CODENOIR RAW REPORT =====");
    System.out.println(Files.readString(reportFile));
    System.out.println("===============================");

    report = objectMapper.readValue(
            reportFile.toFile(),
            OnboardingReport.class
    );

} catch (Exception e) {

    throw new AnalysisException(
            "Failed to parse " + REPORT_FILENAME + ": " + e.getMessage()
    );
}

System.out.println("[CodeNoir] Parsed report:");

System.out.println(
        "  project.name            = " +
        (report.project() != null ? report.project().name() : "NULL")
);

System.out.println(
        "  project.primaryLanguage = " +
        (report.project() != null ? report.project().primaryLanguage() : "NULL")
);

System.out.println(
        "  project.frameworks      = " +
        (report.project() != null ? report.project().frameworks() : "NULL")
);

System.out.println(
        "  project.buildTool       = " +
        (report.project() != null ? report.project().buildTool() : "NULL")
);

System.out.println(
        "  observations.size       = " +
        (report.observations() != null ? report.observations().size() : "NULL")
);

System.out.println(
        "  importantFiles.size     = " +
        (report.importantFiles() != null ? report.importantFiles().size() : "NULL")
);

System.out.println(
        "  starterTask             = " +
        report.starterTask()
);
		return report;
	}

	// -------------------------------------------------------------------------
	// Value objects / exceptions
	// -------------------------------------------------------------------------

	/** Immutable value object carrying the outcome of a ZIP extraction. */
	public record ExtractionResult(Path directory, int fileCount, String projectName) {}

	/** Unchecked exception for Bob Shell failures. */
	public static class AnalysisException extends RuntimeException {
		public AnalysisException(String message) {
			super(message);
		}
	}
}
