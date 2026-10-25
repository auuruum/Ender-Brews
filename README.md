# Ender Brews

**Store potions in your Ender Chest — get their effects forever.**
Terraria-style infinite potions for Minecraft 1.21.1.

One jar for everything: **Fabric · Quilt · NeoForge · Forge** mod and **Paper · Spigot · Purpur** plugin.

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

## Commands

| Command | Who | What |
|---|---|---|
| `/enderbrews` | everyone | Your endless effects, progress and pause buttons |
| `/enderbrews pause\|resume\|toggle <effect>` | everyone | Pause or resume one of your endless effects |
| `/enderbrews reload` | operators (`enderbrews.reload`) | Reload the config |

## Building

Requires Java 21.

```bash
./gradlew build
```

The universal jar ends up in `build/libs/EnderBrews-<version>+mc1.21.1.jar`.

Project layout:

- `shared/` — code shared by every platform: `core` (plain Java rules, config, translations) and `common` (vanilla Minecraft code for the mod loaders). Each platform compiles its own relocated copy (`dev.enderbrews.<platform>.*`), which is what lets all of them live in one jar.
- `fabric/`, `neoforge/`, `forge/` — thin loader entry points.
- `bukkit/` — the Paper / Spigot / Purpur plugin.
- `tools/ingame-test/` — starts real servers and checks the jar with a vanilla bot.
- `docs/DESIGN.md` — the design and every edge case.

## License

MIT
