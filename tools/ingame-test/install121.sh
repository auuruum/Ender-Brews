#!/bin/bash
# Installs disposable 1.21.x test servers for test.js under servers121/<minecraft>/<platform>.
# Usage: ./install121.sh <minecraft> [fabric] [paper] [neoforge] [forge]
# Takes the newest Fabric API, Paper build, NeoForge and Forge for that Minecraft release.
# JAVA must point to a Java 21 binary (installers run with it).
set -e
MC=$1; shift
JAVA=${JAVA:-java}
ROOT="$(cd "$(dirname "$0")" && pwd)/servers121/$MC"
latest() { grep -oE "<version>$1</version>" | sed 's/<[^>]*>//g' | sort -V | tail -1; }
minor=${MC#1.21}; minor=${minor#.}; minor=${minor:-0}

for platform in "$@"; do
  dir="$ROOT/$platform"
  if [ -f "$dir/.installed" ]; then echo "$MC $platform: already installed"; continue; fi
  rm -rf "$dir"; mkdir -p "$dir"
  case $platform in
    fabric)
      loader=$(curl -sf https://meta.fabricmc.net/v2/versions/loader | grep -oE '"version": *"[^"]+"' | head -1 | sed 's/.*"\([^"]*\)"$/\1/')
      installer=$(curl -sf https://meta.fabricmc.net/v2/versions/installer | grep -oE '"version": *"[^"]+"' | head -1 | sed 's/.*"\([^"]*\)"$/\1/')
      curl -sfo "$dir/server.jar" "https://meta.fabricmc.net/v2/versions/loader/$MC/$loader/$installer/server/jar"
      api=$(curl -sf https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml | latest "[^<]*\+$MC")
      mkdir -p "$dir/mods"
      curl -sfo "$dir/mods/fabric-api-$api.jar" "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$api/fabric-api-$api.jar"
      echo "$MC fabric: loader $loader, Fabric API $api" ;;
    paper)
      url=$(curl -sf "https://fill.papermc.io/v3/projects/paper/versions/$MC/builds/latest" | grep -oE '"url":"[^"]+"' | head -1 | sed 's/"url":"//; s/"$//')
      curl -sfo "$dir/paper.jar" "$url"
      echo "$MC paper: $url" ;;
    neoforge)
      v=$(curl -sf https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml | latest "21\.$minor\.[0-9]+(-beta)?")
      curl -sfo "$dir/installer.jar" "https://maven.neoforged.net/releases/net/neoforged/neoforge/$v/neoforge-$v-installer.jar"
      (cd "$dir" && "$JAVA" -jar installer.jar --installServer > install.log 2>&1)
      echo "$MC neoforge: $v" ;;
    forge)
      v=$(curl -sf https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml | latest "${MC//./\\.}-[0-9.]+")
      curl -sfo "$dir/installer.jar" "https://maven.minecraftforge.net/net/minecraftforge/forge/$v/forge-$v-installer.jar"
      (cd "$dir" && "$JAVA" -jar installer.jar --installServer > install.log 2>&1)
      echo "$MC forge: $v" ;;
    *) echo "unknown platform $platform"; exit 1 ;;
  esac
  echo "eula=true" > "$dir/eula.txt"
  touch "$dir/.installed"
done
