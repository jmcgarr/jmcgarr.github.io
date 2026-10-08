// Starts a new draft post (revival tasks T060 and T061, issue #8). Run it through Gradle:
//
//   ./gradlew newPost -Ptitle="My Post Title" [-Ptags="management, leadership"] [-Psummary="A teaser."]
//                     [-Pformat=md|asciidoc] [-Pslug=my-post] [-Pbranch=false] [-Ppr]
//
// It creates src/jbake/content/blog/drafts/<slug>.md (or .asciidoc) with the JBake header filled in and
// status=draft, on a new branch post/<slug> made from main. Drafts render for local preview only and are never
// published (T009). It refuses to reuse a slug that exists as a draft, a post, or a published URL, because
// published URLs are permanent (AGENTS.md, Rule 1).
//
// With -Ppr (T061) it also commits the draft, pushes post/<slug> to origin, and opens a draft pull request
// titled "[WIP] <title>" with the GitHub CLI (gh). Pull requests never deploy (T008), so this publishes nothing.
//
// A single-file Java program (JDK 21): Gradle runs it with `java scripts/NewPost.java`.

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
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
        boolean pr = switch (opts.getOrDefault("pr", "false").strip().toLowerCase()) {
            case "", "true" -> true;
            case "false" -> false;
            default -> {
                fail("-Ppr takes no value (or true/false), not '" + opts.get("pr") + "'.");
                yield false;
            }
        };
        if (pr && !branch) {
            fail("-Ppr opens a pull request from the post/<slug> branch, so it can't be combined with "
                    + "-Pbranch=false.");
        }

        if (!Files.isDirectory(DRAFTS)) {
            fail("Run this from the repository root (no " + DRAFTS + " here).");
        }
        refuseExistingSlug(slug);
        warnAboutTagSpellings(tags);

        String branchName = "post/" + slug;
        String repo = pr ? checkReadyToOpenPr() : null;
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
        String prUrl = pr ? commitPushAndOpenPr(file, title, slug, branchName, repo) : null;

        System.out.println();
        System.out.println("Next:");
        System.out.println("  Write the post, then preview it:  ./gradlew bakePreview");
        System.out.println("    http://localhost:8080/blog/drafts/" + slug + "-draft.html");
        if (summary.isEmpty()) {
            System.out.println("  Fill in summary= (optional): it's the teaser under the title on the home page.");
        }
        if (prUrl != null) {
            System.out.println("  Commit and push to " + branchName + " as you go; the draft PR picks it up.");
            System.out.println("  To publish: set status=published, move the file to " + BLOG + "/, drop [WIP] from");
            System.out.println("    the PR title, mark it ready (gh pr ready), and merge.");
        } else {
            System.out.println("  To publish: set status=published, move the file to " + BLOG + "/, and open a PR.");
        }
        System.out.println("    It will live at https://www.mikemcgarr.com/blog/" + slug + ".html (permanent).");
    }

    /**
     * Checks everything -Ppr needs before anything is created: gh is installed and logged in, it can find the
     * GitHub repository, there's an origin to push to, git knows who is committing, and main has nothing
     * unpushed (it would ride along in the post's PR). Returns the repository as owner/name.
     */
    static String checkReadyToOpenPr() throws IOException, InterruptedException {
        Result version = run(List.of("gh", "--version"));
        if (version == null) {
            fail("-Ppr needs the GitHub CLI (gh), and it isn't installed or isn't on the PATH. "
                    + "Install it (https://cli.github.com), run gh auth login, then try again.");
        }
        Result auth = run(List.of("gh", "auth", "status"));
        if (auth.exit() != 0) {
            fail("-Ppr needs the GitHub CLI to be logged in, and gh auth status says it isn't:\n"
                    + indent(auth.output()) + "\nRun gh auth login, then try again.");
        }
        if (git("remote", "get-url", "origin").isEmpty()) {
            fail("-Ppr pushes the post branch to origin, and this repository has no origin remote.");
        }
        Result repo = run(List.of("gh", "repo", "view", "--json", "nameWithOwner", "--jq", ".nameWithOwner"));
        if (repo.exit() != 0 || repo.output().isEmpty()) {
            fail("-Ppr couldn't find the GitHub repository with gh repo view:\n" + indent(repo.output()));
        }
        if (git("var", "GIT_COMMITTER_IDENT").isEmpty()) {
            fail("-Ppr commits the draft, and git doesn't know your name and email. Set them with "
                    + "git config user.name and git config user.email, then try again.");
        }
        String unpushed = git("rev-list", "--count", "refs/remotes/origin/main..refs/heads/main");
        if (!unpushed.isEmpty() && !unpushed.equals("0")) {
            fail("Your local main has " + unpushed + " commit(s) that aren't on origin/main. They would end up "
                    + "in the post's pull request. Push or reset main first.");
        }
        return repo.output();
    }

    /**
     * Commits the new draft, pushes the branch, and opens a draft PR. If a step fails, it says what happened and
     * what to run next, and exits without undoing anything. Returns the PR's URL.
     */
    static String commitPushAndOpenPr(Path file, String title, String slug, String branchName, String repo)
            throws IOException, InterruptedException {
        String prTitle = "[WIP] " + title;
        Path body = Files.createTempFile("newpost-pr-body-", ".md");
        Files.writeString(body, String.join("\n",
                "Draft post: **" + title + "**",
                "",
                "- File: `" + file + "` (`status=draft`)",
                "- Preview: `./gradlew bakePreview`, then http://localhost:8080/blog/drafts/" + slug + "-draft.html",
                "- Once published, it will live at https://www.mikemcgarr.com/blog/" + slug + ".html",
                "",
                "Work in progress, started with `./gradlew newPost -Ppr`. Drafts are never deployed, and pull "
                        + "requests never deploy, so nothing here is live yet. To publish: set `status=published`, "
                        + "move the file to `" + BLOG + "/`, drop `[WIP]` from the title, mark the PR ready for "
                        + "review, and merge.",
                ""), StandardCharsets.UTF_8);
        List<String> push = List.of("git", "push", "--set-upstream", "origin", branchName);
        List<String> openPr = List.of("gh", "pr", "create", "--draft", "--repo", repo, "--base", "main",
                "--head", branchName, "--title", prTitle, "--body-file", body.toString());

        List<String> add = List.of("git", "add", "--", file.toString());
        List<String> commit = List.of("git", "commit", "--quiet",
                "-m", "Starting a draft post: " + title,
                "-m", "Created with ./gradlew newPost -Ppr. It has status=draft, so it renders for local preview "
                        + "only and isn't published.");
        if (!runVisibly(add) || !runVisibly(commit)) {
            stopAfter("Committing the draft failed (see the git output above). The draft file is in place on "
                    + branchName + ", uncommitted.", List.of(add, commit, push, openPr));
        }
        System.out.println("Committed " + file + " on " + branchName);

        if (!runVisibly(push)) {
            stopAfter("The draft is committed on " + branchName + ", but pushing it to origin failed "
                    + "(see the git output above).", List.of(push, openPr));
        }
        System.out.println("Pushed " + branchName + " to origin");

        Result created = run(openPr, Redirect.INHERIT);
        if (created == null || created.exit() != 0) {
            stopAfter(branchName + " is committed and pushed, but opening the pull request failed"
                    + (created == null ? " (gh didn't run)." : " (see the gh output above)."), List.of(openPr));
        }
        Files.deleteIfExists(body);
        String url = created.output().lines().filter(l -> l.startsWith("https://")).reduce((a, b) -> b)
                .orElse(created.output());
        System.out.println("Opened the draft pull request \"" + prTitle + "\": " + url);
        return url;
    }

    /** Explains a failed step and lists the commands that finish the job, then exits. Nothing is undone. */
    static void stopAfter(String what, List<List<String>> remaining) {
        StringBuilder msg = new StringBuilder(what).append(" Nothing was undone. To finish, run:");
        for (List<String> cmd : remaining) {
            msg.append("\n  ").append(String.join(" ", cmd.stream().map(NewPost::shellQuote).toList()));
        }
        fail(msg.toString());
    }

    static String shellQuote(String s) {
        return s.matches("[A-Za-z0-9_./:=@+,-]+") ? s : "'" + s.replace("'", "'\\''") + "'";
    }

    static String indent(String text) {
        return text.lines().map(l -> "  " + l).reduce((a, b) -> a + "\n" + b).orElse("  (no output)");
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
            if (!arg.startsWith("--") || arg.length() == 2) {
                fail("Unexpected argument '" + arg + "'. Expected --name=value.");
            }
            int eq = arg.indexOf('=');
            if (eq < 0) {
                opts.put(arg.substring(2), "true");   // a bare flag such as --pr
            } else {
                opts.put(arg.substring(2, eq), arg.substring(eq + 1));
            }
        }
        return opts;
    }

    record Result(int exit, String output) {}

    /** Runs a command and captures its trimmed output (stdout and stderr), or returns null if it can't start. */
    static Result run(List<String> cmd) throws InterruptedException {
        return run(cmd, null);
    }

    /** As run(cmd), but with stderr sent to {@code stderr} instead of captured, when it isn't null. */
    static Result run(List<String> cmd, Redirect stderr) throws InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd).redirectInput(Redirect.from(nullDevice()));
        pb.environment().put("GH_PROMPT_DISABLED", "1");
        if (stderr == null) {
            pb.redirectErrorStream(true);
        } else {
            pb.redirectError(stderr);
        }
        try {
            Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
            return new Result(p.waitFor(), out);
        } catch (IOException e) {
            return null;
        }
    }

    /** Runs a command with its output shown to the user. Returns whether it succeeded. */
    static boolean runVisibly(List<String> cmd) throws InterruptedException {
        try {
            return new ProcessBuilder(cmd).inheritIO().start().waitFor() == 0;
        } catch (IOException e) {
            System.err.println("newPost: couldn't run " + cmd.get(0) + ": " + e.getMessage());
            return false;
        }
    }

    static java.io.File nullDevice() {
        return new java.io.File(System.getProperty("os.name").startsWith("Windows") ? "NUL" : "/dev/null");
    }

    /** Runs git and returns its trimmed output, or "" if it exits non-zero. */
    static String git(String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of("git"));
        cmd.addAll(List.of(args));
        Result r = run(cmd);
        if (r == null) {
            throw new IOException("git isn't installed or isn't on the PATH.");
        }
        return r.exit() == 0 ? r.output() : "";
    }

    static void gitOrFail(String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of("git"));
        cmd.addAll(List.of(args));
        if (!runVisibly(cmd)) {
            fail("git " + String.join(" ", args) + " failed.");
        }
    }

    static void fail(String message) {
        System.err.println("newPost: " + message);
        System.exit(1);
    }
}
