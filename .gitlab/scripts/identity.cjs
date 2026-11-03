"use strict";
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const {api, OWNER, NAME, TOKEN_KEY, TOKEN_FILE, TOKEN_FILE_ENV} = require("./api.cjs");
const git = require("../../.github/scripts/verify_identity.cjs");
function localToken() {
  return fs.readFileSync(process.env[TOKEN_FILE_ENV] || path.join(os.homedir(), TOKEN_FILE), "utf8").trim()
    .replace(/^(?:export )?[A-Za-z_][A-Za-z_0-9]*\s*=\s*/, "").replace(/^["']|["']$/g, "");
}
async function verifyPublisher({env = process.env, request = api} = {}) {
  if (!env.GITLAB_TOKEN) throw new Error(`${TOKEN_KEY} must supply GITLAB_TOKEN`);
  const user = await request("/user", {token: env.GITLAB_TOKEN});
  if (user.username !== OWNER) throw new Error(`${NAME} publishing must belong to ${OWNER}`);
}
async function verify(_config, context) {
  await verifyPublisher({env: context.env});
  git.verifyGitIdentity({cwd: context.cwd, env: context.env});
  git.verifyHistory({cwd: context.cwd, env: context.env});
  git.verifyMessage(context.nextRelease?.notes, "Release notes");
}
module.exports = {verifyConditions: verify, prepare: verify, verifyPublisher, localToken};
if (require.main === module) (async () => {
  if (!process.env.GITLAB_TOKEN) process.env.GITLAB_TOKEN = localToken();
  await verify({}, {cwd: process.cwd(), env: process.env});
  console.log(`${NAME} publisher and exact Git identity checks passed`);
})().catch(error => {console.error(error.message);process.exitCode = 1;});
