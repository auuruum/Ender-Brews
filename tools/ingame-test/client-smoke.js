// In-game test with a real dev client: starts a test server, launches a dev client
// (./gradlew :<loader>:runClient) straight into it, drives the endless effects from the server
// console, checks the effects with /data and that both sides stay free of Ender Brews errors.
// Needed for Minecraft versions the mineflayer bot (test.js) does not speak yet.
// Usage: node client-smoke.js <platform> [--vanilla]; plugin servers and Quilt use the Fabric client.
// Env: JAVA (server Java), JAVA_HOME (Gradle Java), EB_SERVERS (servers folder),
//      NEOFORGE_VERSION, FORGE_VERSION (as installed, e.g. 26.3.0.99 and 26.3-66.1.0),
//      EB_GRADLE_ARGS (extra client Gradle args, e.g. -Pminecraft_version=26.2 for older 26.x),
//      EB_CLIENT_TASK (client Gradle task, e.g. :fabric:runProductionClient)
const { spawn, execFile } = require('child_process');
const fs = require('fs');
const path = require('path');

const JAVA = process.env.JAVA || 'java';
const ROOT = path.resolve(process.env.EB_SERVERS || path.join(__dirname, 'servers26'));
const REPO = path.join(__dirname, '..', '..');
const PORT = 25599;
const platform = process.argv[2];
const NEOFORGE = process.env.NEOFORGE_VERSION || '26.3.0.99';
const FORGE = process.env.FORGE_VERSION || '26.3-66.1.0';
const SERVERS = {
  fabric: ['-jar', 'server.jar', 'nogui'],
  paper: ['-jar', 'paper.jar', 'nogui'],
  quilt: ['-jar', 'quilt-server-launch.jar', 'nogui'],
  spigot: ['-jar', 'server.jar', 'nogui'],
  purpur: ['-jar', 'server.jar', 'nogui'],
  neoforge: [`@libraries/net/neoforged/neoforge/${NEOFORGE}/win_args.txt`, 'nogui'],
  forge: [`@libraries/net/minecraftforge/forge/${FORGE}/win_args.txt`, 'nogui'],
};
const CLIENT = process.env.EB_CLIENT || { fabric: 'fabric', quilt: 'fabric', paper: 'fabric', spigot: 'fabric', purpur: 'fabric', neoforge: 'neoforge', forge: 'forge' }[platform];
if (!Object.hasOwn(SERVERS, platform) || !['fabric', 'neoforge', 'forge'].includes(CLIENT)) {
  throw new Error('Usage: node client-smoke.js <fabric|quilt|neoforge|forge|paper|spigot|purpur> [--vanilla]');
}
const bukkit = ['paper', 'spigot', 'purpur'].includes(platform);
const vanilla = process.argv.includes('--vanilla');
const RUN = platform + (vanilla ? '-vanilla' : '');
const sleep = ms => new Promise(r => setTimeout(r, ms));
const results = [];
let serverLog = '';
let clientLog = '';
let server;
let client;
let processError;
let stopping = false;

function check(name, ok, detail) {
  results.push(ok);
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${ok ? '' : '  -> ' + detail}`);
}

function waitFor(get, regex, timeout, from = 0) {
  return new Promise((resolve, reject) => {
    const start = Date.now();
    const timer = setInterval(() => {
      const m = get().slice(from).match(regex);
      if (m) { clearInterval(timer); resolve(m); }
      else if (processError) { clearInterval(timer); reject(processError); }
      else if (Date.now() - start > timeout) { clearInterval(timer); reject(new Error('timeout waiting for ' + regex)); }
    }, 100);
  });
}

const send = cmd => server.stdin.write(cmd + '\n');

async function killTree(child) {
  if (!child?.pid || child.exitCode !== null || child.signalCode !== null) return;
  if (process.platform === 'win32') {
    await new Promise(resolve => execFile('taskkill', ['/pid', String(child.pid), '/T', '/F'], resolve));
  } else {
    child.kill('SIGTERM');
  }
}

async function cleanup() {
  stopping = true;
  if (server && server.exitCode === null && server.signalCode === null && server.stdin.writable) {
    send('stop');
    await new Promise(resolve => {
      const timer = setTimeout(resolve, 15000);
      server.once('exit', () => { clearTimeout(timer); resolve(); });
    });
  }
  await killTree(server);
  await killTree(client);
  fs.writeFileSync(path.join(ROOT, `${RUN}-client.log`), clientLog);
  fs.writeFileSync(path.join(ROOT, `${RUN}-server.log`), serverLog);
}

async function effects(name) {
  const from = serverLog.length;
  send(`data get entity ${name} active_effects`);
  const m = await waitFor(() => serverLog, /(has the following entity data: (.*)|Found no elements matching active_effects)/, 10000, from);
  return m[2] || '';
}

/** Finds one effect in `data get` output; fields at their default value are left out there. */
function effect(data, id) {
  const compounds = [];
  let depth = 0, start = -1;
  for (let i = 0; i < data.length; i++) {
    if (data[i] === '{') { if (depth === 0) start = i; depth++; }
    else if (data[i] === '}') { depth--; if (depth === 0) compounds.push(data.slice(start, i + 1)); }
  }
  for (const c of compounds) {
    const top = c.replace(/hidden_effect: \{[^{}]*\}/, '');
    if (top.includes(`id: "minecraft:${id}"`)) {
      const num = k => { const m = top.match(new RegExp(k + ': (-?\\d+)')); return m ? +m[1] : 0; };
      const hidden = c.match(/hidden_effect: (\{[^{}]*\})/);
      return { amplifier: num('amplifier'), duration: num('duration'), ambient: num('ambient'), hidden: hidden ? hidden[1] : null };
    }
  }
  return null;
}

async function main() {
  const cwd = path.join(ROOT, platform);
  if (bukkit) {
    fs.copyFileSync(path.join(REPO, 'bukkit/build/libs/enderbrews-smoke-commands.jar'),
      path.join(cwd, 'plugins/enderbrews-smoke-commands.jar'));
  }
  const pauses = bukkit ? path.join(cwd, 'plugins/EnderBrews/paused.txt')
    : path.join(cwd, 'smoke-world/enderbrews-paused.txt');
  if (fs.existsSync(pauses)) fs.writeFileSync(pauses, '');
  // Use a dedicated disposable world and leave manually created worlds alone.
  fs.writeFileSync(path.join(cwd, 'server.properties'),
    `online-mode=false\nwhite-list=false\nenforce-whitelist=false\nserver-ip=127.0.0.1\nserver-port=${PORT}\ndifficulty=peaceful\nlevel-name=smoke-world\nlevel-type=minecraft\\:flat\nspawn-protection=0\ngenerate-structures=false\nspawn-monsters=false\nview-distance=3\nsimulation-distance=3\n`);
  server = spawn(JAVA, ['-Xmx2G', ...SERVERS[platform]], { cwd });
  server.on('error', e => { processError = e; });
  server.on('exit', code => { if (!stopping) processError = new Error(`server exited (${code})\n${serverLog.slice(-5000)}`); });
  const strip = d => d.toString().replace(/\x1b\[[0-9;]*m/g, '');
  server.stdout.on('data', d => {
    serverLog += strip(d);
    if (/not white-listed|not whitelisted/i.test(serverLog)) processError = new Error('test client rejected by whitelist');
  });
  server.stderr.on('data', d => { serverLog += strip(d); });
  await waitFor(() => serverLog, /Done \([\d.,]+s\)!/, 600000);
  check('server starts with the jar', true);

  const gradlew = '"' + path.join(REPO, process.platform === 'win32' ? 'gradlew.bat' : 'gradlew') + '"';
  const clientTask = process.env.EB_CLIENT_TASK || (vanilla ? ':shared:runVanillaClient' : `:${CLIENT}:runClient`);
  const clientArgs = [clientTask, `-PquickPlay=127.0.0.1:${PORT}`, '--console=plain'];
  if (vanilla) clientArgs.push(`-PvanillaGameDir=runs/smoke-${RUN}`);
  if (process.env.EB_GRADLE_ARGS) clientArgs.push(...process.env.EB_GRADLE_ARGS.split(' '));
  client = spawn(gradlew, clientArgs,
    { cwd: REPO, shell: true, env: { ...process.env, JAVA_HOME: process.env.JAVA_HOME || path.dirname(path.dirname(JAVA)) } });
  const onClientData = d => {
    clientLog += d;
    if (/has mods that were not found|Mod loading error has occurred/.test(clientLog)) {
      processError = new Error('client mod loading failed; see the saved client log');
    }
  };
  client.stdout.on('data', onClientData);
  client.stderr.on('data', onClientData);
  client.on('error', e => { processError = e; });
  client.on('exit', code => { if (!stopping) processError = new Error(`client exited (${code})\n${clientLog.slice(-5000)}`); });
  const name = (await waitFor(() => serverLog, /(\w+) joined the game/, 900000))[1];
  check(`${vanilla ? 'vanilla' : 'dev ' + CLIENT} client joins (${name})`, true);
  await sleep(5000);
  const command = text => send(bukkit ? `ebsmoke ${name} ${text}` : `execute as ${name} run ${text}`);
  // A new run starts with an empty chest even when the disposable world is reused.
  for (let i = 0; i < 27; i++) send(`item replace entity ${name} enderchest.${i} with minecraft:air`);
  send(`effect clear ${name}`);
  send(`gamemode creative ${name}`);
  await sleep(1500);

  const fill = (from, to, p) => { for (let i = from; i <= to; i++) send(`item replace entity ${name} enderchest.${i} with minecraft:potion[minecraft:potion_contents={potion:"minecraft:${p}"}]`); };
  fill(0, 7, 'strength');
  await sleep(1500);
  let s = effect(await effects(name), 'strength');
  check('8 Strength potions give endless Strength I', s && s.amplifier === 0 && s.duration === -1 && s.ambient === 1, JSON.stringify(s));

  send(`item replace entity ${name} enderchest.7 with minecraft:air`);
  await sleep(1500);
  s = effect(await effects(name), 'strength');
  check('7 potions: effect is gone', s === null, JSON.stringify(s));

  fill(7, 7, 'strength');
  await sleep(1500);
  send(`effect give ${name} minecraft:strength 30 1`);
  await sleep(300);
  s = effect(await effects(name), 'strength');
  check('Strength II from a potion goes on top, endless I hidden underneath',
    s && s.amplifier === 1 && s.duration > 0 && s.hidden && /duration: -1/.test(s.hidden), JSON.stringify(s));
  send(`item replace entity ${name} enderchest.7 with minecraft:air`);
  await sleep(1500);
  s = effect(await effects(name), 'strength');
  check('removing potions preserves Strength II and removes the hidden endless copy',
    s && s.amplifier === 1 && s.duration > 0 && !s.hidden, JSON.stringify(s));
  fill(7, 7, 'strength');
  await sleep(1500);
  send(`effect clear ${name}`);
  await sleep(500);
  s = effect(await effects(name), 'strength');
  check('milk / effect clear: endless effect comes right back', s && s.duration === -1 && s.amplifier === 0, JSON.stringify(s));
  send(`effect give ${name} minecraft:strength 1 1`);
  await sleep(2000);
  s = effect(await effects(name), 'strength');
  check('endless Strength I returns after a stronger temporary effect expires',
    s && s.duration === -1 && s.amplifier === 0, JSON.stringify(s));

  const shulkerItems = Array.from({ length: 8 }, (_, i) =>
    `{slot:${i},item:{id:"minecraft:potion",count:1,components:{"minecraft:potion_contents":{potion:"minecraft:swiftness"}}}}`).join(',');
  send(`item replace entity ${name} enderchest.10 with minecraft:shulker_box[minecraft:container=[${shulkerItems}]]`);
  fill(11, 18, 'poison');
  await sleep(1500);
  const data = await effects(name);
  check('potions in a shulker box in the Ender Chest count', effect(data, 'speed')?.duration === -1, data);
  check('harmful effects are not made endless', effect(data, 'poison') === null, data);

  command('enderbrews pause minecraft:speed');
  await sleep(1500);
  check('pausing removes the endless effect', effect(await effects(name), 'speed') === null, 'speed still there');
  command('enderbrews resume minecraft:speed');
  await sleep(1500);
  check('resuming brings it back', effect(await effects(name), 'speed')?.duration === -1, 'speed missing');
  command('enderbrews');
  send(`give ${name} minecraft:potion[minecraft:potion_contents={potion:"minecraft:strength"}]`);
  await sleep(3000);
  check('client shows the /enderbrews list', /\[CHAT\].*Ender Brews/.test(clientLog), 'no list in client log');

  fill(0, 7, 'strong_strength');
  await sleep(1500);
  s = effect(await effects(name), 'strength');
  check('8 Strength II potions upgrade to endless Strength II', s && s.amplifier === 1 && s.duration === -1, JSON.stringify(s));

  if (process.env.EB_INSPECT) {
    send(`op ${name}`);
    send(`gamemode survival ${name}`);
    console.log(`UI inspection ready (${name}); create ${process.env.EB_INSPECT} to finish.`);
    const inbox = process.env.EB_INSPECT + '.commands';
    await waitFor(() => {
      if (fs.existsSync(inbox)) {
        const commands = fs.readFileSync(inbox, 'utf8');
        if (commands.trim()) { fs.writeFileSync(inbox, ''); send(commands.trim()); }
      }
      return fs.existsSync(process.env.EB_INSPECT) ? 'done' : '';
    }, /done/, 600000);
  }
  await cleanup();

  const problems = (clientLog + serverLog).split('\n')
    .filter(l => /enderbrews/i.test(l) && /(ERROR|Exception|Caused by|WARN)/.test(l));
  check('no Ender Brews errors on client or server', problems.length === 0, problems.join('\n'));
  const failed = results.filter(ok => !ok).length;
  console.log(`\n${RUN}: ${results.length - failed}/${results.length} passed`);
  process.exitCode = failed ? 1 : 0;
}

main().catch(async e => {
  console.log('ERROR ' + e.stack);
  await cleanup();
  process.exitCode = 1;
});
