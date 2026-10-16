#!/bin/sh
# Renders Markdown (stdin) to HTML (stdout) exactly as this site's JBake 2.7 does: flexmark 0.64.8 with
# PegdownOptionsAdapter and jbake.properties' markdown.extensions. A probe for convert.py edge cases.
#   render.sh < file.md
# First run compiles Render.java into classes/. The classpath (flexmark.classpath) lists the jars from
# ~/.gradle/caches; regenerate it if flexmark's version in build.gradle changes:
#   ( find ~/.gradle/caches/modules-2/files-2.1/com.vladsch.flexmark -name '*0.64.8.jar' | grep -v sources;
#     find ~/.gradle/caches/modules-2/files-2.1/org.nibor.autolink -name 'autolink-0.6.0.jar' ) | tr '\n' ':' > flexmark.classpath
DIR=$(cd "$(dirname "$0")" && pwd)
CP=$(cat "$DIR/flexmark.classpath")
if [ ! -f "$DIR/classes/Render.class" ]; then
  javac -d "$DIR/classes" -cp "$CP" "$DIR/Render.java" || exit 1
fi
exec java -cp "$DIR/classes:$CP" Render "$@"
