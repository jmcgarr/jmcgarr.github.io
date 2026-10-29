package com.mikemcgarr.jbakesite.review;

import java.nio.file.Path;

/**
 * A changed file, as {@code git diff --name-status} reports it.
 *
 * @param status A (added), M (modified), D (deleted), R (renamed, with {@code oldPath}) or U (untracked)
 * @param path the file, absolute
 * @param oldPath where a renamed file was, absolute; null otherwise
 */
record Change(char status, Path path, Path oldPath) {

    boolean isNew() {
        return status == 'A' || status == 'U';
    }

    boolean exists() {
        return status != 'D';
    }

    /** A deleted or renamed file's old path: the URL that goes away. */
    Path removed() {
        return status == 'R' ? oldPath : path;
    }
}
