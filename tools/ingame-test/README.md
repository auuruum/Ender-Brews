# In-game tests

## Minecraft 26.3

`client-smoke.js` starts a dedicated server with the universal jar and joins it with a real dev
client. Quilt and Bukkit-based servers are joined with the Fabric client to check compatibility.
It checks potion thresholds and removal, stronger temporary effects and their expiration,
hidden-effect removal, effect restoration after clearing, shulker contents, harmful effects,
pause/resume, stronger endless effects, chat delivery and Ender Brews errors in both logs.

1. Build with Java 25: `./gradlew build :bukkit:smokeCommandsJar`.
   When switching from a different Minecraft-version branch, use `./gradlew clean build
   :bukkit:smokeCommandsJar` first to remove obsolete generated launch files.
2. Install servers under `servers26/<platform>`:
   - Fabric: `server.jar` from https://fabricmc.net/use/server/ and Fabric API in `mods/`.
   - NeoForge 26.3.0.99: run its installer with `--installServer`.
   - Forge 26.3-66.1.0: run its installer with `--installServer`.
   - Paper 26.3: put the server jar at `paper/paper.jar`.
   - Quilt 0.31.0-beta.4: `quilt-server-launch.jar`, the installed vanilla server and Fabric API
     in `quilt/mods/`.
   - Spigot / Purpur 26.3: put the server jar at `spigot/server.jar` / `purpur/server.jar`.
3. Accept the Minecraft EULA in each test server's `eula.txt` and copy the **same**
   `build/libs/EnderBrews-1.0.0+mc26.3.jar` into its `mods/` or `plugins/` directory.
4. Run one platform at a time: `node client-smoke.js fabric`, then `quilt`, `neoforge`, `forge`,
   `paper`, `spigot`, `purpur`.
   Repeat with `--vanilla` to verify a client with no mod: `node client-smoke.js fabric --vanilla`.

PowerShell example:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-25'
$env:JAVA = "$env:JAVA_HOME\bin\java.exe"
node client-smoke.js fabric
```

`EB_SERVERS` overrides the server directory. `NEOFORGE_VERSION` and `FORGE_VERSION` override
the installed loader versions. The first dev-client launch needs network access to download
the loader's client dependencies.

For Paper, Spigot and Purpur, the runner installs `bukkit/build/libs/enderbrews-smoke-commands.jar` on the disposable
server. This console-only test helper dispatches commands with a real `Player` sender, because
`execute as` still uses a proxy sender for Bukkit commands. It is excluded from the universal jar.

The test overwrites the test server's `server.properties`, disables its whitelist, binds only
to `127.0.0.1:25599`, and uses `smoke-world`. It clears the test player's Ender Chest at startup;
do not point it at a production server directory. Existing `world` directories are preserved.
Client and server logs are saved under the server root even on failure (vanilla runs use a
`-vanilla` suffix). Shutdown stops only
the processes launched by the test.

Vanilla clients use a separate `shared/runs/smoke-<platform>-vanilla/` directory per platform.
The manual `shared/runs/vanilla-client/` profile is preserved.
The disposable server's saved pause state is reset before each run. Test servers use Peaceful.

Set `EB_CLIENT=forge` (or `fabric`, `neoforge`) to choose a different modded client, for example
to test Forge → Paper. The default client follows the server loader, using Fabric for Quilt
and Bukkit-based servers.

For UI inspection, set `EB_INSPECT` to the absolute path of a **nonexistent** file before
starting the test. After the checks it grants the test player operator permissions and waits
up to ten minutes. Create that file to finish and shut down. Inspect potion tooltips,
inventory effect clicks and chat replacement in the Minecraft window during this pause.
During inspection, commands written into `<EB_INSPECT>.commands` are forwarded to the test
server console, then the command file is emptied.

## Legacy bot tests

`test.js` uses Mineflayer and the 1.21.1 protocol. It is retained for the `1.21.1` branch;
it cannot join a 26.3 server with the currently installed Mineflayer version.
Run `npm install` to install its dependencies and use `servers/<platform>` for its test servers.
See the `1.21.1` branch for loader versions and setup instructions for that release.
