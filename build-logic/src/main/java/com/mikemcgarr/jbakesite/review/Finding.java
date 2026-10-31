package com.mikemcgarr.jbakesite.review;

/**
 * One thing to fix or look at (docs/archive/05-REVIEW-FLOW.md, T117).
 *
 * @param error an error (E1 to E5), which fails the review, rather than a warning (W2 to W7), which is listed
 * @param id the check's code, such as {@code E1}
 * @param file the file concerned, repository-relative with forward slashes
 * @param line the line, or 0 when unknown
 * @param message what is wrong
 * @param fix what to write instead, or null
 */
record Finding(boolean error, String id, String file, int line, String message, String fix) {
}
