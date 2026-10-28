package com.mikemcgarr.jbakesite.review;

/**
 * What a review came to, for a caller outside the package ({@code openPr}, T121): whether the change may go on, and
 * the report's one-line summary ("Reviewed 1 page (about.html) from 1 changed file. 0 errors, 1 warning. ...", or
 * the clean-tree sentence), for a pull request's description. The findings themselves were printed by the
 * {@link ReviewRunner} that produced this.
 *
 * @param passed no errors (and, under {@code -Pstrict}, no warnings)
 * @param summary the report's summary line, without a line break
 */
public record ReviewOutcome(boolean passed, String summary) {
}
