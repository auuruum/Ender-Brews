<p align="center"><img src="docs/logo.png" alt="Ender Brews" width="256"></p>

# Ender Brews

**Store potions in your Ender Chest — get their effects forever.**
Terraria-style infinite potions for Minecraft 26.3.

One jar for **Fabric · Quilt · NeoForge · Forge** and the **Paper / Spigot / Purpur** plugin API.

This branch targets **26.3 and Java 25**. The **1.21.1** version is maintained on the `1.21.1` branch; download the jar that matches your Minecraft version. See [validation notes](docs/VALIDATION-26.3.md) for the tested server and client combinations.

## How it works

- Put **8 potions of the same kind** in your Ender Chest (or in a shulker box inside it) and that effect becomes **endless**: it shows `∞` and never runs out.
- Take the potions out and the effect is gone.
- Only the Ender Chest counts — not your inventory, not other chests.
- Harmful effects (poison, weakness, slowness…) and instant ones (healing, harming) never become endless.
- A stronger potion still works on top: drink Strength II while you have endless Strength I and you get II until it runs out, then endless I comes back.
- Milk and totems can't take endless effects away; they come right back.
- `/enderbrews` lists your effects and lets you **pause** any of them with a click (great for invisibility or slow falling while building).

Everything is configurable in `config/enderbrews.toml` (mod) or `plugins/EnderBrews/enderbrews.toml` (plugin): the threshold (1, 16, 64…), per-effect thresholds, max level, max number of endless effects, black/whitelists, shulker boxes, splash potions and more. Reload with `/enderbrews reload`.

The mod only needs to be on the **server**. Players with a vanilla client can join, and in singleplayer you just install it.

### Extras with the mod on your client

Installing the same jar on the client is optional and adds a few small things. They work on servers running the mod or the plugin:

- The `/enderbrews` list updates **in place** in chat instead of piling up.
- **Click an endless effect** next to your inventory to pause it. Paused effects show greyed out below the list; click one to resume it.
- **Potion tooltips** show your progress: `Strength: 5/8 in Ender Chest`, or `∞ Strength: endless`.
- A key to **pause / resume all** endless effects (unbound by default, set it in Controls → Ender Brews).

## Commands

| Command | Who | What |
|---|---|---|
| `/enderbrews` | everyone | Your endless effects, progress and pause buttons |
| `/enderbrews pause\|resume\|toggle <effect>` | everyone | Pause or resume one of your endless effects |
| `/enderbrews reload` | operators (`enderbrews.reload`) | Reload the config |

## Building

Requires Java 25.

```bash
./gradlew build
```

The universal jar ends up in `build/libs/EnderBrews-<version>+mc26.3.jar`. This includes the plugin, so this jar requires Java 25 on every platform.

`./gradlew build` also runs the shared rule and protocol tests. For tests with real servers and clients, see [tools/ingame-test/README.md](tools/ingame-test/README.md).

Project layout:

- `shared/` — code shared by every platform: `core` (plain Java rules, config, translations) and `common` (vanilla Minecraft code for the mod loaders). Each platform compiles its own relocated copy (`dev.enderbrews.<platform>.*`), which is what lets all of them live in one jar.
- `fabric/`, `neoforge/`, `forge/` — thin loader entry points.
- `bukkit/` — the Paper / Spigot / Purpur plugin.
- `tools/ingame-test/` — starts real servers and checks the jar with a vanilla bot.
- `docs/DESIGN.md` — the design and every edge case.

## License

MIT
