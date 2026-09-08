#!/bin/bash
# Source this file from the repository root. Local tools stay out of Git.
export ANDROID_HOME="${ANDROID_HOME:-$PWD/.tools/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export GRADLE_USER_HOME="$PWD/.tools/gradle-home"
if [ -d "$PWD/.tools/jdk17/Contents/Home" ]; then
  export JAVA_HOME="$PWD/.tools/jdk17/Contents/Home"
fi
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
