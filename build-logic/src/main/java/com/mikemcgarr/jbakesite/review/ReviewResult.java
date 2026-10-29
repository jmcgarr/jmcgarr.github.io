package com.mikemcgarr.jbakesite.review;

import java.util.List;

/**
 * What a review found. {@link #nothingToReview()} is the clean-tree case, already announced; the rest is what
 * {@link Report} prints.
 *
 * @param findings the findings, errors first
 * @param suppressed how many findings a {@code review: ignore} comment suppressed
 * @param reviewedPages the baked pages that were checked, in order
 * @param changedFiles how many files the branch changed (every content file under {@code -Pall}, which the summary leaves out)
 * @param all whether every page was reviewed rather than the changed ones
 * @param nothingToReview whether there was no change to review
 */
record ReviewResult(List<Finding> findings, int suppressed, List<String> reviewedPages, int changedFiles, boolean all,
        boolean nothingToReview) {

    static ReviewResult nothing() {
        return new ReviewResult(List.of(), 0, List.of(), 0, false, true);
    }

    /**
     * How many errors were found.
     *
     * @return the count
     */
    long errors() {
        return findings.stream().filter(Finding::error).count();
    }

    /**
     * How many warnings were found.
     *
     * @return the count
     */
    long warnings() {
        return findings.size() - errors();
    }

    /**
     * Whether the review fails: errors, or warnings under {@code -Pstrict}.
     *
     * @param strict whether warnings fail the review too
     * @return true when the review failed
     */
    boolean failed(boolean strict) {
        return errors() > 0 || (strict && warnings() > 0);
    }
}
