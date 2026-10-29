# Ender Brews: design

> Keep enough potions of the same kind in your Ender Chest and you get their effect forever.
> Inspired by Fargo's Mutant Mod (Terraria): 30 potions in the piggy bank = a permanent buff.

## 1. The main rule

- The source is **only the player's Ender Chest**.
  - Potions lying directly in the Ender Chest slots count.
  - Potions inside **shulker boxes that sit in the Ender Chest** count too.
  - The player inventory, normal chests, backpacks and bundles do **not** count.
- Potions are **not drunk**. The effect is passive: while the potions are in the Ender Chest the effect is active, take them out and it's gone.
- Threshold: **8 potions** of one effect and level by default. Any number in the config (1, 16, 32, 64…). Below the threshold nothing happens, no partial bonuses.
- **Everything is configurable** (section 7).
- The potion stack size is **not changed** (stays 1), so shulker boxes are the main way to store them: one shulker = 27 potions.

### Why the stack size is not changed
- The mod can run **server-side only**, players with a vanilla client install nothing. The vanilla effect icon with "∞" is already in the game.
- No conflicts with other mods that change stack sizes, or with the brewing stand.

## 2. How potions are counted

1. Walk the 27 slots of the Ender Chest. For a shulker box, look inside (one level, vanilla does not allow a shulker inside a shulker).
2. Every potion adds +1 to the counter of each of its effects, per level.
   - Normal and extended versions (Strength 3:00 and 8:00) share **one counter**, duration doesn't matter.
   - Strength I and Strength II are **separate counters**.
   - **A stronger potion also counts for the weaker levels** (a Strength II potion is +1 Strength II and +1 Strength I). With threshold 8: 4× Strength II + 4× Strength I = endless Strength I.
3. Of all levels of one effect that reach the threshold, the **highest** is applied.
4. Only **regular (drinkable) potions** count. Splash, lingering and tipped arrows don't: people keep those in the Ender Chest as weapons. (Config option.)
5. Potions from other mods work automatically: effects are read from the item itself.

## 3. Which effects can become endless

| Effect | Endless? | Why |
|---|---|---|
| Beneficial effects with a duration: speed, strength, jump boost, regeneration, fire resistance, water breathing, night vision, invisibility, slow falling, luck, resistance | Yes | The point of the mod |
| Instant: healing, harming | No | No duration; endless healing would be immortality |
| Harmful: poison, weakness, slowness | No (option) | People keep weakness potions to cure zombie villagers, you must not suddenly get endless weakness |
| 1.21 potions with side effects: infested, oozing, weaving, wind charged | No | Endless silverfish out of the player on every hit |
| Turtle Master (slowness IV + resistance III) | Only resistance III | The harmful part is dropped, the beneficial part works |
| Water, awkward, mundane, thick | Not counted | No effects |

The server config has a blacklist of effects (for example, ban invisibility on a PvP server).

## 4. How the effect is applied

- The duration is **infinite**: the icon shows "∞", no night vision flicker.
- **No particles** (otherwise the player is always bubbling and invisibility gives itself away). The HUD icon stays.
- The mod remembers **which effects it gave** the player and removes only those. Other effects (from potions, beacons, commands, other mods) are never touched.
- Checked once per second, and right away when the Ender Chest is closed, on join, respawn and dimension change. The load is tiny: at most 27 + 27×27 = 756 items per player per second.

## 5. Edge cases

### Interaction with other effects of the same type
| Situation | What happens |
|---|---|
| Endless Strength I, the player **drinks Strength II** (3:00) | Strength II is active for 3 minutes. When it ends, **endless Strength I comes back by itself**. Nothing is lost |
| Endless Strength I, the player drinks **Strength I** | The potion does nothing (the endless effect already outlasts it). Vanilla behavior, like drinking a second identical potion |
| Endless Strength II, drinks Strength I | Nothing changes, Strength II stays |
| Endless Strength I, Strength II from a potion, and **the player takes the Strength I potions out of the chest at that moment** | Strength II runs out, then endless Strength I **does not come back**. An important trap: vanilla hides the weaker effect "under" the stronger one and restores it afterwards, so the mod has to clear the hidden endless copy |
| Endless Strength I + a **beacon** with Strength II | The beacon's Strength II is active; walk away from the beacon and endless Strength I is back |
| Endless Strength II + a beacon with Strength I | The beacon adds nothing |
| 16 Strength II appear in the chest while the player has endless Strength I | Endless Strength I is replaced by endless Strength II |
| The count drops below the threshold (took 1 potion out of 8) | The effect is removed right away, no delay or "buffer" |

### Clearing effects
| Situation | What happens |
|---|---|
| **Milk** | Removes all effects (poison, bad omen, etc.); the mod restores the endless ones on the very next tick (1/20 s), not noticeable. Milk stays useful |
| **Totem of Undying** | Vanilla clears all effects, endless ones return on the next tick, the totem adds its own (regeneration, absorption, fire resistance) as usual |
| Honey | Removes only poison, as in vanilla |
| `/effect clear` by an admin | Removes everything, endless effects return within a second (an admin can disable the mod for a player or an effect in the config) |
| A harmful effect from an enemy (witch, wither) | Works as usual, endless effects don't interfere |

### Player lifecycle
| Situation | What happens |
|---|---|
| Death and respawn | The Ender Chest isn't lost, so effects are given **right on respawn** |
| Dimension change (Nether, End, leaving the End) | Effects carry over, checked right after the transfer |
| Leaving the server | Endless effects are **removed on logout** and given again on join. Otherwise players would keep Strength forever after the mod is removed |
| Server crash (effects had been saved) | On join the mod recounts everything and removes the extras |
| Removing the mod | Safe: after the next join the effects are gone (except crash + removal right away) |
| Creative | Works as usual |
| Spectator | Not given (effects are pointless for spectators) |

### Ender Chest contents
| Situation | What happens |
|---|---|
| The player has the Ender Chest open and moves potions around | The counter updates once per second, finally on close |
| A shulker box with potions is taken out of the Ender Chest | Its potions no longer count |
| Potions in a bundle inside the Ender Chest | Not counted (a bundle holds 1 potion, no point) |
| Shulker in a shulker / a bag from another mod | Not counted |
| Renamed potions (anvil) | Counted by effects, the name doesn't matter |
| Potions made by command with custom effects | Counted by effects, same rules |
| A potion with several effects | +1 to each of its effects |
| Other mods change the Ender Chest (portable Ender Chest, hoppers from mods) | Works, the mod reads the contents and doesn't watch for opening |
| Every player | Own Ender Chest, own endless effects, no influence on each other |

### Balance
- At threshold 8 the price is about 3 brews (3 bottles each). Cheap, so on hardcore servers an admin sets 16/32/64.
- The strongest candidates are Regeneration II and Strength II. An admin can raise the threshold or ban the effect.
- No duplication: the mod creates and spends nothing, it only reads.

## 6. Pausing effects

Why: endless invisibility gets in the way when playing with friends, slow falling when building, night vision when looking at a sunset. Taking the potions out of the chest for that is annoying.

### Three ways, from the most convenient to the fallback
1. **Click the effect icon in the inventory** (when the mod is on the client). The vanilla inventory already shows the effect list on the right. Click an endless effect to pause it: the icon dims and shows "⏸" instead of "∞". Click again to resume. Nothing new to learn.
2. **A "pause all endless effects" key** (unbound by default, set it in Controls). One press turns everything off / on.
3. **A chat command**, works even for players without the mod on the client. `/enderbrews` prints a list:
   ```
   Strength II    ∞  [Off]
   Invisibility   ⏸  [On]
   Speed          5/8 potions
   ```
   `[On]` / `[Off]` are clickable buttons right in chat (vanilla can do this), no need to type the command.

### Pause rules
- A pause is **saved**: it survives death, logging out and dimension changes.
- Paused potions **still count**, the effect just isn't given.
- Take the potions out and put them back and the pause stays (the player set it). It is reset only by hand.
- A pause removes only the mod's endless effect. If the same effect from a drunk potion is active at that moment, it runs out as usual.
- An admin can disable pausing in the config (`allow_pause`).

## 6.1 Client side (optional)

The mod fully works on the server alone. The same jar on the client adds small conveniences, both on a server with the mod and on a server with the plugin:

- **The `/enderbrews` list updates in place.** The server tags the list with a hidden marker and a client with the mod removes the old copy when a new one arrives. Players without the mod just get the new list below.
- **Click an endless effect** next to the inventory to pause it. Paused effects are drawn grey under the vanilla list, click them to turn them back on.
- **Potion tooltips:** "Strength: 5/8 in Ender Chest", "∞ Strength: endless", "⏸ … paused", "can't become endless".
- **A key** to pause / resume all endless effects (unbound by default).

How it works: after each recount the server sends clients with the mod an `enderbrews:status` packet (effect list, counters, pause), only if something changed. The client sends `enderbrews:pause` (one effect or all). The format is plain bytes (`core/Protocol`), the same for the mod and the plugin. Players without the mod don't get these packets, and a client with the mod on a server without Ender Brews simply does nothing.

## 7. Config (server side, everything is configurable)

File `config/enderbrews.toml` (mod) or `plugins/EnderBrews/enderbrews.toml` (plugin), created automatically with comments. Reload without a restart: `/enderbrews reload`.

| Option | Default | What it does |
|---|---|---|
| `threshold` | 8 | How many potions of one effect and level are needed (any number ≥ 1) |
| `threshold_overrides` | `[]` | Own threshold for an effect, e.g. `["minecraft:regeneration=32"]` |
| `count_shulker_boxes` | true | Count potions in shulker boxes inside the Ender Chest |
| `count_splash_and_lingering` | false | Count splash and lingering potions |
| `extra_potion_items` | `[]` | Extra items with effects (potions from mods): id or `#tag` |
| `higher_level_counts_for_lower` | true | Strength II also counts for Strength I |
| `max_level` | 0 | Highest level of an endless effect (1 = I); 0 = no limit |
| `max_endless_effects` | 0 | How many endless effects at once; 0 = no limit |
| `allow_harmful_effects` | false | Allow harmful effects |
| `blacklist` | infested, oozing, weaving, wind charged | Banned effects: id or `#tag` |
| `whitelist` | `[]` | If not empty, only these effects are allowed |
| `always_allowed` | `[]` | Allow an effect even if the game calls it harmful |
| `show_particles` | false | Particles around the player |
| `show_icon` | true | The effect icon with "∞" |
| `remove_on_logout` | true | Remove on logout and give on join (safe mod removal) |
| `apply_in_spectator` | false | Give to spectators |
| `allow_pause` | true | Let players pause through `/enderbrews` |
| `check_interval_ticks` | 20 | How often the Ender Chest is recounted (20 = 1 s) |

Instant effects (healing, harming) are never endless, this isn't configurable. Milk and the totem have no setting of their own: endless effects always come back.
