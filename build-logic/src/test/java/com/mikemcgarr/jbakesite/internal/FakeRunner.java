package com.mikemcgarr.jbakesite.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link CommandRunner} for tests: answers each command from a script keyed by the command's start ("gh auth
 * status"; the longest matching start wins) and records what was asked. A command nothing matches fails loudly: a
 * captured run throws the {@link IOException} the real runner throws for a program that isn't installed (so a test
 * can script "gh is missing" by scripting nothing for gh), and a visible run throws {@link IllegalStateException},
 * since a tool only runs a command visibly after checking the program is there, so an unscripted one is a test's
 * mistake. A visible run succeeds when its scripted exit is 0.
 */
public final class FakeRunner implements CommandRunner {

    private final Map<String, Result> script = new LinkedHashMap<>();
    private final List<String> calls = new ArrayList<>();
    private final StringBuilder shown = new StringBuilder();

    /**
     * Scripts an answer.
     *
     * @param commandPrefix the command's words, as one string, that the answer is for
     * @param exit the exit code
     * @param output the output
     * @return this
     */
    public FakeRunner answer(String commandPrefix, int exit, String output) {
        script.put(commandPrefix, new Result(exit, output));
        return this;
    }

    /**
     * The commands run so far, each as one string.
     *
     * @return the commands, in order
     */
    public List<String> calls() {
        return calls;
    }

    /**
     * What a visibly run command printed.
     *
     * @return the output
     */
    public String shown() {
        return shown.toString();
    }

    @Override
    public Result run(List<String> command, Stderr stderr) throws IOException {
        String joined = String.join(" ", command);
        calls.add(joined);
        return script.entrySet().stream()
                .filter(e -> joined.startsWith(e.getKey()))
                .max(Comparator.comparingInt(e -> e.getKey().length()))
                .map(Map.Entry::getValue)
                .orElseThrow(() -> new IOException("Cannot run program \"" + command.get(0) + "\": nothing scripted for '" + joined + "'"));
    }

    @Override
    public boolean runVisibly(List<String> command) {
        Result result;
        try {
            result = run(command, Stderr.MERGE);
        } catch (IOException e) {
            throw new IllegalStateException("A visible command the test didn't script: " + e.getMessage(), e);
        }
        shown.append(result.output());
        return result.exit() == 0;
    }
}
