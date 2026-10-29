package com.mikemcgarr.jbakesite.review;

import com.mikemcgarr.jbakesite.FileTrees;
import com.mikemcgarr.jbakesite.internal.Git;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.internal.ToolFailure;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * What a branch changed: every file under {@code src/jbake} and {@code src/case-redirects} that differs from the
 * merge base with the base branch (committed or not), plus the untracked files there; or, for {@code -Pall}, every
 * content file as if modified.
 */
final class Changes {

    private final SiteLayout site;
    private final Git git;
    private final String base;
    private String mergeBase;

    Changes(SiteLayout site, Git git, String base) {
        this.site = site;
        this.git = git;
        this.base = base;
    }

    String base() {
        return base;
    }

    /** The files that differ from the merge base, and the untracked ones. */
    List<Change> changed() throws IOException, InterruptedException {
        if (git.output("rev-parse", "--is-inside-work-tree").isEmpty()) {
            throw ToolFailure.couldNotRun("Not a git checkout, so there's no change to review. Use -Pall to review every page.");
        }
        if (git.output("rev-parse", "--verify", "--quiet", base + "^{commit}").isEmpty()) {
            throw ToolFailure.couldNotRun("The base '" + base + "' isn't a commit here. Pass -Pbase=<ref> (for example -Pbase=origin/main),"
                    + " or fetch it first.");
        }
        String merge = mergeBase();
        List<Change> changes = new ArrayList<>();
        String content = site.display(site.source());
        String caseRedirects = site.display(site.caseRedirects());
        for (String line : git.output("diff", "--name-status", "-M", merge, "--", content, caseRedirects).split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\t");
            char status = parts[0].charAt(0);
            if (status == 'R' && parts.length >= 3) {
                changes.add(new Change('R', site.resolve(parts[2]), site.resolve(parts[1])));
            } else if (parts.length >= 2) {
                changes.add(new Change(status == 'C' ? 'A' : status, site.resolve(parts[1]), null));
            }
        }
        for (String line : git.output("ls-files", "--others", "--exclude-standard", "--", content, caseRedirects).split("\n")) {
            if (!line.isBlank()) {
                changes.add(new Change('U', site.resolve(line), null));
            }
        }
        return changes;
    }

    /** {@code -Pall}: every content file, as if modified (so nothing is "new" and the diff-only warnings stay quiet). */
    List<Change> everything() throws IOException {
        List<Change> changes = new ArrayList<>();
        for (Path p : FileTrees.regularFilesUnder(site.content())) {
            if (p.toString().endsWith(".md")) {
                changes.add(new Change('M', p, null));
            }
        }
        return changes;
    }

    /** The merge base of the base branch and HEAD. */
    String mergeBase() throws IOException, InterruptedException {
        if (mergeBase == null) {
            mergeBase = git.output("merge-base", base, "HEAD");
            if (mergeBase.isEmpty()) {
                throw ToolFailure.couldNotRun("No merge base between " + base + " and HEAD (a shallow clone? fetch with more history).");
            }
        }
        return mergeBase;
    }

    /** A file's text at the merge base, or "" if it wasn't there. */
    String atMergeBase(Path path) throws IOException, InterruptedException {
        return git.output("show", mergeBase() + ":" + site.display(path));
    }
}
