#!/bin/bash
# Runs test.js on every server installed under servers120/1.20.1 with the jar from build/libs.
# Usage: ./run.sh [platform...]   (default: every installed platform)
# JAVA must point to a Java 17 binary. Results go to servers120/results.txt.
cd "$(dirname "$0")"
MC=1.20.1
jar=$(ls ../../build/libs/EnderBrews-*+mc$MC.jar)
platforms=${*:-$(ls servers120/$MC | grep -v -E '\.log$')}
for platform in $platforms; do
  dir=servers120/$MC/$platform
  target="$dir/mods"; [ "$platform" = paper ] && target="$dir/plugins"
  mkdir -p "$target"; rm -f "$target"/EnderBrews-*.jar; cp "$jar" "$target/"
  line=$(node test.js "$platform" 2>&1 | tee "servers120/$MC/$platform-test.log" | grep -E "passed$|^FAIL|^ERROR")
  echo "$MC $platform $(basename "$jar"): $line" | tee -a servers120/results.txt
done
