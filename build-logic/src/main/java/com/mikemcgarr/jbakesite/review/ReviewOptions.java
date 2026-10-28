package com.mikemcgarr.jbakesite.review;

/**
 * What {@code ./gradlew reviewChanges} is given.
 *
 * @param base the branch to diff against ({@code -Pbase}, default {@code main})
 * @param all review every page, not the changed ones ({@code -Pall})
 * @param strict fail on warnings too ({@code -Pstrict})
 * @param online accepted and ignored until external links are checked (T119; {@code -Ponline})
 */
public record ReviewOptions(String base, boolean all, boolean strict, boolean online) {

    /** The defaults: the changes since the merge base with {@code main}, warnings listed but not failing. */
    public static final ReviewOptions DEFAULTS = new ReviewOptions("main", false, false, false);
}
