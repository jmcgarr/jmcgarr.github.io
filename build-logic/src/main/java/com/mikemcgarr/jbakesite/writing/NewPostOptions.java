package com.mikemcgarr.jbakesite.writing;

/**
 * What {@code ./gradlew newPost} is given, each as the raw {@code -P} value or null when the property wasn't set
 * ({@code -Ppr} alone is ""). {@link NewPost} interprets them, with the program's rules: a missing title is an
 * error, {@code -Pbranch} and {@code -Ppr} take no value or true/false ({@code -Pbranch=false} stays on the
 * current branch), and
 * {@code -Pformat} is refused since every post is Markdown (T102).
 *
 * @param title the post's title; required
 * @param tags comma-separated tags, or null
 * @param summary the teaser under the title on the home page, or null
 * @param slug the file and URL name instead of one made from the title, or null
 * @param branch null, "" or "true" to make {@code post/<slug>}; "false" to stay on the current branch
 * @param pr "", "true" or "false": whether to commit, push and open a draft pull request (T061)
 * @param format set when {@code -Pformat} was given; always refused
 */
record NewPostOptions(String title, String tags, String summary, String slug, String branch, String pr,
        String format) {

    /**
     * Options with only a title, as {@code ./gradlew newPost -Ptitle="..."} gives them.
     *
     * @param title the title
     * @return the options
     */
    static NewPostOptions withTitle(String title) {
        return new NewPostOptions(title, null, null, null, null, null, null);
    }
}
