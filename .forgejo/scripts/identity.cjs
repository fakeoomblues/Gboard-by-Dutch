"use strict";
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const {api} = require("./api.cjs");
const guard = require("../../.github/scripts/verify_identity.cjs");
function localToken() {
  const file = process.env.CODEBERG_TOKEN_FILE || path.join(os.homedir(), ".codeberg");
  const value = fs.readFileSync(file, "utf8").trim();
  return value.replace(/^(?:export )?[A-Za-z_][A-Za-z_0-9]*\s*=\s*/, "").replace(/^["']|["']$/g, "");
}
async function verifyPublisher({env = process.env, request = api} = {}) {
  if (!env.CODEBERG_TOKEN) throw new Error("JOHNINNL_CODEBERG_TOKEN must supply CODEBERG_TOKEN");
  const user = await request("/user", {token: env.CODEBERG_TOKEN});
  if (user?.login !== "JohninNL") throw new Error("Codeberg release authentication must belong to JohninNL");
}
async function verify(_config, context) {
  await verifyPublisher({env: context.env});
  const options = {cwd: context.cwd, env: context.env};
  guard.verifyGitIdentity(options);
  guard.verifyHistory(options);
  guard.verifyMessage(context.nextRelease?.notes, "Release notes");
}
module.exports = {verifyConditions: verify, prepare: verify, verifyPublisher, localToken};
if (require.main === module) (async () => {
  const mode = process.argv[2];
  if (!["--local", "--release", "--history"].includes(mode)) throw new Error("Use --local, --release or --history");
  if (mode !== "--history") {
    if (mode === "--local" && !process.env.CODEBERG_TOKEN) process.env.CODEBERG_TOKEN = localToken();
    await verifyPublisher(); guard.verifyGitIdentity();
  }
  guard.verifyHistory();
  console.log("Codeberg publisher and Git identity checks passed");
})().catch(error => {console.error(error.message);process.exitCode = 1;});
