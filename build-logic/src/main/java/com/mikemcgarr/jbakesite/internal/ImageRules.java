package com.mikemcgarr.jbakesite.internal;

/**
 * The size rules for a published image (AGENTS.md Rule 4, revival task T016): what {@code prepareImage} makes an
 * image fit and what the review's E5 checks it against. One place for both since the review of T127, so the two
 * can't drift apart.
 */
public final class ImageRules {

    /** The byte target: 400 KB. */
    public static final long MAX_BYTES = 400 * 1024;

    /** The widest a published image may be, and the width a masthead is resized to. */
    public static final int MAX_WIDTH = 1920;

    private ImageRules() {
    }
}
