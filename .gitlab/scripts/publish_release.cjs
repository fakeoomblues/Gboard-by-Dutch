"use strict";
process.env.GITLAB_DESTINATION = "gitlab";
const fs = require("node:fs");
const {execFileSync} = require("node:child_process");
const {api, REPOSITORY} = require("./api.cjs");
const {gitAuth} = require("./release.cjs");
const {verifyBundle} = require("./bundle.cjs");
async function recoverTaggedRelease(branch, {request = api, build = () => execFileSync("./gradlew", [":patches:buildAndroid", "--no-daemon", "-Dorg.gradle.jvmargs=-Xmx2g"], {stdio: "inherit"}), publish = require("./publish.cjs").publish} = {}) {
  const manifest = JSON.parse(fs.readFileSync("patches-bundle.json", "utf8"));
  const pattern = branch === "dev" ? /^\d+\.\d+\.\d+-dev\.\d+$/ : /^\d+\.\d+\.\d+$/;
  if (!["main", "dev"].includes(branch) || !pattern.test(manifest.version)) throw new Error("Tagged source channel/version mismatch");
  const tag = `v${manifest.version}`;
  const tags = execFileSync("git", ["tag", "--points-at", "HEAD"], {encoding: "utf8"}).trim().split("\n");
  if (!tags.includes(tag)) return null;
  // Resume the exact signed upload if the tag was pushed before publication finished.
  const filename = `patches-${manifest.version}.mpp`;
  const stored = await request(`/projects/${encodeURIComponent(REPOSITORY)}/packages/generic/morphe-patches/${manifest.version}/${filename}`, {binary: true});
  if (stored) {
    verifyBundle(stored, fs.readFileSync("morphe-signing.pub", "utf8").trim());
    fs.mkdirSync("patches/build/libs", {recursive: true});
    fs.writeFileSync(`patches/build/libs/${filename}`, stored);
  } else build();
  await publish({}, {cwd: process.cwd(), env: process.env, branch: {name: branch}, nextRelease: {
    version: manifest.version, gitTag: tag, gitHead: execFileSync("git", ["rev-parse", "HEAD"], {encoding: "utf8"}).trim(), notes: manifest.description,
  }});
  return {version: manifest.version, branch};
}
function verifyCurrentHead(branch, readRemote = () => execFileSync("git", ["ls-remote", `https://gitlab.com/${REPOSITORY}.git`, `refs/heads/${branch}`], {encoding: "utf8"})) {
  const local = execFileSync("git", ["rev-parse", "HEAD"], {encoding: "utf8"}).trim();
  const remote = readRemote().trim().split(/\s+/)[0];
  if (!/^[a-f0-9]{40}$/.test(remote)) throw new Error("Cannot verify the current GitLab source head");
  if (local !== remote) throw new Error("Stale release checkout: start a recovery pipeline at the current GitLab release tag or branch head");
}
async function main() {
  process.env.GITLAB_REPOSITORY = REPOSITORY;
  process.env.GITHUB_REPOSITORY = REPOSITORY;
  Object.assign(process.env, gitAuth(process.env));
  const branch = execFileSync("git", ["branch", "--show-current"], {encoding: "utf8"}).trim();
  if (!["main", "dev"].includes(branch)) throw new Error("Only main/dev can publish");
  const {default: release} = await import("semantic-release");
  const result = await release(require("../gitlab-com.release.config.cjs"));
  let outcome;
  if (result) outcome = {version: result.nextRelease.version, branch};
  else {
    outcome = await recoverTaggedRelease(branch);
    if (!outcome) {verifyCurrentHead(branch); outcome = {published: false};}
  }
  fs.mkdirSync("build/release-safety", {recursive: true});
  fs.writeFileSync("build/release-safety/gitlab-result.json", JSON.stringify(outcome));
}
module.exports = {recoverTaggedRelease, verifyCurrentHead};
if (require.main === module) main().catch(error => {console.error(error.message); process.exitCode = 1;});
