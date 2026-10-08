// Starts a new draft post (revival task T060, issue #8). Run it through Gradle:
//
//   ./gradlew newPost -Ptitle="My Post Title" [-Ptags="management, leadership"] [-Psummary="A teaser."]
//                     [-Pformat=md|asciidoc] [-Pslug=my-post] [-Pbranch=false]
//
// It creates src/jbake/content/blog/drafts/<slug>.md (or .asciidoc) with the JBake header filled in and
// status=draft, on a new branch post/<slug> made from main. Drafts render for local preview only and are never
// published (T009). It refuses to reuse a slug that exists as a draft, a post, or a published URL, because
// published URLs are permanent (AGENTS.md, Rule 1).
//
// A single-file Java program (JDK 21): Gradle runs it with `java scripts/NewPost.java`.

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

public class NewPost {

    static final Path BLOG = Path.of("src/jbake/content/blog");
    static final Path DRAFTS = BLOG.resolve("drafts");
    static final Path PUBLISHED_URLS = Path.of("docs/baseline/urls.txt");
    static final List<String> EXTENSIONS = List.of("md", "asciidoc", "adoc", "html");

    public static void main(String[] args) throws Exception {
        Map<String, String> opts = parse(args);
        String title = opts.getOrDefault("title", "").strip();
        if (title.isEmpty()) {
            fail("A title is required: ./gradlew newPost -Ptitle=\"My Post Title\"");
        }
        String format = opts.getOrDefault("format", "md");
        if (!format.equals("md") && !format.equals("asciidoc")) {
            fail("-Pformat must be md or asciidoc, not '" + format + "'.");
        }
        String slug = opts.containsKey("slug") ? opts.get("slug").strip() : slugify(title);
        if (!slug.matches("[a-z0-9]+(-[a-z0-9]+)*")) {
            fail("The slug '" + slug + "' isn't usable. Use lowercase letters, digits, and single hyphens, "
                    + "for example -Pslug=my-post.");
        }
        List<String> tags = Arrays.stream(opts.getOrDefault("tags", "").split(","))
                .map(String::strip).filter(t -> !t.isEmpty()).toList();
        String summary = opts.getOrDefault("summary", "").strip();
        boolean branch = !opts.getOrDefault("branch", "true").equalsIgnoreCase("false");

        if (!Files.isDirectory(DRAFTS)) {
            fail("Run this from the repository root (no " + DRAFTS + " here).");
        }
        refuseExistingSlug(slug);
        warnAboutTagSpellings(tags);

        String branchName = "post/" + slug;
        if (branch) {
            if (!git("rev-parse", "--verify", "--quiet", "refs/heads/" + branchName).isEmpty()
                    || !git("rev-parse", "--verify", "--quiet", "refs/remotes/origin/" + branchName).isEmpty()) {
                fail("The branch " + branchName + " already exists.");
            }
            if (!git("status", "--porcelain", "--untracked-files=no").isEmpty()) {
                fail("You have uncommitted changes. Commit or stash them first, so they don't end up on "
                        + branchName + " (or pass -Pbranch=false to stay on the current branch).");
            }
            if (git("rev-parse", "--verify", "--quiet", "refs/heads/main").isEmpty()) {
                fail("There's no local main branch to start " + branchName + " from.");
            }
            gitOrFail("switch", "--quiet", "--create", branchName, "main");
        }

        Path file = DRAFTS.resolve(slug + "." + format);
        String header = String.join("\n",
                "title=" + title,
                "date=" + LocalDate.now(),
                "type=post",
                "tags=" + String.join(", ", tags),
                "status=draft",
                "summary=" + summary,
                "~~~~~~",
                "",
                "");
        Files.writeString(file, header, StandardCharsets.UTF_8);

        System.out.println();
        System.out.println("Created " + file + " (status=draft, never published)");
        if (branch) {
            System.out.println("Switched to the new branch " + branchName + " (from main)");
        }
        System.out.println();
        System.out.println("Next:");
        System.out.println("  Write the post, then preview it:  ./gradlew bakePreview");
        System.out.println("    http://localhost:8080/blog/drafts/" + slug + "-draft.html");
        if (summary.isEmpty()) {
            System.out.println("  Fill in summary= (optional): it's the teaser under the title on the home page.");
        }
        System.out.println("  To publish: set status=published, move the file to " + BLOG + "/, and open a PR.");
        System.out.println("    It will live at https://www.mikemcgarr.com/blog/" + slug + ".html (permanent).");
    }

    /** "Boyd's Law: Iteration & Speed!" -> "boyds-law-iteration-speed". */
    static String slugify(String title) {
        String ascii = Normalizer.normalize(title, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return ascii.toLowerCase()
                .replaceAll("['’]", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }

    static void refuseExistingSlug(String slug) throws IOException {
        for (Path dir : List.of(BLOG, DRAFTS)) {
            for (String ext : EXTENSIONS) {
                Path existing = dir.resolve(slug + "." + ext);
                if (Files.exists(existing)) {
                    fail(existing + " already exists. Pick another title or pass -Pslug=<other-slug>.");
                }
            }
        }
        if (Files.exists(PUBLISHED_URLS)
                && Files.readAllLines(PUBLISHED_URLS).contains("blog/" + slug + ".html")) {
            fail("/blog/" + slug + ".html was already published (" + PUBLISHED_URLS + "). Published URLs are "
                    + "permanent, so pick another title or pass -Pslug=<other-slug>.");
        }
    }

    /** Tags that differ only by case make separate tag pages that each list only some posts (T055). */
    static void warnAboutTagSpellings(List<String> tags) throws IOException {
        if (tags.isEmpty()) {
            return;
        }
        Map<String, TreeSet<String>> spellings = new HashMap<>();
        try (Stream<Path> files = Files.list(BLOG)) {
            for (Path f : files.filter(Files::isRegularFile).toList()) {
                String text = Files.readString(f, StandardCharsets.UTF_8).replace("\r\n", "\n").replace('\r', '\n');
                for (String line : text.split("\n")) {
                    if (line.startsWith("~~~~~~")) {
                        break;
                    }
                    if (line.startsWith("tags=")) {
                        for (String t : line.substring(5).split(",")) {
                            if (!t.isBlank()) {
                                spellings.computeIfAbsent(t.strip().toLowerCase(), k -> new TreeSet<>()).add(t.strip());
                            }
                        }
                    }
                }
            }
        }
        for (String tag : tags) {
            TreeSet<String> existing = spellings.get(tag.toLowerCase());
            if (existing == null) {
                System.out.println("Note: '" + tag + "' is a new tag, so publishing creates /tags/" + tag + ".html.");
            } else if (!existing.contains(tag)) {
                System.out.println("WARNING: the tag '" + tag + "' is already used as " + existing + ". "
                        + "A different capitalization makes a separate tag page. Use the existing spelling.");
            }
        }
    }

    static Map<String, String> parse(String[] args) {
        Map<String, String> opts = new LinkedHashMap<>();
        for (String arg : args) {
            if (!arg.startsWith("--") || !arg.contains("=")) {
                fail("Unexpected argument '" + arg + "'. Expected --name=value.");
            }
            int eq = arg.indexOf('=');
            opts.put(arg.substring(2, eq), arg.substring(eq + 1));
        }
        return opts;
    }

    /** Runs git and returns its trimmed output, or "" if it exits non-zero. */
    static String git(String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of("git"));
        cmd.addAll(List.of(args));
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
        return p.waitFor() == 0 ? out : "";
    }

    static void gitOrFail(String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of("git"));
        cmd.addAll(List.of(args));
        Process p = new ProcessBuilder(cmd).inheritIO().start();
        if (p.waitFor() != 0) {
            fail("git " + String.join(" ", args) + " failed.");
        }
    }

    static void fail(String message) {
        System.err.println("newPost: " + message);
        System.exit(1);
    }
}
