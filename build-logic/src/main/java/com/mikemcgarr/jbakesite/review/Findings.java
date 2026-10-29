package com.mikemcgarr.jbakesite.review;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** The findings of one review, without duplicates, and how many a {@code review: ignore} comment suppressed. */
final class Findings {

    private final List<Finding> findings = new ArrayList<>();
    private int suppressed;

    /** Records a finding, once. */
    void add(boolean error, String id, String file, int line, String message, String fix) {
        Finding f = new Finding(error, id, file.replace('\\', '/'), line, message, fix);
        if (!findings.contains(f)) {
            findings.add(f);
        }
    }

    /** Records a finding unless its code is among the ones a comment on the line before told the review to ignore. */
    void unlessIgnored(Set<String> ignore, boolean error, String id, String file, int line, String message, String fix) {
        if (ignore.contains(id)) {
            suppressed++;
            return;
        }
        add(error, id, file, line, message, fix);
    }

    /** The findings: errors first, then by file, line and code. */
    List<Finding> sorted() {
        List<Finding> sorted = new ArrayList<>(findings);
        sorted.sort(Comparator.comparing((Finding f) -> !f.error()).thenComparing(Finding::file)
                .thenComparingInt(Finding::line).thenComparing(Finding::id));
        return sorted;
    }

    int suppressed() {
        return suppressed;
    }
}
