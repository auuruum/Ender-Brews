#!/bin/bash
# Installs disposable 1.20.1 test servers for test.js under servers120/1.20.1/<platform>.
# Usage: ./install.sh [fabric] [paper] [forge] [neoforge]
# JAVA must point to a Java 17 binary (installers run with it).
set -e
MC=1.20.1
JAVA=${JAVA:-java}
ROOT="$(cd "$(dirname "$0")" && pwd)/servers120/$MC"

for platform in "$@"; do
  dir="$ROOT/$platform"
  if [ -f "$dir/.installed" ]; then echo "$MC $platform: already installed"; continue; fi
  mkdir -p "$dir"
  case $platform in
    fabric)
      loader=$(curl -sf https://meta.fabricmc.net/v2/versions/loader | grep -oE '"version": *"[^"]+"' | head -1 | sed 's/.*"\([^"]*\)"$/\1/')
      installer=$(curl -sf https://meta.fabricmc.net/v2/versions/installer | grep -oE '"version": *"[^"]+"' | head -1 | sed 's/.*"\([^"]*\)"$/\1/')
      curl -sfo "$dir/server.jar" "https://meta.fabricmc.net/v2/versions/loader/$MC/$loader/$installer/server/jar"
      api=0.92.12+1.20.1
      mkdir -p "$dir/mods"
      curl -sfo "$dir/mods/fabric-api-$api.jar" "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$api/fabric-api-$api.jar"
      echo "$MC fabric: loader $loader, Fabric API $api" ;;
    paper)
      url=$(curl -sf "https://fill.papermc.io/v3/projects/paper/versions/$MC/builds/latest" | grep -oE '"url":"[^"]+"' | head -1 | sed 's/"url":"//; s/"$//')
      curl -sfo "$dir/paper.jar" "$url"
      echo "$MC paper: $url" ;;
    forge)
      v=$MC-47.4.28
      curl -sfo "$dir/installer.jar" "https://maven.minecraftforge.net/net/minecraftforge/forge/$v/forge-$v-installer.jar"
      (cd "$dir" && "$JAVA" -jar installer.jar --installServer > install.log 2>&1)
      echo "$MC forge: $v" ;;
    neoforge)
      v=$MC-47.1.106
      curl -sfo "$dir/installer.jar" "https://maven.neoforged.net/releases/net/neoforged/forge/$v/forge-$v-installer.jar"
      (cd "$dir" && "$JAVA" -jar installer.jar --installServer > install.log 2>&1)
      echo "$MC neoforge: $v" ;;
    *) echo "unknown platform $platform"; exit 1 ;;
  esac
  echo "eula=true" > "$dir/eula.txt"
  touch "$dir/.installed"
done
