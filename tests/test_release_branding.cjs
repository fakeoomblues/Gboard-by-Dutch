const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { test, after } = require("node:test");
const { cosmiconfigSync } = require("cosmiconfig");

const root = path.resolve(__dirname, "..");
const pluginPath = path.join(root, ".github/scripts/branded_release_notes.cjs");
const source = "0123456789abcdef0123456789abcdef01234567";
const existing = "## 1.9.0\n\n### 🐛 Bug Fixes\n\n* Retain the exact compatibility check ([abc1234](https://example.test/commit/abc1234))\n";
const releaseConfig = cosmiconfigSync("release").search(root).config;
const noteConfig = [...releaseConfig.plugins.find((plugin) => Array.isArray(plugin) && plugin[1].preset === "conventionalcommits")];
noteConfig[0] = pluginPath;

function plugin() {
  assert.ok(fs.existsSync(pluginPath), "the local release-notes wrapper must exist");
  return require(pluginPath);
}

let brandingRepository;
function brandingFixture() {
  if (brandingRepository) return brandingRepository;
  const dir = fs.mkdtempSync(path.join(require("node:os").tmpdir(), "dap-branding-fixture-"));
  const git = (...args) => require("node:child_process").execFileSync("git", args, {cwd: dir, encoding: "utf8"}).trim();
  git("init", "-b", "main");
  git("config", "user.email", "test@example.test");
  git("config", "user.name", "Test");
  const declarations = path.join(dir, "patches/src/main/kotlin/patches/fixture");
  fs.mkdirSync(declarations, {recursive: true});
  fs.writeFileSync(path.join(declarations, "Compatibility.kt"), 'val fixture = Compatibility(name = "Fixture")\n');
  git("add", ".");
  git("commit", "-m", "feat: fixture baseline");
  git("tag", "v1.8.0");
  const base = git("rev-parse", "HEAD");
  git("commit", "--allow-empty", "-m", "fix(release): retain exact compatibility check");
  fs.copyFileSync(path.join(root, ".releaserc"), path.join(dir, ".releaserc"));
  fs.symlinkSync(path.join(root, "node_modules"), path.join(dir, "node_modules"), "dir");
  brandingRepository = {dir, base, head: git("rev-parse", "HEAD")};
  return brandingRepository;
}
after(() => { if (brandingRepository) fs.rmSync(brandingRepository.dir, {recursive: true, force: true}); });

function context(version = "1.9.0") {
  const fixture = brandingFixture();
  return {
    cwd: fixture.dir,
    env: { GITHUB_REPOSITORY: "johninnl/nl-morphe-patches" },
    options: { repositoryUrl: "https://github.com/johninnl/nl-morphe-patches.git" },
    branch: { name: "main", type: "release" },
    commits: [{ hash: fixture.head, message: "fix(release): retain exact compatibility check" }],
    lastRelease: { version: "1.8.0", gitTag: "v1.8.0", gitHead: fixture.base },
    nextRelease: { version, gitTag: `v${version}`, gitHead: fixture.head },
    logger: { log() {} },
  };
}

test("release banner is pinned to the full source commit and generated notes remain intact", () => {
  const ctx = context(); ctx.nextRelease.gitHead = source;
  const result = plugin().renderNotes(existing, ctx);
  assert.match(result, new RegExp(`https://raw\\.githubusercontent\\.com/johninnl/nl-morphe-patches/${source}/docs/site/assets/brand/release-banner\\.png`));
  assert.ok(result.endsWith(existing), "generated Markdown must be retained byte for byte");
  assert.match(result, /Release · patch bundle v1\.9\.0/);
  assert.match(result, /https:\/\/morphe\.software\/add-source\?github=johninnl\/nl-morphe-patches/);
  assert.match(result, /https:\/\/johninnl\.github\.io\/nl-morphe-patches\/en\//);
});

test("reapplying branding does not duplicate the banner or changelog", () => {
  const first = plugin().renderNotes(existing, context());
  assert.equal(plugin().renderNotes(first, context()), first);
  assert.equal(first.match(/release-banner\.png/g).length, 1);
  assert.equal(first.match(/Retain the exact compatibility check/g).length, 1);
});

test("mutable refs and unsafe repository names cannot enter the source banner URL", () => {
  const ctx = context();
  ctx.nextRelease.gitHead = "main";
  assert.throws(() => plugin().renderNotes(existing, ctx), /source commit/);
  ctx.nextRelease.gitHead = source;
  ctx.env.GITHUB_REPOSITORY = "owner/repo/../../main";
  assert.throws(() => plugin().renderNotes(existing, ctx), /repository/);
});

test("wrapper delegates to the existing conventional generator and ignores accumulated notes on regeneration", async () => {
  const wrapper = plugin();
  const { generateNotes } = await import("@semantic-release/release-notes-generator");
  const ctx = context();
  const generated = await generateNotes(noteConfig[1], {...ctx, commits: wrapper.scopeCommits(ctx.commits,ctx)});
  assert.match(generated, /🐛 Bug Fixes/);
  const decorated = await wrapper.generateNotes(noteConfig[1], ctx);
  assert.ok(decorated.endsWith(generated));
  ctx.nextRelease.notes = decorated;
  assert.equal(await wrapper.generateNotes(noteConfig[1], ctx), decorated);
  assert.equal(decorated.match(/retain exact compatibility check/g).length, wrapper.scopeCommits(ctx.commits,ctx).length);
});

test("configured semantic-release generateNotes pipeline emits one complete branded changelog", async () => {
  plugin();
  const { default: buildPlugins } = await import("semantic-release/lib/plugins/index.js");
  const logger = { log() {}, success() {}, warn() {}, error() {}, scope() { return this; } };
  const ctx = context();
  ctx.options.plugins = [releaseConfig.plugins[0], noteConfig];
  ctx.logger = logger;
  const pipeline = await buildPlugins(ctx, {});
  const notes = await pipeline.generateNotes(ctx);
  assert.equal(notes.match(/release-banner\.png/g).length, 1);
  assert.equal(notes.match(/retain exact compatibility check/g).length, plugin().scopeCommits(ctx.commits,ctx).length);
  assert.match(notes, /🐛 Bug Fixes/);
});

const os = require("node:os");
const { execFileSync } = require("node:child_process");

function releaseRepository(run) {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "dap-release-notes-"));
  const git = (...args) => execFileSync("git", args, {cwd: dir, encoding: "utf8"}).trim();
  git("init", "-b", "main");
  git("config", "user.email", "test@example.test");
  git("config", "user.name", "Test");
  const commit = message => { git("commit", "--allow-empty", "-m", message); return git("rev-parse", "HEAD"); };
  try { return run({dir, git, commit}); } finally { fs.rmSync(dir, {recursive: true, force: true}); }
}

test("multi-app notes use one explicit Morphe scope per app", () => {
  const commits = plugin().scopeCommits([{hash: source,
    message: "feat(news): promote newspaper controls\n\nRelease-Apps: Tubantia, HBVL"}]);
  assert.deepEqual(commits.map(c => c.message.split("\n")[0]), [
    "feat(Tubantia): promote newspaper controls", "feat(HBVL): promote newspaper controls"
  ]);
});

test("unscoped general notes cannot match common quoted patch names in Morphe", () => {
  const [commit] = plugin().scopeCommits([{hash: source, message: "fix: correct `Hide ads` reporting"}]);
  assert.equal(commit.message, "fix(General): correct `Hide ads` reporting");
});

test("release guard rejects replayed bullets and missing app scopes before publication", () => releaseRepository(({dir, git, commit}) => {
  const write = (file, content) => { fs.mkdirSync(path.dirname(path.join(dir, file)), {recursive:true}); fs.writeFileSync(path.join(dir,file),content); };
  write("patches/src/main/kotlin/patches/old/Compatibility.kt", 'val app = Compatibility( name = "Old App", packageName = "example.old")');
  git("add", "."); const old = commit("feat(Old App): original support"); git("tag", "v1.27.0");
  write("patches/src/main/kotlin/patches/tubantia/Compatibility.kt", 'val app = Compatibility( name = "Tubantia", packageName = "example.tubantia")');
  git("add", "."); const head = commit("feat(Tubantia): add controls");
  const ctx = context("1.28.0"); ctx.cwd=dir; ctx.lastRelease={version:"1.27.0",gitTag:"v1.27.0",gitHead:old}; ctx.nextRelease.gitHead=head;
  ctx.commits=[{hash:head,message:"feat(Tubantia): add controls"}];
  const header="## [1.28.0](https://github.com/test/repo/compare/v1.27.0...v1.28.0) (2026-10-30)\n\n";
  const bullet=(scope,hash)=>`* **${scope}:** controls ([${hash.slice(0,7)}](https://github.com/test/repo/commit/${hash}))`;
  const guard=require("../.github/scripts/verify_release_notes.cjs");
  ctx.nextRelease.notes=header+bullet("Tubantia",head);
  assert.doesNotThrow(()=>guard.verifyNotes(ctx));
  ctx.nextRelease.notes=header+bullet("Old App",old);
  assert.throws(()=>guard.verifyNotes(ctx),/replayed|outside/i);
  ctx.nextRelease.notes=header+bullet("news",head);
  assert.throws(()=>guard.verifyNotes(ctx),/scope|Tubantia/i);
  ctx.nextRelease.notes=header+bullet("Old App",head);
  assert.throws(()=>guard.verifyNotes(ctx),/unchanged|scope/i);
}));

test("release configuration publishes only main and validates pull requests to main", () => {
  const {load} = require("js-yaml");
  assert.deepEqual(releaseConfig.branches, ["main"]);
  const release = load(fs.readFileSync(path.join(root, ".github/workflows/release.yml"), "utf8"));
  assert.deepEqual(release.on.push.branches, ["main"]);
  assert.equal(release.jobs.release.if, "github.ref == 'refs/heads/main'");
  const safety = load(fs.readFileSync(path.join(root, ".github/workflows/release-safety.yml"), "utf8"));
  assert.deepEqual(safety.on.pull_request.branches, ["main"]);
  const dependabot = load(fs.readFileSync(path.join(root, ".github/dependabot.yml"), "utf8"));
  for (const update of dependabot.updates) {
    assert.equal(update["target-branch"], "main", `${update["package-ecosystem"]} updates must target main`);
  }
  const plugins = releaseConfig.plugins.map(item => Array.isArray(item) ? item[0] : item);
  assert.deepEqual(plugins.filter(name => name.startsWith("./.github/scripts/")), [
    "./.github/scripts/branded_release_notes.cjs", "./.github/scripts/verify_release_notes.cjs"
  ]);
});

test("release safety guards stay wired into publishing and pull requests", () => {
  assert.ok(releaseConfig.plugins.includes("./.github/scripts/verify_release_notes.cjs"));
  const workflow=fs.readFileSync(path.join(root,".github/workflows/release-safety.yml"),"utf8");
  assert.match(workflow,/pull_request:/);
  assert.match(workflow,/node \.github\/scripts\/preview_release_notes\.cjs/);
  assert.match(workflow,/test_release_branding\.cjs/);
});

test("semantic-release verifyRelease succeeds before nextRelease.notes exists", async () => {
  const {default: buildPlugins}=await import("semantic-release/lib/plugins/index.js");
  const ctx=context("1.28.0");
  const logger={log(){},success(){},warn(){},error(){},scope(){return this;}};
  ctx.logger=logger;
  ctx.options.plugins=[noteConfig,path.join(root,".github/scripts/verify_release_notes.cjs")];
  const parts=execFileSync("git",["log","--no-merges","--format=%H%x00%B%x00",`${ctx.lastRelease.gitTag}..${ctx.nextRelease.gitHead}`],{cwd:ctx.cwd,encoding:"utf8"}).trim().split("\0");
  ctx.commits=[];
  for(let i=0;i+1<parts.length;i+=2) ctx.commits.push({hash:parts[i].trim(),message:parts[i+1].trim()});
  const pipeline=await buildPlugins(ctx,{});
  assert.equal(ctx.nextRelease.notes,undefined);
  await pipeline.verifyRelease(ctx);
  const notes=await pipeline.generateNotes(ctx);
  assert.match(notes,/1\.28\.0/);
});

test("each shared-only commit requires its own affected-app declaration", () => releaseRepository(({dir,git,commit}) => {
  const file="patches/src/main/kotlin/patches/paper/Compatibility.kt";
  fs.mkdirSync(path.dirname(path.join(dir,file)),{recursive:true});
  fs.writeFileSync(path.join(dir,file),'val app = Compatibility( name = "Paper", packageName = "example.paper")');
  git("add", "."); const base=commit("feat(Paper): original app"); git("tag", "v1.0.0");
  fs.appendFileSync(path.join(dir,file),"\n// Changed control");git("add", ".");const direct=commit("fix(Paper): direct change");
  const shared="patches/src/main/kotlin/patches/shared/Helper.kt";
  fs.mkdirSync(path.dirname(path.join(dir,shared)),{recursive:true});fs.writeFileSync(path.join(dir,shared),"fun helper() = true");
  git("add", ".");const indirect=commit("fix: shared change");
  const ctx=context("1.1.0");ctx.cwd=dir;ctx.lastRelease={version:"1.0.0",gitTag:"v1.0.0",gitHead:base};ctx.nextRelease.gitHead=indirect;
  const bullet=(scope,hash)=>`* **${scope}:** changed ([${hash.slice(0,7)}](https://github.com/test/repo/commit/${hash}))`;
  ctx.nextRelease.notes="## [1.1.0](https://github.com/test/repo/compare/v1.0.0...v1.1.0) (2026-10-30)\n"+bullet("Paper",direct)+"\n"+bullet("General",indirect);
  const guard=require("../.github/scripts/verify_release_notes.cjs");
  assert.throws(()=>guard.verifyNotes(ctx),/shared.*Release-Apps/i);
  git("commit","--amend","-m","fix: shared change\n\nRelease-Apps: Paper");
  const declared=git("rev-parse","HEAD");ctx.nextRelease.gitHead=declared;
  ctx.nextRelease.notes="## [1.1.0](https://github.com/test/repo/compare/v1.0.0...v1.1.0) (2026-10-30)\n"+bullet("Paper",direct)+"\n"+bullet("Paper",declared);
  assert.doesNotThrow(()=>guard.verifyNotes(ctx));
}));
