package com.mikemcgarr.jbakesite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mikemcgarr.jbakesite.review.ReviewChangesTask;
import com.mikemcgarr.jbakesite.verification.CheckDocsNotPublishedTask;
import com.mikemcgarr.jbakesite.verification.CheckMastheadVariantsTask;
import com.mikemcgarr.jbakesite.verification.CheckTagRedirectsTask;
import com.mikemcgarr.jbakesite.verification.CheckUrlsTask;
import com.mikemcgarr.jbakesite.writing.NewPostTask;
import com.mikemcgarr.jbakesite.writing.PrepareImageTask;
import com.mikemcgarr.jbakesite.writing.TagRedirects;
import com.mikemcgarr.jbakesite.writing.TagRedirectsTask;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Applies the plugin to an in-memory project and checks what it registers and how the extension drives it. */
class JbakeSitePluginTest {

    private Project project;
    private JbakeSiteExtension site;

    @BeforeEach
    void applyPlugin() {
        project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("jbake-site");
        site = project.getExtensions().getByType(JbakeSiteExtension.class);
    }

    @Test
    void registersTheExtensionWithTheSiteDefaults() {
        assertTrue(project.getPlugins().hasPlugin(JbakeSitePlugin.class));

        assertEquals(projectFile("src/jbake"), site.getSource().get().getAsFile());
        assertEquals(projectFile("build/jbake"), site.getDestination().get().getAsFile());
        assertEquals(projectFile("src/case-redirects"), site.getCaseRedirects().get().getAsFile());
        assertEquals(projectFile("docs/baseline/urls.txt"), site.getPublishedUrls().get().getAsFile());
        assertEquals(projectFile("docs/baseline/expected-removals.txt"), site.getExpectedRemovals().get().getAsFile());
        assertEquals(TagRedirects.RENAMES, site.getTagRenames().get(), "the owner-approved renames of 2026-10-09");
        assertEquals(List.of(), site.getCommitTrailers().get(), "openPr adds no trailer unless the site says so");
        assertFalse(site.getLogbackConfiguration().isPresent(), "unset: the bundled configuration");
        assertEquals(projectFile("src/scss"), site.getScss().getSource().get().getAsFile());
        assertEquals("css", site.getScss().getPublishedDir().get());
        assertEquals(List.of(), site.getScss().getRetiredCss().get());
        assertEquals("the site's CSS is compiled from the SCSS in src/scss; JBake would publish this copy, and it would go stale.",
                site.getScss().getHandEditedCssExplanation().get(), "a project-relative path: the same task input on every machine");
        assertEquals(JbakeClasspath.DEFAULT_EXCLUDED_GROUPS, site.getClasspath().getExcludedGroups().get());
        assertEquals(JbakeClasspath.DEFAULT_SUBSTITUTIONS, site.getClasspath().getSubstitutions().get());
    }

    @Test
    void registersCompileSassWithoutAGroup() {
        CompileSassTask task = (CompileSassTask) project.getTasks().getByName(JbakeSitePlugin.COMPILE_SASS_TASK_NAME);

        assertNull(task.getGroup(), "an implementation detail of bake, hidden from `gradlew tasks` (T133)");
        assertEquals(projectFile("src/scss"), task.getSource().get().getAsFile());
        assertEquals(projectFile("build/generated/sass/css"), task.getDestinationDirectory().get().getAsFile());
    }

    @Test
    void registersBakeInTheJbakeGroupWithWhatJbakeNeeds() {
        BakeTask bake = (BakeTask) project.getTasks().getByName(JbakeSitePlugin.BAKE_TASK_NAME);

        assertEquals(JbakeSitePlugin.JBAKE_GROUP, bake.getGroup());
        assertEquals("Bakes the site from src/jbake into build/jbake with JBake.", bake.getDescription());
        assertEquals(projectFile("src/jbake"), bake.getSource().get().getAsFile());
        assertEquals(projectFile("build/jbake"), bake.getDestination().get().getAsFile());
        assertEquals(projectFile("src/case-redirects"), bake.getCaseRedirects().get().getAsFile());
        assertEquals(Set.of(projectFile("build/generated/sass/css")), bake.getThemeCss().getFiles(), "compileSass's output");
        assertEquals("css", bake.getThemeCssPublishedDir().get());
        assertEquals(NewJdkJvmArgs.forCurrentJvm(), bake.getJvmArguments().get());
        assertEquals(projectFile("build/tmp/jffi"), bake.getJffiDirectory().get().getAsFile());
        assertFalse(bake.getLogbackConfiguration().isPresent());
        assertEquals(Set.of(JbakeSitePlugin.CHECK_NO_HAND_EDITED_CSS_TASK_NAME, JbakeSitePlugin.COMPILE_SASS_TASK_NAME),
                dependenciesOf(bake), "the guard explicitly, compileSass through the CSS input");
    }

    @Test
    void registersBakePreviewInTheJbakeGroupWithTheScssWatcher() {
        BakePreviewTask preview = (BakePreviewTask) project.getTasks().getByName(JbakeSitePlugin.BAKE_PREVIEW_TASK_NAME);

        assertEquals(JbakeSitePlugin.JBAKE_GROUP, preview.getGroup());
        assertEquals("Bakes, serves http://localhost:8080, rebuilds on save, and reloads the open page (Ctrl+C to stop).",
                preview.getDescription());
        assertEquals(projectFile("src/jbake"), preview.getSource().get().getAsFile());
        assertEquals(projectFile("build/jbake"), preview.getDestination().get().getAsFile());
        assertEquals(Set.of(projectFile("build/generated/sass/css")), preview.getThemeCss().getFiles());
        assertEquals("css", preview.getThemeCssPublishedDir().get());
        assertEquals(NewJdkJvmArgs.forCurrentJvm(), preview.getJvmArguments().get());
        assertEquals(Set.of(JbakeSitePlugin.CHECK_NO_HAND_EDITED_CSS_TASK_NAME, JbakeSitePlugin.COMPILE_SASS_TASK_NAME),
                dependenciesOf(preview));
        assertTrue(preview.getOutputs().getFiles().isEmpty(), "no outputs: the preview runs every time");

        SassPreviewWatcher watcher = preview.getScssWatcher().get();
        assertEquals(projectFile("src/scss"), watcher.getParameters().getSourceDir().get().getAsFile());
        assertEquals(projectFile("build/jbake/css"), watcher.getParameters().getTargetDir().get().getAsFile());
        assertEquals("css", watcher.getParameters().getPublishedDir().get());
        assertEquals("src/scss", watcher.getParameters().getSourceName().get(), "as the log lines name it");
    }

    @Test
    void theExtensionIsReadLazily() {
        site.getSource().set(projectFile("site"));
        site.getDestination().set(projectFile("out"));
        site.getCaseRedirects().set(projectFile("stubs"));
        site.getLogbackConfiguration().set(projectFile("logging.xml"));
        site.scss(scss -> {
            scss.getSource().set(projectFile("styles"));
            scss.getPublishedDir().set("assets/styles");
            scss.getRetiredCss().set(List.of("old.css"));
        });

        CompileSassTask compile = (CompileSassTask) project.getTasks().getByName(JbakeSitePlugin.COMPILE_SASS_TASK_NAME);
        CheckNoHandEditedCssTask guard =
                (CheckNoHandEditedCssTask) project.getTasks().getByName(JbakeSitePlugin.CHECK_NO_HAND_EDITED_CSS_TASK_NAME);
        BakeTask bake = (BakeTask) project.getTasks().getByName(JbakeSitePlugin.BAKE_TASK_NAME);

        assertEquals(projectFile("styles"), compile.getSource().get().getAsFile());
        assertEquals(projectFile("build/generated/sass/assets/styles"), compile.getDestinationDirectory().get().getAsFile());
        assertEquals(projectFile("styles"), guard.getScssSource().get().getAsFile());
        assertEquals(List.of("old.css"), guard.getRetiredCss().get());
        assertTrue(guard.getExplanation().get().contains("in styles;"), guard.getExplanation().get());
        assertEquals(projectFile("site"), bake.getSource().get().getAsFile());
        assertEquals(projectFile("out"), bake.getDestination().get().getAsFile());
        assertEquals(projectFile("stubs"), bake.getCaseRedirects().get().getAsFile());
        assertEquals(projectFile("logging.xml"), bake.getLogbackConfiguration().get().getAsFile());
        assertEquals("assets/styles", bake.getThemeCssPublishedDir().get());

        BakePreviewTask preview = (BakePreviewTask) project.getTasks().getByName(JbakeSitePlugin.BAKE_PREVIEW_TASK_NAME);
        assertEquals(projectFile("out"), preview.getDestination().get().getAsFile());
        assertEquals(projectFile("out/assets/styles"), preview.getScssWatcher().get().getParameters().getTargetDir().get().getAsFile());
        assertEquals("styles", preview.getScssWatcher().get().getParameters().getSourceName().get());
    }

    @Test
    void registersTheWritingTasksWithTheSiteAndTheirProperties() {
        NewPostTask newPost = (NewPostTask) project.getTasks().getByName(JbakeSitePlugin.NEW_POST_TASK_NAME);
        ReviewChangesTask review = (ReviewChangesTask) project.getTasks().getByName(JbakeSitePlugin.REVIEW_CHANGES_TASK_NAME);
        PrepareImageTask prepareImage = (PrepareImageTask) project.getTasks().getByName(JbakeSitePlugin.PREPARE_IMAGE_TASK_NAME);
        TagRedirectsTask tagRedirects = (TagRedirectsTask) project.getTasks().getByName(JbakeSitePlugin.TAG_REDIRECTS_TASK_NAME);

        for (SiteToolTask task : List.of(newPost, review, prepareImage, tagRedirects)) {
            assertEquals(JbakeSitePlugin.WRITING_GROUP, task.getGroup(), task.getName());
            assertEquals(projectFile("src/jbake"), task.getSource().get().getAsFile(), task.getName());
            assertEquals(projectFile("src/case-redirects"), task.getCaseRedirects().get().getAsFile(), task.getName());
            assertEquals(projectFile("docs/baseline/urls.txt"), task.getPublishedUrls().get().getAsFile(), task.getName());
            assertTrue(task.getOutputs().getFiles().isEmpty(), task.getName() + " has no outputs: it runs every time");
        }
        assertEquals("Starts a draft post on a new post/<slug> branch: ./gradlew newPost -Ptitle=\"My Post Title\"", newPost.getDescription());
        assertFalse(newPost.getTitle().isPresent(), "no -Ptitle on this project");
        assertEquals("Reviews the pages this branch changed for broken Markdown, links and images: ./gradlew reviewChanges [-Pall]",
                review.getDescription());
        assertEquals(Set.of(JbakeSitePlugin.BAKE_TASK_NAME), dependenciesOf(review), "reviewed after a bake");
        assertEquals(projectFile("build/jbake"), review.getSite().get().getAsFile());
        assertEquals("Prepares a hand-added image for the site (sRGB, metadata, size, masthead variants): ./gradlew prepareImage -Pimage=<path>",
                prepareImage.getDescription());
        assertEquals(TagRedirects.RENAMES, tagRedirects.getTagRenames().get());
    }

    @Test
    void registersTheChecksInTheVerificationGroupUnderCheckSiteWhichCheckRunsWithTheReview() {
        CheckTagRedirectsTask tagRedirects = (CheckTagRedirectsTask) project.getTasks().getByName(JbakeSitePlugin.CHECK_TAG_REDIRECTS_TASK_NAME);
        CheckMastheadVariantsTask mastheads = (CheckMastheadVariantsTask) project.getTasks().getByName(JbakeSitePlugin.CHECK_MASTHEAD_VARIANTS_TASK_NAME);
        CheckUrlsTask urls = (CheckUrlsTask) project.getTasks().getByName(JbakeSitePlugin.CHECK_URLS_TASK_NAME);
        CheckDocsNotPublishedTask docs = (CheckDocsNotPublishedTask) project.getTasks().getByName(JbakeSitePlugin.CHECK_DOCS_NOT_PUBLISHED_TASK_NAME);
        Task checkSite = project.getTasks().getByName(JbakeSitePlugin.CHECK_SITE_TASK_NAME);
        Task check = project.getTasks().getByName("check");

        for (Task task : List.of(tagRedirects, mastheads, urls, docs, checkSite)) {
            assertEquals(JbakeSitePlugin.VERIFICATION_GROUP, task.getGroup(), task.getName());
            assertTrue(task.getOutputs().getFiles().isEmpty(), task.getName() + " has no outputs: it runs every time");
        }
        assertEquals(Set.of(JbakeSitePlugin.BAKE_TASK_NAME), dependenciesOf(tagRedirects));
        assertEquals(projectFile("build/jbake"), tagRedirects.getSite().get().getAsFile(), "the bake, unless -PsiteDir says otherwise");
        assertEquals(TagRedirects.RENAMES, tagRedirects.getTagRenames().get());
        assertEquals(Set.of(), dependenciesOf(mastheads), "reads the source tree only");
        assertEquals(Set.of(JbakeSitePlugin.BAKE_TASK_NAME), dependenciesOf(urls));
        assertEquals(projectFile("build/jbake"), urls.getSite().get().getAsFile());
        assertEquals(projectFile("docs/baseline/urls.txt"), urls.getPublishedUrls().get().getAsFile(), "the baseline");
        assertEquals(projectFile("docs/baseline/expected-removals.txt"), urls.getExpectedRemovals().get().getAsFile());
        assertFalse(urls.getStrict().isPresent(), "no -Pstrict on this project");
        assertEquals(Set.of(JbakeSitePlugin.BAKE_TASK_NAME), dependenciesOf(docs));
        assertEquals(projectFile("build/jbake"), docs.getSite().get().getAsFile());
        assertEquals(Set.of(JbakeSitePlugin.CHECK_URLS_TASK_NAME, JbakeSitePlugin.CHECK_DOCS_NOT_PUBLISHED_TASK_NAME,
                JbakeSitePlugin.CHECK_TAG_REDIRECTS_TASK_NAME, JbakeSitePlugin.CHECK_MASTHEAD_VARIANTS_TASK_NAME),
                dependenciesOf(checkSite), "what CI runs on the staged site");
        assertEquals(Set.of(JbakeSitePlugin.REVIEW_CHANGES_TASK_NAME, JbakeSitePlugin.CHECK_SITE_TASK_NAME), dependenciesOf(check),
                "the review is about the change, checkSite about the site");
    }

    @Test
    void theCheckUrlsTaskFollowsTheExtensionsBaselineAndExpectedRemovals() {
        site.getPublishedUrls().set(projectFile("live-urls.txt"));
        site.getExpectedRemovals().set(projectFile("gone.txt"));

        CheckUrlsTask urls = (CheckUrlsTask) project.getTasks().getByName(JbakeSitePlugin.CHECK_URLS_TASK_NAME);

        assertEquals(projectFile("live-urls.txt"), urls.getPublishedUrls().get().getAsFile());
        assertEquals(projectFile("gone.txt"), urls.getExpectedRemovals().get().getAsFile());
    }

    @Test
    void onlyTheJbakeWritingAndVerificationGroupsShowInTheTaskList() {
        Set<String> groups = project.getTasks().stream().map(Task::getGroup).filter(g -> g != null).collect(Collectors.toSet());

        assertEquals(Set.of(JbakeSitePlugin.JBAKE_GROUP, JbakeSitePlugin.WRITING_GROUP, JbakeSitePlugin.VERIFICATION_GROUP, "build"),
                groups, "ours, plus the base plugin's clean and assemble");
    }

    private Set<String> dependenciesOf(Task task) {
        return task.getTaskDependencies().getDependencies(task).stream().map(Task::getName).collect(Collectors.toSet());
    }

    private File projectFile(String path) {
        return new File(project.getProjectDir(), path);
    }
}
