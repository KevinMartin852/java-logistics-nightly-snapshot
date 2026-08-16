#!/usr/bin/env sh
set -eu

BUILD_DIR="${TMPDIR:-/tmp}/logistics-nightly-snapshot-classes"
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"
find src/main/java src/test/java -name '*.java' -print | sort | xargs javac -d "$BUILD_DIR"
java -cp "$BUILD_DIR" dev.infrai.logistics.snapshot.NightlySnapshotPlannerTest
