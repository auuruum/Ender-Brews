# Minecraft 1.20.1 validation

Port of the 1.21 feature set to 1.20.1, checked on Windows on 2026-10-30. One jar,
`EnderBrews-1.0.0+mc1.20.1.jar`, built against Fabric API 0.92.12, Forge 47.4.28 and the Spigot
1.20.1 API. The Forge code also compiles against the oldest Forge, 47.0.1.

## Server runs

`tools/ingame-test/run.sh`: a real dedicated server with the finished jar on Java 17, driven by a
headless Mineflayer bot, 24 checks per run (thresholds, stronger potions on top, removal,
`/effect clear`, shulker boxes, harmful effects, `/enderbrews` pause/resume, replies in the client's
language, status and pause packets as a modded client sends them, no endless effects saved on
logout, no Ender Brews errors in the log).

| Server | Version | Result |
| --- | --- | --- |
| Fabric | Loader 0.20.1, Fabric API 0.92.12 | 24/24 |
| Paper | build 196 | 24/24 |
| Forge | 47.4.28 | 24/24 |
| NeoForge | 47.1.106 | 24/24 |

## Client runs

A real Fabric and a real Forge dev client (no bot) joined a server running the finished jar and ran a
temporary self-test: status packet received, potion tooltip line, two `/enderbrews` leave one list in
chat, clicking an effect in the inventory pauses it, clicking the greyed-out row resumes it, the
pause-all key works both ways. Screenshots of the inventory were checked by eye.

| Client | Server | Result |
| --- | --- | --- |
| Fabric | Fabric | all passed |
| Fabric | Paper | all passed |
| Forge | Forge | all passed |
| Forge | Paper | all passed |

The very first Forge client run stalled while joining a Forge server through the automatic quick-play
connect and was disconnected after 30 seconds. It did not happen again in four more Forge runs, two of
them from a fresh client directory. The cause is unknown.

## Differences bridged from 1.21

- No data components: potions are read with `PotionUtils`, shulker box contents from `BlockEntityTag`.
- No `Holder<MobEffect>` in effect instances and no `CustomPacketPayload`: packets are plain
  `ResourceLocation` channels on every loader, Fabric's receivers hop to the main thread themselves.
- Vanilla does not keep the client's language on the server: Fabric gets it from a small mixin on
  `ServerPlayer.updateOptions`, Forge has `getLanguage()`.
- Forge runs on SRG names: the jar is reobfuscated with ModDevGradle Legacy and the private chat list
  is found by its SRG name, `f_93760_`.
- Bukkit 1.20.1 has no effect categories, `Registry.EFFECT` or `getBasePotionType`: harmful effects
  are a fixed list of the vanilla ones, base potions go through `PotionBrewer`.
- The inventory effect background still comes from `inventory.png`, not from sprites.

## Limits

The client runs used dev builds, not the finished jar. Quilt, Spigot and Purpur servers and NeoForge
clients were not run. Fabric Loader older than 0.20.1 was not run; the jar allows 0.14.21, and Fabric
API 0.92.12 itself needs 0.16.10.
