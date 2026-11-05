/**
 * What the plugin runs <em>inside JBake's JVM</em>, not Gradle's: the launcher the bake and the preview start
 * instead of JBake's own, and the parser engine it registers, which lets every post and page carry YAML front
 * matter (T145, docs/07-FRONT-MATTER.md). These classes compile against jbake-core and flexmark
 * ({@code compileOnly} in build-logic/build.gradle) and are loaded only on the {@code jbake} configuration's
 * classpath, where the plugin's own jar is added for them ({@code JbakeSitePlugin}); nothing in Gradle's JVM
 * references them except by name.
 */
package com.mikemcgarr.jbakesite.engine;
