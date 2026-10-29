package com.mikemcgarr.jbakesite.review;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A finding that is right for the site but wrong for one line is suppressed by an HTML comment on the line before
 * it: {@code <!-- review: ignore E2, the brackets are intended -->}. The comment survives into the baked page, so
 * the page checks honour it too.
 */
final class Suppressions {

    private static final Pattern SUPPRESS = Pattern.compile("<!--\\s*review:\\s*ignore\\s+([A-Z]\\d+(?:\\s*,\\s*[A-Z]\\d+)*)[^>]*-->");

    private Suppressions() {
    }

    /** The codes a suppression comment in this text names, or null when it has none. */
    static Set<String> in(String text) {
        Matcher m = SUPPRESS.matcher(text);
        return m.find() ? Set.of(m.group(1).split("\\s*,\\s*")) : null;
    }
}
