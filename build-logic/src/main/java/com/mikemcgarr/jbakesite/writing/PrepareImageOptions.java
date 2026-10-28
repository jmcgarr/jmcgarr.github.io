package com.mikemcgarr.jbakesite.writing;

/**
 * What {@code ./gradlew prepareImage} is given besides the images (T140, T127).
 *
 * @param dryRun say what would happen, change nothing ({@code -PdryRun})
 * @param png keep a PNG as PNG: a graphic, a screenshot, a logo ({@code -Ppng})
 * @param jpeg turn a PNG into a JPEG even if it looks like a graphic ({@code -Pjpeg})
 * @param keepSource don't remove a new PNG after writing its JPEG ({@code -PkeepSource})
 * @param widthCap cap the width at this many pixels ({@code -Pwidth=N}), or null for the default: 1920 for a
 *        masthead, 1000 for an inline image (the T016/T017 limits)
 */
public record PrepareImageOptions(boolean dryRun, boolean png, boolean jpeg, boolean keepSource, Integer widthCap) {

    /** The defaults: a real run, the format decided by the pixels, the width cap by the image's place. */
    public static final PrepareImageOptions DEFAULTS = new PrepareImageOptions(false, false, false, false, null);

    /**
     * These options as a dry run.
     *
     * @return a copy with {@code dryRun} set
     */
    public PrepareImageOptions dryRun(boolean on) {
        return new PrepareImageOptions(on, png, jpeg, keepSource, widthCap);
    }
}
