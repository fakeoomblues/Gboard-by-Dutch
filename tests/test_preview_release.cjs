"use strict";
const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const {execFileSync, spawnSync} = require("node:child_process");
const {test} = require("node:test");
const previewScript = path.resolve(__dirname, "../.github/scripts/preview_release_notes.cjs");

function repository(run) {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "dap-first-release-"));
  const git = (...args) => execFileSync("git", args, {cwd: dir, encoding: "utf8"}).trim();
  git("init", "-b", "main");
  git("config", "user.email", "test@example.test");
  git("config", "user.name", "Test");
  fs.writeFileSync(path.join(dir, ".releaserc"), JSON.stringify({plugins: [["unused", {preset: "conventionalcommits"}]]}));
  const declarations = path.join(dir, "patches/src/main/kotlin/patches/fixture");
  fs.mkdirSync(declarations, {recursive: true});
  fs.writeFileSync(path.join(declarations, "Compatibility.kt"), 'val app = Compatibility(name = "Fixture")\n');
  git("add", ".");
  git("commit", "-m", "feat(Fixture): add initial controls");
  try { run({dir, git}); } finally { fs.rmSync(dir, {recursive: true, force: true}); }
}

test("preview first release without tags includes the initial commit", () => repository(({dir}) => {
  const result = spawnSync(process.execPath, ["-e", `require(${JSON.stringify(previewScript)}).preview().catch(e=>{console.error(e);process.exitCode=1})`], {cwd: dir, encoding: "utf8", timeout: 30000});
  assert.equal(result.status, 0, result.stderr);
  const notes = fs.readFileSync(path.join(dir, "build/release-safety/notes-preview.md"), "utf8");
  assert.ok(notes.includes("patch bundle v1.0.0"), notes);
  assert.match(notes, /\* \*\*Fixture:\*\* add initial controls/);
  assert.doesNotMatch(notes, /undefined|null/);
}));
