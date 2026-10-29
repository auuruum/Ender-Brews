# Minecraft 1.21 – 1.21.11 validation

Backport of the 26.3 feature set to every 1.21.x release, checked on Windows with Java 21 on 2026-10-30.
Six jars, each a Fabric/Quilt, NeoForge and Forge mod and a Paper/Spigot plugin at once:

| Jar | Minecraft | Compiled against |
| --- | --- | --- |
| `+mc1.21-1.21.1` | 1.21, 1.21.1 | 1.21.1, Fabric API 0.116.17, NeoForge 21.1.265, Forge 52.1.20 |
| `+mc1.21.2-1.21.4` | 1.21.2 – 1.21.4 | 1.21.4, Fabric API 0.119.4, NeoForge 21.4.161, Forge 54.1.22 |
| `+mc1.21.5` | 1.21.5 | 1.21.5, Fabric API 0.128.2, NeoForge 21.5.101, Forge 55.1.18 |
| `+mc1.21.6-1.21.8` | 1.21.6 – 1.21.8 | 1.21.8, Fabric API 0.136.1, NeoForge 21.8.57, Forge 58.1.26 |
| `+mc1.21.9-1.21.10` | 1.21.9, 1.21.10 | 1.21.10, Fabric API 0.138.4, NeoForge 21.10.64, Forge 60.1.18 |
| `+mc1.21.11` | 1.21.11 | 1.21.11, Fabric API 0.141.6, NeoForge 21.11.48, Forge 61.2.4 |

## Compile checks

Besides each target's own version, the shared, Fabric, NeoForge and Forge code was compiled against
every other release in each range: 1.21 (NeoForge 21.0.167, Forge 51.0.33), 1.21.2 (NeoForge
21.2.1-beta; no Forge exists), 1.21.3 (Forge 53.1.16), 1.21.6 (NeoForge 21.6.20-beta, Forge 56.0.9),
1.21.7 (NeoForge 21.7.25-beta, Forge 57.0.3) and 1.21.9 (NeoForge 21.9.16-beta, Forge 59.0.5).
All pass, except the expected Forge 51 case below.

## Server runs

`tools/ingame-test/run121.sh`: a real dedicated server with the jar, driven by a headless Mineflayer
bot, 23 checks per run (thresholds, stronger potions on top, removal, `/effect clear`, shulker boxes,
harmful effects, `/enderbrews` pause/resume, status and pause packets as a modded client sends them,
no endless effects saved on logout, no Ender Brews errors in the log). **36 runs, 828/828 checks.**

| Minecraft | Fabric | Paper | NeoForge | Forge |
| --- | --- | --- | --- | --- |
| 1.21 | 23/23 | 23/23 (build 130) | 23/23 (21.0.167) | 23/23 (51.0.33) |
| 1.21.1 | 23/23 | 23/23 (133) | 23/23 (21.1.265) | 23/23 (52.1.20) |
| 1.21.2 | 23/23 | — | 23/23 (21.2.1-beta) | no Forge release |
| 1.21.4 | 23/23 | 23/23 (232) | 23/23 (21.4.161) | 23/23 (54.1.22) |
| 1.21.5 | 23/23 | 23/23 (114) | 23/23 (21.5.101) | 23/23 (55.1.18) |
| 1.21.6 | 23/23 | — | 23/23 (21.6.20-beta) | 23/23 (56.0.9) |
| 1.21.8 | 23/23 | 23/23 (60) | 23/23 (21.8.57) | 23/23 (58.1.26) |
| 1.21.9 | 23/23 | — | 23/23 (21.9.16-beta) | 23/23 (59.0.5) |
| 1.21.10 | 23/23 | 23/23 (130) | 23/23 (21.10.64) | 23/23 (60.1.18) |
| 1.21.11 | 23/23 | 23/23 (132) | 23/23 (21.11.48) | 23/23 (61.2.4) |

Fabric servers ran Fabric Loader 0.20.1 with the newest Fabric API for that release.

## Differences bridged between releases

- 1.21.2: `EffectRenderingInventoryScreen` is gone (`showsActiveEffects()` instead), sprites take a
  render type and a color, `Registry.getHolder` became `get`.
- 1.21.5: `ClickEvent` / `HoverEvent` became records.
- 1.21.6: GUI render pipelines, deferred tooltips (drawn directly after `nextStratum()`), Forge moved
  to EventBus 7 (`Event.BUS`), mod-bus events via `getBus(modBusGroup)`.
- 1.21.9: `KeyMapping.Category`, mouse clicks as `MouseButtonEvent`, Forge tick events became records.
- 1.21.11: `ResourceLocation` is now `Identifier`, permission checks, variable-width effect list.
- NeoForge moved `sendToServer` to `ClientPacketDistributor` in 21.7; the vanilla
  `connection.send(new ServerboundCustomPayloadPacket(...))` is used on every version instead.
- Forge 51 (1.21) cannot inject the mod context into the constructor and has no
  `SystemMessageReceivedEvent`; there the chat list is not replaced in place (everything else works).

## Limits

The client extras (potion tooltips, clicking effects in the inventory, in-place chat list,
pause-all key) were compiled against every release but not run in a game client. Quilt, Spigot and
Purpur servers, Paper on 1.21.2/1.21.6/1.21.9, and 1.21.3/1.21.7 servers were not run.
