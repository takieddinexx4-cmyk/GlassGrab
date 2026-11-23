#!/bin/sh
# GlassGrab Gradle Wrapper Launcher (POSIX)
# If gradle-wrapper.jar is absent (e.g., fresh repo before CI wrapper generation),
# this script invokes system gradle to bootstrap the wrapper or execute tasks.
set -e
APP_HOME="$(cd "$(dirname "$0")" && pwd -P)"
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
    if command -v gradle >/dev/null 2>&1; then
        echo "Bootstrapping Gradle wrapper (9.6.0)..."
        gradle wrapper --gradle-version 9.6.0 --no-daemon
    else
        echo "ERROR: gradle-wrapper.jar not found and system 'gradle' is unavailable." >&2
        echo "Run 'gradle wrapper --gradle-version 9.6.0' or use the GitHub Actions workflow." >&2
        exit 1
    fi
fi

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

exec "$JAVACMD" ${DEFAULT_JVM_OPTS:-"-Xmx64m -Xms64m"}     -Dorg.gradle.appname="gradlew"     -classpath "$WRAPPER_JAR"     org.gradle.wrapper.GradleWrapperMain "$@"
