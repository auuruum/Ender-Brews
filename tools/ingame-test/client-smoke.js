// Smoke test for the optional client side: starts a test server, launches the matching dev client
// (./gradlew :<platform>:runClient) straight into it, drives the endless effects from the server
// console and checks that both sides stay free of Ender Brews errors.
// Usage: node client-smoke.js <fabric|neoforge|forge>
const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

const JAVA = process.env.JAVA || 'java';
const ROOT = process.env.EB_SERVERS || path.join(__dirname, 'servers');
const REPO = path.join(__dirname, '..', '..');
const PORT = 25599;
const platform = process.argv[2];
const SERVERS = {
  fabric: ['-jar', 'server.jar', 'nogui'],
  neoforge: ['@libraries/net/neoforged/neoforge/21.1.80/win_args.txt', 'nogui'],
  forge: ['@libraries/net/minecraftforge/forge/1.21.1-52.0.28/win_args.txt', 'nogui'],
};
const sleep = ms => new Promise(r => setTimeout(r, ms));
let serverLog = '';
let clientLog = '';

function waitFor(get, regex, timeout) {
  return new Promise((resolve, reject) => {
    const start = Date.now();
    const timer = setInterval(() => {
      const m = get().match(regex);
      if (m) { clearInterval(timer); resolve(m); }
      else if (Date.now() - start > timeout) { clearInterval(timer); reject(new Error('timeout waiting for ' + regex)); }
    }, 250);
  });
}

async function main() {
  const cwd = path.join(ROOT, platform);
  fs.rmSync(path.join(cwd, 'world'), { recursive: true, force: true });
  fs.writeFileSync(path.join(cwd, 'server.properties'),
    `online-mode=false\nserver-port=${PORT}\nlevel-type=minecraft\\:flat\nspawn-protection=0\ngenerate-structures=false\nspawn-monsters=false\n`);
  const server = spawn(JAVA, ['-Xmx2G', ...SERVERS[platform]], { cwd });
  server.stdout.on('data', d => { serverLog += d; });
  server.stderr.on('data', d => { serverLog += d; });
  await waitFor(() => serverLog, /Done \([\d.,]+s\)!/, 600000);
  console.log('server up');

  const gradlew = '"' + path.join(REPO, process.platform === 'win32' ? 'gradlew.bat' : 'gradlew') + '"';
  const client = spawn(gradlew, [`:${platform}:runClient`, `-PquickPlay=127.0.0.1:${PORT}`, '--console=plain'],
    { cwd: REPO, shell: true, env: { ...process.env, JAVA_HOME: process.env.JAVA_HOME || path.dirname(path.dirname(JAVA)) } });
  client.stdout.on('data', d => { clientLog += d; });
  client.stderr.on('data', d => { clientLog += d; });
  client.on('exit', () => {
    if (!/joined the game/.test(serverLog)) {
      console.log('client exited early:\n' + clientLog.slice(-3000));
      process.exit(1);
    }
  });
  const name = (await waitFor(() => serverLog, /(\w+) joined the game/, 900000))[1];
  console.log('client joined as ' + name);
  await sleep(5000);

  const send = cmd => server.stdin.write(cmd + '\n');
  for (let i = 0; i < 8; i++) send(`item replace entity ${name} enderchest.${i} with minecraft:potion[minecraft:potion_contents={potion:"minecraft:strength"}]`);
  for (let i = 8; i < 16; i++) send(`item replace entity ${name} enderchest.${i} with minecraft:potion[minecraft:potion_contents={potion:"minecraft:swiftness"}]`);
  await sleep(2000);
  send(`execute as ${name} run enderbrews`);
  await sleep(1000);
  send(`execute as ${name} run enderbrews pause minecraft:speed`);
  await sleep(1500);
  send(`execute as ${name} run enderbrews resume minecraft:speed`);
  await sleep(1500);
  send(`give ${name} minecraft:potion[minecraft:potion_contents={potion:"minecraft:strength"}]`);
  await sleep(3000);

  send('stop');
  await Promise.race([new Promise(r => server.once('exit', r)), sleep(60000)]);
  await sleep(5000);
  spawn('taskkill', ['/pid', String(client.pid), '/T', '/F'], { shell: true });
  await sleep(3000);
  fs.writeFileSync(path.join(ROOT, `${platform}-client.log`), clientLog);

  const problems = (clientLog + serverLog).split('\n')
    .filter(l => /enderbrews/i.test(l) && /(ERROR|Exception|Caused by|WARN)/.test(l));
  const statusPackets = /enderbrews/i.test(clientLog);
  console.log(problems.length ? 'FAIL  errors:\n' + problems.join('\n') : 'PASS  no Ender Brews errors on client or server');
  console.log(`client log mentions enderbrews: ${statusPackets}`);
  process.exit(problems.length ? 1 : 0);
}

main().catch(e => { console.log('ERROR ' + e.stack); process.exit(1); });
