#!/bin/bash
# Runs test.js on every server installed under servers121/ with the matching jar from build/release.
# Usage: ./run121.sh [minecraft...]   (default: every installed version)
# JAVA must point to a Java 21 binary. Results go to servers121/results.txt.
cd "$(dirname "$0")"
RELEASE=../../build/release
jar_for() {
  case $1 in
    1.21|1.21.1) echo 1.21-1.21.1 ;;
    1.21.2|1.21.3|1.21.4) echo 1.21.2-1.21.4 ;;
    1.21.5) echo 1.21.5 ;;
    1.21.6|1.21.7|1.21.8) echo 1.21.6-1.21.8 ;;
    1.21.9|1.21.10) echo 1.21.9-1.21.10 ;;
    1.21.11) echo 1.21.11 ;;
  esac
}
versions=${*:-$(ls servers121 | grep -v results)}
for mc in $versions; do
  jar=$(ls $RELEASE/EnderBrews-*+mc$(jar_for $mc).jar)
  for dir in servers121/$mc/*/; do
    platform=$(basename "$dir")
    target="$dir/mods"; [ "$platform" = paper ] && target="$dir/plugins"
    mkdir -p "$target"; rm -f "$target"/EnderBrews-*.jar; cp "$jar" "$target/"
    line=$(node test.js "$platform" "$mc" 2>&1 | tee "servers121/$mc/$platform-test.log" | grep -E "passed$|^FAIL|^ERROR")
    echo "$mc $platform $(basename "$jar"): $line" | tee -a servers121/results.txt
  done
done
