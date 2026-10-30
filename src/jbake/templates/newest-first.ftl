<#-- Newest first; posts with the same date by URL, A-Z. JBake's lists come sorted by date only; same-date
     order depends on the CPU-core count of the baking machine (OrientDB clusters). sort_by is stable and
     has no descending mode, hence the two ?reverse. The URL comparison is FreeMarker's collator for the
     baking JVM's default locale; the site's slugs are ASCII and the tied ones differ at the first letter,
     so every locale agrees. docs/reports/same-date-ordering-2026-10-30.md -->
<#function newestFirst docs>
  <#return docs?sort_by("uri")?reverse?sort_by("date")?reverse>
</#function>
