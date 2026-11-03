"use strict";
const os = require("node:os");
const path = require("node:path");
const {api, OWNER, NAME, TOKEN_KEY, TOKEN_FILE_ENV} = require("./api.cjs");
const {loadApiKeys,loadTokenFile} = require("../../scripts/local-release/credentials.cjs");
const git = require("../../.github/scripts/verify_identity.cjs");
function localToken({env=process.env,home=os.homedir()}={}) {
  if(env[TOKEN_FILE_ENV])return loadTokenFile(env[TOKEN_FILE_ENV]);
  return loadApiKeys(path.join(home,".ci"))[NAME.toUpperCase()+"_TOKEN"];
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
