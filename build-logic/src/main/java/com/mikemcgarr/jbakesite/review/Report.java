package com.mikemcgarr.jbakesite.review;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The review's output: each finding with its fix, wrapped at 110 columns, then the one-line summary; and, when
 * running in GitHub Actions ({@code GITHUB_ACTIONS=true}), a workflow annotation per finding and the report
 * appended to the job's step summary ({@code GITHUB_STEP_SUMMARY}).
 */
final class Report {

    private final PrintStream out;
    private final Map<String, String> env;

    /**
     * Creates the report writer.
     *
     * @param out where the report goes
     * @param env the environment, read for {@code GITHUB_ACTIONS} and {@code GITHUB_STEP_SUMMARY}
     */
    Report(PrintStream out, Map<String, String> env) {
        this.out = out;
        this.env = env;
    }

    /**
     * Prints the report (nothing for a review with nothing to review, which announced itself).
     *
     * @param result what the review found
     * @throws IOException if the step summary can't be written
     */
    void print(ReviewResult result) throws IOException {
        if (result.nothingToReview()) {
            return;
        }
        String text = text(result);
        out.print(text);
        out.flush();
        if ("true".equals(env.get("GITHUB_ACTIONS"))) {
            for (String annotation : annotations(result)) {
                out.println(annotation);
            }
            String stepSummary = env.get("GITHUB_STEP_SUMMARY");
            if (stepSummary != null && !stepSummary.isEmpty()) {
                Files.writeString(Path.of(stepSummary), "### Review of the changed pages\n\n```\n" + text + "```\n",
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        }
    }

    /** The findings and the summary line, as printed. */
    static String text(ReviewResult result) {
        StringBuilder sb = new StringBuilder();
        for (Finding f : result.findings()) {
            sb.append(f.error() ? "ERROR " : "WARN  ").append(f.id()).append(' ').append(f.file());
            if (f.line() > 0) {
                sb.append(':').append(f.line());
            }
            sb.append('\n').append(wrap(f.message())).append('\n');
            if (f.fix() != null) {
                sb.append("  Fix: ").append(f.fix()).append('\n');
            }
            sb.append('\n');
        }
        return sb.append(summary(result)).append('\n').toString();
    }

    /** The report's last line: what was reviewed and the counts, without a line break. */
    static String summary(ReviewResult result) {
        List<String> reviewed = result.reviewedPages();
        String pages = reviewed.isEmpty() ? "no pages" : reviewed.size() + (reviewed.size() == 1 ? " page (" : " pages (")
                + String.join(", ", reviewed.size() > 6 ? List.of(String.join(", ", reviewed.subList(0, 6)), "…") : reviewed) + ")";
        long errors = result.errors();
        long warnings = result.warnings();
        int changedFiles = result.changedFiles();
        return "Reviewed " + pages
                + (result.all() ? "" : " from " + changedFiles + " changed file" + (changedFiles == 1 ? "" : "s"))
                + ". " + errors + " error" + (errors == 1 ? "" : "s")
                + ", " + warnings + " warning" + (warnings == 1 ? "" : "s")
                + (result.suppressed() > 0 ? ", " + result.suppressed() + " suppressed" : "")
                + ". External links not checked (T119).";
    }

    /** GitHub Actions workflow commands, one per finding, so the findings show on the PR's files. */
    static List<String> annotations(ReviewResult result) {
        List<String> lines = new ArrayList<>();
        for (Finding f : result.findings()) {
            lines.add("::" + (f.error() ? "error" : "warning") + " file=" + f.file() + (f.line() > 0 ? ",line=" + f.line() : "")
                    + ",title=" + f.id() + "::" + escape(f.message() + (f.fix() != null ? " Fix: " + f.fix() : "")));
        }
        return lines;
    }

    static String wrap(String message) {
        StringBuilder sb = new StringBuilder("  ");
        int col = 2;
        for (String word : message.split(" ")) {
            if (col + word.length() > 110 && col > 2) {
                sb.append("\n  ");
                col = 2;
            }
            sb.append(word).append(' ');
            col += word.length() + 1;
        }
        return sb.toString().stripTrailing();
    }

    static String shorten(String s) {
        s = s.replaceAll("\\s+", " ");
        return s.length() > 80 ? s.substring(0, 77) + "…" : s;
    }

    private static String escape(String s) {
        return s.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A");
    }
}
