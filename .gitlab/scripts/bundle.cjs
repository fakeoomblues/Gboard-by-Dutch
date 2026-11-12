"use strict";
const {verifyBundle} = require("../../.forgejo/scripts/bundle.cjs");
const {transferTimeout} = require("./api.cjs");
async function download(url, expectedSize, fetcher = fetch) {
  const target = new URL(url);
  if (target.protocol !== "https:" || target.hostname !== "gitlab.com" || target.username || target.password) throw new Error("Unexpected GitLab download host");
  if (expectedSize != null && (!Number.isSafeInteger(expectedSize) || expectedSize <= 0)) throw new Error("Invalid expected bundle size");
  // Downloads are public. Never forward a publishing token to an asset redirect.
  // Unknown size: budget for a bundle-sized (up to 64 MiB) transfer.
  const response = await fetcher(url, {signal: AbortSignal.timeout(transferTimeout(expectedSize || 64 * 1024 * 1024))});
  if (!response.ok) throw new Error(`Bundle download failed: HTTP ${response.status}`);
  const bytes = Buffer.from(await response.arrayBuffer());
  if (expectedSize != null && bytes.length !== expectedSize) throw new Error("Downloaded bundle size does not match its package");
  return bytes;
}
module.exports = {verifyBundle, download};
