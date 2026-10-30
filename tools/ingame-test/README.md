# In-game tests

## Minecraft 1.20.1 (this branch)

`test.js` starts a real dedicated server with the jar and drives it with a headless Mineflayer bot
(no game window). It checks thresholds, stronger potions on top, shulker boxes, harmful effects,
`/enderbrews` with pause/resume, server replies in the language the client reports, the status/pause
packets a modded client would use and that endless effects are not saved.

With Java 17 (`JAVA` pointing to its `java`), from this directory:

1. `npm install`
2. `./install.sh fabric paper forge neoforge` installs disposable servers under
   `servers120/1.20.1/` and accepts the Minecraft EULA in them.
3. Build the jar (`./gradlew build` in the repository root), then `./run.sh [platform...]` copies it
   into each installed server and runs `test.js`. Results are appended to `servers120/results.txt`;
   one platform without copying the jar: `node test.js fabric`.

The test overwrites each server's `server.properties`, binds only to `127.0.0.1:25599` and deletes
its `world`. Do not point it at a server you care about.

The client extras (tooltips, inventory clicks, chat list replacement) are not covered by these runs.
