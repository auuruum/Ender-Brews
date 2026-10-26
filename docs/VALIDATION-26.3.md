# Minecraft 26.3 validation

Checked on Windows with Java 25 on 2026-10-26. The existing Minecraft 1.21.1 implementation
remains on the `1.21.1` branch. This port targets 26.3; it does not add 1.20.1 or 1.16.5 support.

## Runtime matrix

Every completed run checks the same 15 scenarios using a dedicated server and a real client.
The server loads the universal jar from `mods/` or `plugins/`. Modded clients are Gradle dev
clients; vanilla clients run without a loader or Ender Brews.

| Server | Version | Client with Ender Brews | Vanilla client |
| --- | --- | --- | --- |
| Fabric | Loader 0.19.5, Fabric API 0.165.0+26.3 | Fabric, 15/15 | 15/15 |
| Quilt | Loader 0.31.0-beta.4, Fabric API 0.165.0+26.3 | Fabric, 15/15 | 15/15 |
| NeoForge | 26.3.0.99 | NeoForge, 15/15 | 15/15 |
| Forge | 66.1.0 | Forge, 15/15 | 15/15 |
| Paper | 26.3 build 223 | Fabric, 15/15 | 15/15 |
| Spigot / Bukkit API | 26.3, BuildTools revision 4664 | Fabric, 15/15 | 15/15 |
| Purpur | 26.3 build 2654 | Fabric, 15/15 | 15/15 |

The baseline matrix totals 210 checks over 14 completed runs during port validation.
An additional Forge client → Paper plugin run passed all 15 checks. The user manually
confirmed inventory Pause/Resume on Forge → Forge, then inventory Pause/Resume, effect-hover
tooltips and replacement of the chat list on Forge → Paper after the client fixes below.

The checks cover startup and joining, the eight-potion threshold, removing the eighth potion,
temporary Strength II above endless Strength I, removing the hidden endless copy while keeping
the temporary effect, restoring effects after `/effect clear`, natural expiration of the stronger
effect, shulker contents, excluding poison, pause, resume, chat delivery, upgrading to endless
Strength II, and absence of Ender Brews errors in client/server logs.

## Build and packaging

- `./gradlew build :bukkit:smokeCommandsJar` succeeds with Java 25.
- All 19 shared JUnit tests pass: 16 rule/configuration tests and 3 protocol tests.
- The universal jar contains all four entry points and loader/plugin metadata.
- Test-only `SmokeCommands` classes are excluded from the universal jar.
- The resource metadata covers the 26.3 resource and data pack formats.

`smokeCommandsJar` is only a test fixture. It lets the console dispatch a Bukkit command with
a real Player sender; `/execute as` supplies a proxy sender to Bukkit and cannot exercise the
normal player-only command path. Production plugin command behavior was preserved.

## Port fixes

- Updated Java/Gradle, loaders, Minecraft APIs, networking names and GUI extraction hooks for
  26.3. The official [Fabric porting notes](https://fabricmc.net/2026/09/15/263.html) describe
  this generation of the toolchain.
- Enabled merged source sets for ForgeGradle 7 dev runs. Without this, Forge found the mod
  metadata in the resources directory but could not find the mod classes.
- Set the Fabric Loader minimum to 0.19.5. Quilt 0.31.0-beta.4 supplies that compatibility
  version and rejects the previous unnecessarily strict 0.20.1 requirement.
- Disabled the whitelist explicitly in disposable test servers, bound them to localhost,
  preserved existing manual worlds, and added cleanup and failure logs to the runner.
- Waited for the potion recount before checking immediate effect restoration, removing a
  timing race in the original smoke test.
- Replaced the old left-mouse value `0` with `InputConstants.MOUSE_BUTTON_LEFT` (`1` in 26.3).
- Removed the overlay filter that discarded server chat messages on Forge 26.3: Forge passes
  the remote-message flag through that accessor. The shared marker check selects our lists.
- Extracted effect-hover tooltips directly because loader post-render hooks run after the
  screen has already flushed deferred tooltips.
- Loaded missing Forge default server configs on vanilla-server login when Forge's own
  config remains unloaded. Forge 66.1.0 places its normal setup in the optional code-of-conduct
  handler; the guarded fallback fixes Forge client → Paper without touching loaded configs.

## Limits

This is Windows validation. Native Quilt clients, Linux/macOS, singleplayer, potion-item tooltip
appearance and the pause-all key were not manually rechecked on 26.3. Computer Use was stopped
with Escape and the user chose to perform the inventory/chat/hover checks manually.
The effect-clear scenario uses the command, not physical milk/totem interaction.

Vanilla dev-client startup initially failed with native Windows exits 0xC0000005/0xC0000409
before connection. Cleaning old 1.21.1 launch files did not reliably fix it. After adding the
26.3 manifest JVM arguments (`StackShadowPages=32`, native access and the internal-misc export),
Paper, Quilt, Purpur and Spigot vanilla runs all passed. Each platform uses its own vanilla
game directory. Clean generated files when switching Minecraft-version branches.

The empty-list welcome seen during testing was caused by setup commands, not a join hook.
Setup now clears only the disposable test pause state and does not issue those commands.

Updating an existing list on an unmodified vanilla chat client remains a separate UX question.
Current behavior resends the list for vanilla clients; modded clients replace the previous copy.

Reproduction and setup: [in-game tests](../tools/ingame-test/README.md). Logs are written under
`tools/ingame-test/servers26/` and are excluded from Git.
