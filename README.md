# <img src="docs/logo.png" alt="" width="72" align="absmiddle"> Ender Brews

[![Modrinth downloads](https://img.shields.io/modrinth/dt/ender-brews?logo=modrinth&label=Modrinth&color=1bd96a)](https://modrinth.com/project/ender-brews)
[![CurseForge](https://img.shields.io/badge/CurseForge-Ender%20Brews-f16436?logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/ender-brews)
[![Latest version](https://img.shields.io/modrinth/v/ender-brews?logo=modrinth&label=version&color=1bd96a)](https://modrinth.com/project/ender-brews/versions)
[![Minecraft](https://img.shields.io/modrinth/game-versions/ender-brews?logo=minecraft&label=Minecraft&color=62b47a)](https://modrinth.com/project/ender-brews/versions)
[![GitHub stars](https://img.shields.io/github/stars/auuruum/Ender-Brews?logo=github&color=yellow)](https://github.com/auuruum/Ender-Brews)
[![License](https://img.shields.io/github/license/auuruum/Ender-Brews?color=blue)](LICENSE)

Put 8 potions of the same kind in your Ender Chest and that effect becomes endless. Take them out and it's gone.

<p align="center">
  <a href="https://youtu.be/_1pOw55mNeg"><img src="https://img.youtube.com/vi/_1pOw55mNeg/maxresdefault.jpg" alt="Ender Brews showcase" width="640"></a>
</p>

<p align="center">
  <a href="https://modrinth.com/project/ender-brews"><img src="https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/available/modrinth_vector.svg" alt="Available on Modrinth" height="64"></a>
  &nbsp;
  <a href="https://www.curseforge.com/minecraft/mc-mods/ender-brews"><img src="https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/available/curseforge_vector.svg" alt="Available on CurseForge" height="64"></a>
</p>

## 🧪 How it works

- Only the Ender Chest counts (shulker boxes inside it too).
- Instant effects never become endless. Harmful ones don't either, unless you enable `allow_harmful_effects`.
- A stronger potion stacks on top: Strength II over endless Strength I gives II, then I comes back.
- Milk and totems can't remove endless effects.
- `/enderbrews` lists your effects. Click one to pause it.

## 📥 Install

One jar for every supported mod loader and plugin server. Pick the jar for your Minecraft version from the downloads.

It only has to be on the server. Vanilla clients can join. Installing it on the client as well adds potion tooltips (`Strength: 5/8 in Ender Chest`), live-updating chat list and a pause-all key.

## ⚙️ Config

`config/enderbrews.toml` (plugin: `plugins/EnderBrews/enderbrews.toml`). Every option is commented in the file. Reload with `/enderbrews reload` (permission `enderbrews.reload`).

## Building

This branch is the Minecraft 1.21.x line (1.21 – 1.21.11); the 26.x line lives on `port/26.3`. Needs Java 21.
Each jar covers a range of releases and runs on Fabric, Quilt, NeoForge, Forge and Paper/Spigot:

| Jar | Minecraft |
| --- | --- |
| `EnderBrews-<version>+mc1.21-1.21.1.jar` | 1.21, 1.21.1 |
| `EnderBrews-<version>+mc1.21.2-1.21.4.jar` | 1.21.2, 1.21.3, 1.21.4 |
| `EnderBrews-<version>+mc1.21.5.jar` | 1.21.5 |
| `EnderBrews-<version>+mc1.21.6-1.21.8.jar` | 1.21.6, 1.21.7, 1.21.8 |
| `EnderBrews-<version>+mc1.21.9-1.21.10.jar` | 1.21.9, 1.21.10 |
| `EnderBrews-<version>+mc1.21.11.jar` | 1.21.11 |

```bash
./gradlew build -Ptarget=1.21.1
```

builds one jar into `build/libs/` (targets are the files in `versions/`). `./build-all.ps1` builds all of them into
`build/release/`. Code that differs between releases is picked with `//#if >=1.21.6` lines, see
[Preprocessor.groovy](buildSrc/src/main/groovy/Preprocessor.groovy).
Design notes: [docs/DESIGN.md](docs/DESIGN.md). Real-server tests: [tools/ingame-test](tools/ingame-test/README.md).

## License

[MIT](LICENSE)
