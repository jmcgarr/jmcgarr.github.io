package com.mikemcgarr.jbakesite.review;

import com.mikemcgarr.jbakesite.internal.FrontMatter;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** W6: a deleted or renamed content file or asset removes a published URL (AGENTS.md Rule 1). */
final class RemovalChecks {

    private final SiteLayout site;
    private final Changes changes;
    private final Findings findings;

    RemovalChecks(SiteLayout site, Changes changes, Findings findings) {
        this.site = site;
        this.changes = changes;
        this.findings = findings;
    }

    void check(List<Change> changed) throws IOException, InterruptedException {
        for (Change c : changed) {
            if (c.status() != 'D' && c.status() != 'R') {
                continue;
            }
            Path old = c.removed();
            String what;
            if (old.startsWith(site.content())) {
                FrontMatter before = FrontMatter.parse(changes.atMergeBase(old));
                if (before.isDraft()) {
                    continue;   // a draft was never a URL
                }
                what = "/" + SiteLayout.relativePath(site.content(), old).replaceAll("\\.md$", ".html");
            } else if (old.startsWith(site.assets())) {
                what = "/" + SiteLayout.relativePath(site.assets(), old);
            } else {
                continue;
            }
            findings.add(false, "W6", site.display(old), 0, (c.status() == 'R' ? "Renaming" : "Deleting") + " this file removes the published URL "
                    + what + ". Published URLs are permanent (AGENTS.md Rule 1): this needs the owner's approval, a redirect stub at"
                    + " the old path, and a line in docs/baseline/expected-removals.txt. CI's check-urls.sh fails otherwise.", null);
        }
    }
}
