// In-game test of the universal Ender Brews jar on a real server, driven by a vanilla bot.
// Usage: node test.js <fabric|neoforge|forge|paper>
// Expects servers/<platform>/ with an installed 1.21.1 server, eula.txt and the jar in mods/ or plugins/
// (see README.md next to this file). Set JAVA to a Java 21 binary if `java` is not Java 21.
const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const mineflayer = require('mineflayer');
const nbt = require('prismarine-nbt');

const JAVA = process.env.JAVA || 'java';
const ROOT = process.env.EB_SERVERS || path.join(__dirname, 'servers');
const PORT = 25599;
const BOT = 'BrewBot';
const platform = process.argv[2];
const SERVERS = {
  fabric: ['-jar', 'server.jar', 'nogui'],
  paper: ['-jar', 'paper.jar', 'nogui'],
  neoforge: ['@libraries/net/neoforged/neoforge/21.1.80/win_args.txt', 'nogui'],
  forge: ['@libraries/net/minecraftforge/forge/1.21.1-52.0.28/win_args.txt', 'nogui'],
};
const LOADED = {
  fabric: /enderbrews 1\.0\.0/,
  neoforge: /enderbrews/i,
  forge: /Found valid mod file EnderBrews.* with \{enderbrews\}/,
  paper: /Enabling EnderBrews v1\.0\.0/,
};
const cwd = path.join(ROOT, platform);
const results = [];
const listeners = [];
let output = '';
let server;

const sleep = ms => new Promise(r => setTimeout(r, ms));

function check(name, ok, detail) {
  results.push({ name, ok, detail });
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${ok ? '' : '  -> ' + detail}`);
}

/** Waits for {@code regex} in the server output written after position {@code from}. */
function waitFor(regex, timeout = 240000, from = 0) {
  return new Promise((resolve, reject) => {
    const existing = output.slice(from).match(regex);
    if (existing) return resolve(existing);
    const timer = setTimeout(() => reject(new Error('timeout waiting for ' + regex)), timeout);
    listeners.push(() => {
      const m = output.slice(from).match(regex);
      if (m) { clearTimeout(timer); resolve(m); return true; }
      return false;
    });
  });
}

function send(cmd) { server.stdin.write(cmd + '\n'); }

async function effects() {
  const from = output.length;
  send(`data get entity ${BOT} active_effects`);
  const m = await waitFor(/(has the following entity data: (.*)|Found no elements matching active_effects)/, 10000, from);
  return m[2] || '';
}

/** Finds one effect in `data get` output. Effects are flat compounds except for hidden_effect. */
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
      // Fields at their default value (amplifier 0, ambient false) are left out of the output.
      const num = k => { const m = top.match(new RegExp(k + ': (-?\\d+)')); return m ? +m[1] : 0; };
      const hidden = c.match(/hidden_effect: (\{[^{}]*\})/);
      return { amplifier: num('amplifier'), duration: num('duration'), ambient: num('ambient'), hidden: hidden ? hidden[1] : null };
    }
  }
  return null;
}

// Minimal reader for the Ender Brews status packet (see shared/.../core/Protocol.java).
function decodeStatus(buf) {
  let pos = 0;
  const varInt = () => { let v = 0, s = 0, b; do { b = buf[pos++]; v |= (b & 0x7f) << s; s += 7; } while (b & 0x80); return v; };
  const str = () => { const n = varInt(); const t = buf.toString('utf8', pos, pos + n); pos += n; return t; };
  if (buf[pos++] !== 1) return null;
  const allowPause = buf[pos++] === 1;
  const entries = [];
  for (let i = varInt(); i > 0; i--) {
    entries.push({ id: str(), amplifier: varInt(), count: varInt(), threshold: varInt(),
      state: ['ACTIVE', 'PROGRESS', 'LIMITED', 'BLOCKED'][buf[pos++]], paused: buf[pos++] === 1 });
  }
  return { allowPause, entries };
}

function pausePacket(effectId, paused) {
  const parts = [Buffer.from([1, effectId ? 1 : 0])];
  if (effectId) { const id = Buffer.from(effectId, 'utf8'); parts.push(Buffer.from([id.length]), id); }
  parts.push(Buffer.from([paused ? 1 : 0]));
  return Buffer.concat(parts);
}

const potion = p =>`minecraft:potion[minecraft:potion_contents={potion:"minecraft:${p}"}]`;
function fill(from, to, p) { for (let i = from; i <= to; i++) send(`item replace entity ${BOT} enderchest.${i} with ${potion(p)}`); }
function clearSlot(i) { send(`item replace entity ${BOT} enderchest.${i} with minecraft:air`); }

async function main() {
  fs.rmSync(path.join(cwd, 'world'), { recursive: true, force: true });
  fs.writeFileSync(path.join(cwd, 'server.properties'),
    `online-mode=false\nwhite-list=false\nenforce-whitelist=false\nserver-ip=127.0.0.1\nserver-port=${PORT}\nlevel-type=minecraft\\:flat\nspawn-protection=0\ngenerate-structures=false\nspawn-monsters=false\nspawn-animals=false\nmotd=enderbrews-test\n`);
  server = spawn(JAVA, ['-Xmx2G', ...SERVERS[platform]], { cwd });
  const log = fs.createWriteStream(path.join(ROOT, `${platform}-run.log`));
  const onData = d => {
    const text = d.toString().replace(/\x1b\[[0-9;]*m/g, '');
    output += text;
    log.write(text);
    for (let i = listeners.length - 1; i >= 0; i--) if (listeners[i]()) listeners.splice(i, 1);
  };
  server.stdout.on('data', onData);
  server.stderr.on('data', onData);

  await waitFor(/Done \([\d.,]+s\)!/, 600000);
  check('server starts with the jar', true);
  const debugLog = path.join(cwd, 'logs', 'debug.log');
  const fullLog = output + (platform === 'forge' && fs.existsSync(debugLog) ? fs.readFileSync(debugLog, 'utf8') : '');
  check('Ender Brews is loaded', LOADED[platform].test(fullLog), 'not found in log');

  const messages = [];
  const rawMessages = [];
  const statuses = [];
  const bot = mineflayer.createBot({ host: '127.0.0.1', port: PORT, username: BOT, version: '1.21.1', auth: 'offline' });
  bot.on('message', m => { messages.push(m.toString()); rawMessages.push(JSON.stringify(m.json)); });
  bot._client.on('custom_payload', packet => {
    if (packet.channel === 'enderbrews:status') statuses.push(decodeStatus(packet.data));
  });
  const kicked = new Promise(r => bot.once('kicked', reason => r(JSON.stringify(reason))));
  const spawned = new Promise(r => bot.once('spawn', () => r(null)));
  const kick = await Promise.race([spawned, kicked, sleep(60000).then(() => 'timeout')]);
  check('vanilla client can join', kick === null, kick);
  if (kick !== null) return finish();
  await sleep(1500);

  fill(0, 7, 'strength');
  await sleep(1500);
  let s = effect(await effects(), 'strength');
  check('8 Strength potions give endless Strength I', s && s.amplifier === 0 && s.duration === -1 && s.ambient === 1, JSON.stringify(s));

  clearSlot(7);
  await sleep(1500);
  s = effect(await effects(), 'strength');
  check('7 potions: effect is gone', s === null, JSON.stringify(s));

  fill(7, 7, 'strength');
  await sleep(1500);
  send(`effect give ${BOT} minecraft:strength 30 1`);
  await sleep(300);
  s = effect(await effects(), 'strength');
  check('drinking Strength II puts it on top, endless I hidden underneath',
    s && s.amplifier === 1 && s.duration > 0 && s.hidden && /duration: -1/.test(s.hidden), JSON.stringify(s));

  clearSlot(7);
  await sleep(1500);
  s = effect(await effects(), 'strength');
  check('removing potions during Strength II drops the hidden endless copy',
    s && s.amplifier === 1 && s.duration > 0 && !s.hidden, JSON.stringify(s));

  fill(7, 7, 'strength');
  send(`effect clear ${BOT} minecraft:strength`);
  await sleep(1500);
  s = effect(await effects(), 'strength');
  check('endless Strength comes back after the potion is gone', s && s.amplifier === 0 && s.duration === -1, JSON.stringify(s));

  send(`effect clear ${BOT}`);
  await sleep(300);
  s = effect(await effects(), 'strength');
  check('milk / effect clear: endless effect comes right back', s && s.duration === -1, JSON.stringify(s));

  const shulkerItems = Array.from({ length: 8 }, (_, i) =>
    `{slot:${i},item:{id:"minecraft:potion",count:1,components:{"minecraft:potion_contents":{potion:"minecraft:swiftness"}}}}`).join(',');
  send(`item replace entity ${BOT} enderchest.10 with minecraft:shulker_box[minecraft:container=[${shulkerItems}]]`);
  await sleep(1500);
  s = effect(await effects(), 'speed');
  check('potions inside a shulker box in the Ender Chest count', s && s.duration === -1, JSON.stringify(s));

  fill(11, 18, 'poison');
  await sleep(1500);
  s = effect(await effects(), 'poison');
  check('harmful effects (poison) are not made endless', s === null, JSON.stringify(s));

  messages.length = 0;
  bot.chat('/enderbrews');
  await sleep(1000);
  const list = messages.find(m => /Ender Brews/.test(m)) || '';
  check('/enderbrews lists effects in one message', list.split('\n').length >= 3, JSON.stringify(messages));
  console.log('      /enderbrews output:\n        ' + list.split('\n').join('\n        '));

  messages.length = 0;
  bot.chat('/enderbrews pause minecraft:speed');
  await sleep(1500);
  s = effect(await effects(), 'speed');
  check('pausing removes the endless effect', s === null, JSON.stringify(s));
  const updated = messages.find(m => /Ender Brews/.test(m)) || '';
  check('pausing sends the list again with the new state', /⏸ Speed/.test(updated) && /\[Resume\]/.test(updated), JSON.stringify(messages));
  bot.chat('/enderbrews resume minecraft:speed');
  await sleep(1500);
  s = effect(await effects(), 'speed');
  check('resuming brings it back', s && s.duration === -1, JSON.stringify(s));

  fill(0, 7, 'strong_strength');
  await sleep(1500);
  s = effect(await effects(), 'strength');
  check('8 Strength II potions upgrade to endless Strength II', s && s.amplifier === 1 && s.duration === -1, JSON.stringify(s));

  // From here on the bot acts like a client that has the mod installed.
  check('vanilla client gets no status packets', statuses.length === 0, JSON.stringify(statuses));
  check('chat list carries the hidden marker for modded clients', rawMessages.some(m => m.includes('enderbrews:list')), rawMessages.join(' | '));
  bot._client.write('custom_payload', { channel: 'minecraft:register', data: Buffer.from('enderbrews:status\0enderbrews:pause') });
  await sleep(2000);
  let status = statuses[statuses.length - 1];
  const find = id => status && status.entries.find(e => e.id === `minecraft:${id}`);
  check('modded client receives a status packet', status && find('strength')?.state === 'ACTIVE' && find('strength').amplifier === 1
    && find('speed')?.state === 'ACTIVE' && find('poison')?.state === 'BLOCKED', JSON.stringify(status));

  bot._client.write('custom_payload', { channel: 'enderbrews:pause', data: pausePacket('minecraft:speed', true) });
  await sleep(1500);
  s = effect(await effects(), 'speed');
  status = statuses[statuses.length - 1];
  check('pause packet pauses the effect and the status follows', s === null && find('speed')?.paused === true, JSON.stringify({ s, status }));

  bot._client.write('custom_payload', { channel: 'enderbrews:pause', data: pausePacket(null, false) });
  await sleep(1500);
  s = effect(await effects(), 'speed');
  status = statuses[statuses.length - 1];
  check('resume-all packet brings everything back', s && s.duration === -1 && find('speed')?.paused === false, JSON.stringify({ s, status }));

  bot.quit();
  await sleep(2000);
  send('save-all flush');
  await sleep(2000);
  const dataDir = path.join(cwd, 'world', 'playerdata');
  const file = fs.readdirSync(dataDir).find(f => f.endsWith('.dat'));
  const { parsed } = await nbt.parse(zlib.gunzipSync(fs.readFileSync(path.join(dataDir, file))));
  const saved = nbt.simplify(parsed).active_effects || [];
  const leftovers = saved.filter(e => e.duration === -1);
  check('endless effects are not saved when the player leaves', leftovers.length === 0, JSON.stringify(saved));

  return finish();
}

async function finish() {
  send('stop');
  await Promise.race([new Promise(r => server.once('exit', r)), sleep(60000)]);
  if (server.exitCode === null) server.kill();
  const errors = output.split('\n').filter(l => /enderbrews/i.test(l) && /(ERROR|Exception|WARN)/.test(l));
  check('no Ender Brews errors or warnings in the log', errors.length === 0, errors.join(' | '));
  const failed = results.filter(r => !r.ok).length;
  console.log(`\n${platform}: ${results.length - failed}/${results.length} passed`);
  process.exit(failed ? 1 : 0);
}

main().catch(async e => { console.log('ERROR ' + e.stack); check('harness', false, e.message); await finish(); });
