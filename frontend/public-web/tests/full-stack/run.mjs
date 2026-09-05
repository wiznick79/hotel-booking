import { mkdtempSync, writeFileSync, unlinkSync, rmdirSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { generateKeyPairSync, randomUUID } from 'node:crypto';
import { execFileSync, spawnSync } from 'node:child_process';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');
const work = mkdtempSync(join(tmpdir(), 'hb-browser-'));
const project = `hb-e2e-${randomUUID().slice(0, 8)}`;
const emptyEnv = join(work, 'empty.env');
const configFile = join(work, 'compose.json');
writeFileSync(emptyEnv, '');
// A fresh test-only key pair. Never read the developer's .env or credentials.
const { privateKey, publicKey } = generateKeyPairSync('rsa', { modulusLength: 2048 });
const env = Object.fromEntries(Object.entries(process.env).filter(([key]) =>
  /^(path|systemroot|windir|temp|tmp|home|userprofile|localappdata|appdata|programfiles|programdata|comspec|pathext|docker_host|docker_context|docker_config)$/i.test(key)));
Object.assign(env, {
  JWT_PRIVATE_KEY_BASE64: privateKey.export({ type: 'pkcs8', format: 'der' }).toString('base64'),
  JWT_PUBLIC_KEY_BASE64: publicKey.export({ type: 'spki', format: 'der' }).toString('base64'),
});
const compose = (...args) => execFileSync('docker', ['compose', '--project-name', project,
  '--env-file', emptyEnv, '-f', configFile, ...args], { cwd: root, env, encoding: 'utf8', maxBuffer: 32 * 1024 * 1024 });
let started = false;
try {
  const config = JSON.parse(execFileSync('docker', ['compose', '--env-file', emptyEnv,
    '-f', join(root, 'docker-compose.yml'), 'config', '--format', 'json'], { env, cwd: work, encoding: 'utf8' }));
  delete config.name;
  delete config.volumes;
  delete config.services.prometheus;
  delete config.services.grafana;
  config.networks = { default: {} };
  for (const [name, service] of Object.entries(config.services)) {
    delete service.container_name;
    delete service.ports;
    delete service.volumes;
    service.networks = { default: null };
    if (['public-web', 'api-gateway', 'mailpit'].includes(name)) {
      const target = name === 'public-web' ? 80 : name === 'mailpit' ? 8025 : 8080;
      service.ports = [{ target, host_ip: '127.0.0.1' }];
    }
    if (name === 'postgres') {
      service.tmpfs = ['/var/lib/postgresql/data'];
      service.volumes = [{ type: 'bind', source: join(root, 'infrastructure/postgres/init'), target: '/docker-entrypoint-initdb.d', read_only: true }];
    }
  }
  writeFileSync(configFile, JSON.stringify(config));
  started = true;
  console.log(`Starting disposable stack ${project}`);
  const up = spawnSync('docker', ['compose', '--project-name', project, '--env-file', emptyEnv,
    '-f', configFile, 'up', '-d', '--build', '--wait', '--wait-timeout', '240'], { cwd: root, env, stdio: 'inherit' });
  if (up.status !== 0) throw new Error('Disposable stack failed to start.');
  const endpoint = (service, port) => `http://${compose('port', service, String(port)).trim()}`;
  const result = spawnSync(process.execPath, ['node_modules/@playwright/test/cli.js', 'test', '-c', 'playwright.full-stack.config.ts'], {
    cwd: join(root, 'frontend/public-web'), stdio: 'inherit',
    env: { ...env, E2E_BASE_URL: endpoint('public-web', 80), E2E_API_URL: endpoint('api-gateway', 8080),
      E2E_MAIL_URL: endpoint('mailpit', 8025), E2E_PRIVATE_KEY: env.JWT_PRIVATE_KEY_BASE64 },
  });
  process.exitCode = result.status ?? 1;
} finally {
  // Only the randomly named stack created above is eligible for cleanup.
  if (started && /^hb-e2e-[a-f0-9]{8}$/.test(project)) {
    console.log(`Removing disposable stack ${project}`);
    compose('down', '--volumes', '--remove-orphans', '--rmi', 'local');
  }
  for (const file of [configFile, emptyEnv]) {
    try { unlinkSync(file); } catch (error) { if (error.code !== 'ENOENT') throw error; }
  }
  rmdirSync(work);
}
