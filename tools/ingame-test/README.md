# In-game test

Starts a real 1.21.1 server with the universal jar, joins with a vanilla bot (mineflayer) and checks
the edge cases from `docs/DESIGN.md`: threshold, removal, stronger potions on top, milk, shulker
boxes, harmful effects, `/enderbrews` pause/resume, upgrades and that nothing is saved on logout.

1. `npm install`
2. Put servers in `servers/<platform>` (`fabric`, `neoforge`, `forge`, `paper`):
   - fabric: `server.jar` from https://fabricmc.net/use/server/ and Fabric API in `mods/`
   - neoforge / forge: run the installer with `--install-server` / `--installServer` in that folder
   - paper: `paper.jar` from https://papermc.io/downloads/paper
3. Accept the Minecraft EULA in each folder (`eula.txt`) and copy `build/libs/EnderBrews-*.jar`
   into `mods/` (or `plugins/` for Paper).
4. `node test.js fabric` (Java 21 on PATH, or `JAVA=/path/to/java`). The world is recreated on
   every run. The NeoForge/Forge launch args in `test.js` name the installed loader version.
