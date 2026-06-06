#!/bin/sh
#
# Gradle start up script for POSIX compatible systems.
#

# Attempt to set APP_HOME
DIRNAME="$(dirname "$0")"
APP_HOME="$(cd "$DIRNAME" && pwd -P)"

exec "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@" 2>/dev/null || \
  exec java -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@" 2>/dev/null || \
  exec java -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
    org.gradle.wrapper.GradleWrapperMain "$@"
