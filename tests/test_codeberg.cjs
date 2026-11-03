"use strict";
const assert = require("node:assert/strict");
const {test} = require("node:test");
const fs = require("node:fs");
const path = require("node:path");
const os = require("node:os");
const crypto = require("node:crypto");
const {api, REPOSITORY} = require("../.forgejo/scripts/api.cjs");
const {verifyPublisher} = require("../.forgejo/scripts/identity.cjs");
const {verifyBundle} = require("../.forgejo/scripts/bundle.cjs");
const {verifyDiscovery} = require("../.forgejo/scripts/discovery.cjs");
const {load} = require("js-yaml");
function signedFixture() {
  const {privateKey, publicKey} = crypto.generateKeyPairSync("ed25519");
  const key = Buffer.from(publicKey.export({format: "jwk"}).x, "base64url").toString("base64");
  const zip = Buffer.alloc(22); zip.writeUInt32LE(0x06054b50);
  const signature = crypto.sign(null, Buffer.concat([Buffer.from("MorpheSig1"), crypto.createHash("sha256").update(zip).digest()]), privateKey);
  const comment = Buffer.from(`MorpheSig1 ${key} ${signature.toString("base64")}`);
  zip.writeUInt16LE(comment.length, 20);
  return {key, bytes: Buffer.concat([zip, comment])};
}

test("Codeberg API scopes authentication to the exact HTTPS host and refuses redirects", async () => {
  let options;
  assert.deepEqual(await api("/user", {token: "fixture-token", fetcher: async (url, opts) => {
    assert.equal(url, "https://codeberg.org/api/v1/user"); options = opts;
    return {ok: true, status: 200, json: async () => ({login: "JohninNL"})};
  }}), {login: "JohninNL"});
  assert.equal(options.headers.Authorization, "token fixture-token");
  assert.equal(options.redirect, "error");
  await assert.rejects(api("//other.invalid/user", {token: "fixture"}), /Invalid/);
  await assert.rejects(api("/user", {token: "bad\ntoken"}), /required/);
});

test("Codeberg publishing requires the owner's account", async () => {
  await assert.doesNotReject(verifyPublisher({env: {CODEBERG_TOKEN: "fixture"}, request: async () => ({login: "JohninNL"})}));
  await assert.rejects(verifyPublisher({env: {CODEBERG_TOKEN: "fixture"}, request: async () => ({login: "automation"})}), /must belong/);
  await assert.rejects(verifyPublisher({env: {}}), /JOHNINNL_CODEBERG_TOKEN/);
});

test("bundle verifier accepts the published raw Ed25519 format and rejects tampering", () => {
  const {key, bytes} = signedFixture();
  assert.match(verifyBundle(bytes, key), /^[a-f0-9]{64}$/);
  const changed = Buffer.from(bytes); changed[4] ^= 1;
  assert.throws(() => verifyBundle(changed, key), /signature/);
  assert.throws(() => verifyBundle(bytes, signedFixture().key), /identity/);
  assert.throws(() => verifyBundle(Buffer.alloc(22), key), /signed ZIP/);
});

test("Codeberg semantic config preserves app scopes and exact Git identity guard before push", () => {
  const config = require("../.forgejo/release.config.cjs");
  const plugins = config.plugins.map(p => Array.isArray(p) ? p[0] : p);
  assert.equal(config.repositoryUrl, `https://codeberg.org/${REPOSITORY}.git`);
  assert.deepEqual(config.branches, ["main", {name: "dev", prerelease: "dev", channel: "dev"}]);
  assert.equal(plugins[plugins.indexOf("./.github/scripts/public_git.cjs") - 1], "./.forgejo/scripts/identity.cjs");
  assert.ok(plugins.includes("./.github/scripts/verify_release_notes.cjs"));
  assert.ok(plugins.includes("./.forgejo/scripts/changelog.cjs"));
  assert.ok(plugins.includes("./.forgejo/scripts/publish.cjs"));
  assert.ok(!plugins.includes("./.github/scripts/public_github.cjs"));
});

test("Codeberg changelog creates real discoverable manifest without GitHub URL parsing", async () => {
  const cwd = fs.mkdtempSync(path.join(os.tmpdir(), "codeberg-changelog-"));
  try {
    const {key} = signedFixture(); fs.writeFileSync(path.join(cwd, "morphe-signing.pub"), key);
    await require("../.forgejo/scripts/changelog.cjs").prepare({}, {cwd, options: {repositoryUrl: `https://codeberg.org/${REPOSITORY}.git`}, nextRelease: {version: "1.2.3", notes: "Release notes."}});
    const manifest = JSON.parse(fs.readFileSync(path.join(cwd, "patches-bundle.json")));
    assert.equal(manifest.download_url, `https://codeberg.org/${REPOSITORY}/releases/download/v1.2.3/patches-1.2.3.mpp`);
    assert.equal(manifest.public_key, key);
    assert.equal(manifest.version, "1.2.3");
  } finally {fs.rmSync(cwd, {recursive: true, force: true});}
});

test("publication uses a draft until upload completes, verifies bytes and supports reruns", async () => {
  const cwd = fs.mkdtempSync(path.join(os.tmpdir(), "codeberg-publish-"));
  const {key, bytes} = signedFixture();
  try {
    fs.mkdirSync(path.join(cwd, "patches/build/libs"), {recursive: true});
    fs.writeFileSync(path.join(cwd, "patches/build/libs/patches-1.2.3.mpp"), bytes);
    fs.writeFileSync(path.join(cwd, "morphe-signing.pub"), key);
    const release = {id: 1, author: {login: "JohninNL"}, prerelease: false, draft: true, html_url: "https://codeberg.org/release"};
    const asset = {name: "patches-1.2.3.mpp", size: bytes.length, browser_download_url: "https://codeberg.org/asset"};
    const calls = [];
    let exists = false;
    const request = async (endpoint, options = {}) => {
      const method = options.method || "GET"; calls.push([method, endpoint, options.body]);
      if (endpoint === "/user") return {login: "JohninNL"};
      if (endpoint.includes("/tags/")) return exists ? {...release, draft: false} : null;
      if (endpoint.endsWith("/assets") && method === "GET") return exists ? [asset] : [];
      if (endpoint.includes("/assets?")) { assert.ok(options.body instanceof FormData); return asset; }
      if (method === "PATCH") return {...release, draft: options.body.draft};
      assert.equal(options.body.draft, true); return release;
    };
    const context = {cwd, env: {CODEBERG_TOKEN: "fixture"}, branch: {name: "main"}, nextRelease: {version: "1.2.3", gitTag: "v1.2.3", gitHead: "a".repeat(40), notes: "Release."}};
    await require("../.forgejo/scripts/publish.cjs").publish({}, context, request, async () => bytes);
    assert.ok(calls.findIndex(c => c[0] === "POST" && c[1].includes("/assets?")) < calls.findIndex(c => c[0] === "PATCH" && c[2].draft === false));
    exists = true; calls.length = 0;
    await require("../.forgejo/scripts/publish.cjs").publish({}, context, request, async () => bytes);
    assert.ok(calls.every(c => c[0] === "GET"));
  } finally {fs.rmSync(cwd, {recursive: true, force: true});}
});

test("Forgejo workflows use bounded hosted runners and keep secrets out of pull request builds", () => {
  for (const name of ["checks", "release", "pages"]) {
    const config = load(fs.readFileSync(path.join(__dirname, `../.forgejo/workflows/${name}.yml`), "utf8"));
    for (const job of Object.values(config.jobs)) {
      assert.ok(["codeberg-medium", "codeberg-small"].includes(job["runs-on"]));
      assert.ok(job["timeout-minutes"] <= (job["runs-on"] === "codeberg-small" ? 5 : 10));
    }
    if (name === "checks") assert.ok(!JSON.stringify(config).includes("secrets."));
  }
});
