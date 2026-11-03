"use strict";
const {verifyBundle} = require("../../.forgejo/scripts/bundle.cjs");
async function download(url, expectedSize, fetcher = fetch) {
  const target = new URL(url);
  if (target.protocol !== "https:" || target.hostname !== "gitlab.com" || target.username || target.password) throw new Error("Unexpected GitLab download host");
  if (!Number.isSafeInteger(expectedSize) || expectedSize <= 0) throw new Error("Invalid expected bundle size");
  // Downloads are public. Never forward a publishing token to an asset redirect.
  const response = await fetcher(url, {signal: AbortSignal.timeout(60000)});
  if (!response.ok) throw new Error(`Bundle download failed: HTTP ${response.status}`);
  const bytes = Buffer.from(await response.arrayBuffer());
  if (bytes.length !== expectedSize) throw new Error("Downloaded bundle size does not match its package");
  return bytes;
}
module.exports = {verifyBundle, download};
