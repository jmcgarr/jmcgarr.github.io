package com.mikemcgarr.jbakesite.internal;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

/**
 * The header of a content file: YAML front matter, a block of {@code key: value} lines between two {@code ---}
 * lines at the top of the file (AGENTS.md Rule 3; T145, docs/07-FRONT-MATTER.md), each key with its line number.
 * The review checks it (E4), the tag tools read {@code tags} and {@code status} from it, {@code newPost}
 * {@linkplain #yaml writes one}, and the plugin's parser engine translates it for JBake inside the bake.
 *
 * <p>Every value is kept as text, as written: {@code date: 2026-10-21} reads as the string {@code 2026-10-21},
 * {@code title: 2012} as {@code 2012}, whatever a YAML type resolver would make of them, because JBake takes
 * every header value as text and parses the date itself. A list, {@code tags: [a, b]} (or one {@code - a} line per
 * tag), reads as JBake's comma-separated form, {@code a, b}, so {@link Tags#split} and JBake see the same tags.
 * A nested mapping has no place in a header and is reported. The first value of a repeated key wins.
 *
 * <p><b>The format before T145</b> (2026-11-02), JBake's own {@code key=value} lines ending with a {@code ~~~~~~}
 * line, is still read, so the review can compare a file with its version on {@code main} and say exactly what is
 * wrong with a file that still has it ({@link #legacy()}). JBake renders it as before. New files are YAML.
 *
 * @param values the keys and their values, as text, in file order (a list as its items joined with {@code ", "})
 * @param lines each key's 1-based line number
 * @param bodyStart the 0-based index of the first body line (the line after the closing {@code ---})
 * @param separated whether the header's closing line was found; without it JBake renders nothing
 * @param legacy whether the header is in the format before T145 ({@code key=value} lines and {@code ~~~~~~}), or
 *        the file has no header at all (it doesn't start with {@code ---})
 * @param error what is wrong with the YAML, or null; the values are then whatever was read before the problem
 * @param errorLine the 1-based line of the problem, 0 when there is none
 */
public record FrontMatter(Map<String, String> values, Map<String, Integer> lines, int bodyStart, boolean separated,
        boolean legacy, String error, int errorLine) {

    /** The line that opens and the line that closes the front matter. */
    public static final String DELIMITER = "---";

    /** The line that ended the header in the format before T145. */
    public static final String LEGACY_SEPARATOR = "~~~~~~";

    /** How a list value's items are joined: JBake's {@code tags=a, b} form. */
    static final String LIST_SEPARATOR = ", ";

    /** The words YAML reads as a boolean or null; a value spelled like one is quoted on the way out. */
    private static final List<String> RESERVED_WORDS = List.of("true", "false", "yes", "no", "on", "off", "y", "n",
            "null", "~");

    /** A value YAML would read as a number: a decimal, a float, a hexadecimal, an octal, an infinity, or NaN. */
    private static final Pattern NUMBER = Pattern.compile(
            "[-+]?(?:[0-9][0-9_]*(?:\\.[0-9_]*)?(?:[eE][-+]?[0-9]+)?|\\.[0-9_]+(?:[eE][-+]?[0-9]+)?|0x[0-9a-fA-F_]+|0o?[0-7_]+|\\.(?:inf|Inf|INF)|\\.(?:nan|NaN|NAN))");

    /** A value YAML would read as a date or a timestamp: {@code 2026-10-21}, with or without a time after it. */
    private static final Pattern TIMESTAMP = Pattern.compile("[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}(?:[Tt ].*)?");

    /** The one value kept plain though YAML reads it as a date: the post's {@code date}, in the site's form. */
    private static final Pattern PLAIN_DATE = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");

    /** The characters a plain scalar can't start with: YAML's indicators. */
    private static final String INDICATORS = "-?:,[]{}#&*!|>'\"%@`";

    /**
     * Reads the header of a file's text; line endings of any kind are accepted ({@link Text#lines}).
     *
     * @param text the whole file
     * @return the header
     */
    public static FrontMatter parse(String text) {
        String[] all = Text.lines(text);
        if (all.length > 0 && all[0].strip().equals(DELIMITER)) {
            return parseYaml(all);
        }
        return parseLegacy(all);
    }

    /** The front matter between the first line and the next {@code ---} line. */
    private static FrontMatter parseYaml(String[] all) {
        Map<String, String> values = new LinkedHashMap<>();
        Map<String, Integer> lines = new HashMap<>();
        int close = 1;
        while (close < all.length && !all[close].strip().equals(DELIMITER)) {
            close++;
        }
        if (close == all.length) {
            return new FrontMatter(values, lines, all.length, false, false, null, 0);
        }
        String block = String.join("\n", List.of(all).subList(1, close));
        String error = null;
        int errorLine = 0;
        try {
            Node root = new Yaml().compose(new StringReader(block));
            if (root != null && !(root instanceof MappingNode)) {
                error = "The front matter must be key: value lines.";
                errorLine = 2;
            } else if (root != null) {
                for (NodeTuple tuple : ((MappingNode) root).getValue()) {
                    int line = tuple.getKeyNode().getStartMark().getLine() + 2;   // the block starts on line 2
                    if (!(tuple.getKeyNode() instanceof ScalarNode keyNode)) {
                        error = "A key must be a word.";
                        errorLine = line;
                        break;
                    }
                    String key = keyNode.getValue();
                    String value = text(tuple.getValueNode());
                    if (value == null) {
                        error = key + " must be a value or a list of values, not nested keys.";
                        errorLine = line;
                        break;
                    }
                    values.putIfAbsent(key, value);
                    lines.putIfAbsent(key, line);
                }
            }
        } catch (MarkedYAMLException e) {
            error = e.getProblem() + ".";
            errorLine = e.getProblemMark().getLine() + 2;
        } catch (YAMLException e) {
            error = e.getMessage() + ".";
            errorLine = 2;
        }
        return new FrontMatter(values, lines, close + 1, true, false, error, errorLine);
    }

    /** A scalar as its text, a sequence of scalars as the items joined; null for anything else. */
    private static String text(Node node) {
        if (node instanceof ScalarNode scalar) {
            return scalar.getValue();
        }
        if (node instanceof SequenceNode sequence) {
            List<String> items = new ArrayList<>();
            for (Node item : sequence.getValue()) {
                if (!(item instanceof ScalarNode scalar)) {
                    return null;
                }
                items.add(scalar.getValue());
            }
            return String.join(LIST_SEPARATOR, items);
        }
        return null;
    }

    /** The format before T145: {@code key=value} lines up to the separator, as {@code ReviewChanges.java} read it. */
    private static FrontMatter parseLegacy(String[] all) {
        Map<String, String> values = new LinkedHashMap<>();
        Map<String, Integer> lines = new HashMap<>();
        for (int i = 0; i < all.length; i++) {
            String line = all[i].strip();
            if (line.startsWith(LEGACY_SEPARATOR)) {
                return new FrontMatter(values, lines, i + 1, true, true, null, 0);
            }
            int eq = line.indexOf('=');
            if (eq > 0) {
                String key = line.substring(0, eq).strip();
                values.putIfAbsent(key, line.substring(eq + 1).strip());
                lines.putIfAbsent(key, i + 1);
            }
        }
        return new FrontMatter(values, lines, all.length, false, true, null, 0);
    }

    /**
     * A value, stripped; "" when the key is absent.
     *
     * @param key the key, such as {@code date}
     * @return the value
     */
    public String get(String key) {
        return values.getOrDefault(key, "").strip();
    }

    /**
     * Whether the file is a draft ({@code status: draft}): rendered for preview, never published.
     *
     * @return true for a draft
     */
    public boolean isDraft() {
        return get("status").equals("draft");
    }

    /**
     * Whether the file is published ({@code status: published}).
     *
     * @return true for a published page or post
     */
    public boolean isPublished() {
        return get("status").equals("published");
    }

    /**
     * The file as JBake reads it (the parser engine's translation, T145): a {@code key=value} line per entry, the
     * header separator, then the body lines exactly as they are, LF line endings. A line break inside a value
     * becomes a space, because JBake's header has no multi-line values.
     *
     * @param text the whole file this header was read from
     * @param separator JBake's {@code header.separator}
     * @return the translated file
     */
    public String jbake(String text, String separator) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            out.add(entry.getKey() + "=" + entry.getValue().replace("\r\n", " ").replace('\r', ' ').replace('\n', ' '));
        }
        out.add(separator);
        String[] all = Text.lines(text);
        out.addAll(List.of(all).subList(bodyStart, all.length));
        return String.join("\n", out);
    }

    /**
     * The front matter for the given values, as {@code newPost} writes it: the opening {@code ---}, one
     * {@code key: value} line per entry in order, the closing {@code ---}, each line ending with LF. A
     * {@link List} is written as a flow list, {@code tags: [a, b]}. A value is written plain when YAML reads it back
     * as the same text, and double-quoted otherwise: when it is empty, starts with an indicator such as {@code [}
     * or {@code "}, contains {@code ": "} or {@code " #"}, ends with a colon, has a space at either end, or would
     * read as a boolean, a null, a number or a timestamp. The exception is {@code date} in the site's
     * {@code YYYY-MM-DD} form, which stays plain, as every other front matter tool expects it. A line break in a
     * value becomes a space: JBake's header has no multi-line values.
     *
     * @param values the keys and their values, a String or a List of Strings each
     * @return the front matter, ready to go before the body
     */
    public static String yaml(Map<String, ?> values) {
        StringBuilder out = new StringBuilder(DELIMITER).append('\n');
        values.forEach((key, value) -> {
            out.append(key).append(": ");
            if (value instanceof List<?> items) {
                out.append('[');
                for (int i = 0; i < items.size(); i++) {
                    out.append(i == 0 ? "" : LIST_SEPARATOR).append(scalar(key, String.valueOf(items.get(i)), true));
                }
                out.append(']');
            } else {
                out.append(scalar(key, String.valueOf(value), false));
            }
            out.append('\n');
        });
        return out.append(DELIMITER).append('\n').toString();
    }

    /** A value as YAML writes it: plain when it reads back unchanged, double-quoted otherwise. */
    private static String scalar(String key, String value, boolean inList) {
        String text = value.replace("\r\n", " ").replace('\r', ' ').replace('\n', ' ');
        return needsQuotes(key, text, inList) ? '"' + text.replace("\\", "\\\\").replace("\"", "\\\"") + '"' : text;
    }

    private static boolean needsQuotes(String key, String value, boolean inList) {
        if (value.isEmpty() || !value.equals(value.strip()) || INDICATORS.indexOf(value.charAt(0)) >= 0
                || value.contains(": ") || value.contains(" #") || value.endsWith(":")) {
            return true;
        }
        if (inList && (value.contains(",") || value.contains("[") || value.contains("]") || value.contains("{")
                || value.contains("}"))) {
            return true;
        }
        if (key.equals("date") && PLAIN_DATE.matcher(value).matches()) {
            return false;
        }
        return RESERVED_WORDS.contains(value.toLowerCase(Locale.ROOT)) || NUMBER.matcher(value).matches()
                || TIMESTAMP.matcher(value).matches();
    }
}
