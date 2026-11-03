"use strict";
const fs = require("node:fs");
const {api, BASE, REPOSITORY, OWNER} = require("./api.cjs");
const {verifyBundle, download} = require("./bundle.cjs");
function verifyDiscovery(manifest, release, {branch, version, publicKey}) {
  const pattern = branch === "dev" ? /^\d+\.\d+\.\d+-dev\.\d+$/ : /^\d+\.\d+\.\d+$/;
  if (!["main", "dev"].includes(branch) || !pattern.test(version)) throw new Error("Version/channel mismatch");
  if (manifest.version !== version || manifest.public_key !== publicKey.trim()) throw new Error("Source version or signing key changed");
  if (!release || release.tag_name !== `v${version}` || release.upcoming_release || release.author?.username !== OWNER) throw new Error("Published release does not match its source");
  const filename = `patches-${version}.mpp`;
  const url = `${BASE}/${REPOSITORY}/-/releases/v${version}/downloads/${filename}`;
  const packageUrl = `${BASE}/api/v4/projects/${encodeURIComponent(REPOSITORY)}/packages/generic/morphe-patches/${version}/${filename}`;
  if (manifest.download_url !== url || !release.assets?.links?.some(link => link.name === filename && link.url === packageUrl)) throw new Error("Source does not point to the published bundle");
  return {url, filename};
}
async function main() {
  const result = JSON.parse(fs.readFileSync("build/release-safety/gitlab-result.json", "utf8"));
  if (result.published === false) {console.log("No new release was selected"); return;}
  const {branch, version} = result;
  const response = await fetch(`${BASE}/${REPOSITORY}/-/raw/${branch}/patches-bundle.json`, {signal: AbortSignal.timeout(30000)});
  if (!response.ok) throw new Error(`Public source manifest returned HTTP ${response.status}`);
  const manifest = await response.json();
  const project = `/projects/${encodeURIComponent(REPOSITORY)}`;
  const release = await api(`${project}/releases/v${version}`);
  const publicKey = fs.readFileSync("morphe-signing.pub", "utf8").trim();
  const {url, filename} = verifyDiscovery(manifest, release, {branch, version, publicKey});
  const packages = await api(`${project}/packages?package_type=generic&package_name=morphe-patches&package_version=${version}&per_page=100`);
  const pkg = packages?.find(item => item.name === "morphe-patches" && item.version === version);
  if (!pkg) throw new Error("Published package metadata is missing");
  const files = await api(`${project}/packages/${pkg.id}/package_files`);
  const asset = files.find(item => item.file_name === filename && item.size > 0);
  if (!asset) throw new Error("Published bundle file metadata is missing");
  const hash = verifyBundle(await download(url, asset.size), publicKey);
  if (asset.file_sha256 && hash !== asset.file_sha256) throw new Error("Public bundle differs from its package metadata");
  console.log(`PASS public Morphe discovery: ${branch} ${version}; signed bundle SHA-256 ${hash}`);
}
module.exports = {verifyDiscovery};
if (require.main === module) main().catch(error => {console.error(error.message); process.exitCode = 1;});
