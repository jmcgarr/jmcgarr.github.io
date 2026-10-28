package com.mikemcgarr.jbakesite.writing;

/**
 * What {@code ./gradlew openPr} is given, each as the raw {@code -P} value or null when the property wasn't set.
 * {@link OpenPr} interprets them.
 *
 * @param title the pull request's title; default the subject of the branch's first commit
 * @param message the commit message for the uncommitted work; without it, that work is listed and nothing happens
 * @param closes issue numbers the pull request closes, comma-separated ({@code 3,16} or {@code #3}), each a
 *        {@code Closes #N} line of the description
 * @param refs issue numbers the pull request contributes to, comma-separated, each a {@code Refs #N} line
 * @param dryRun whether to do everything but commit, push and open the pull request
 */
record OpenPrOptions(String title, String message, String closes, String refs, boolean dryRun) {

    /** No options: the branch's commits, as they are. */
    static final OpenPrOptions NONE = new OpenPrOptions(null, null, null, null, false);
}
