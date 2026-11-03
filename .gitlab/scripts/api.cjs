"use strict";
const BASE = "https://gitgud.io";
const REPOSITORY = "JohninNL/nl-morphe-patches";
async function api(endpoint, {token = process.env.GITLAB_TOKEN, method = "GET", body, binary = false, fetcher = fetch} = {}) {
  if (!endpoint.startsWith("/") || endpoint.startsWith("//")) throw new Error("Invalid GitGud API path");
  if (!token || !/^[A-Za-z0-9._-]+$/.test(token)) throw new Error("GITLAB_TOKEN is required");
  const headers = {"PRIVATE-TOKEN": token, Accept: "application/json"};
  if (body && !Buffer.isBuffer(body)) {headers["Content-Type"] = "application/json";body = JSON.stringify(body);}
  if (Buffer.isBuffer(body)) headers["Content-Type"] = "application/octet-stream";
  const response = await fetcher(`${BASE}/api/v4${endpoint}`, {method, headers, body, redirect: "error", signal: AbortSignal.timeout(60000)});
  if (response.status === 404 && method === "GET") return null;
  if (!response.ok) throw new Error(`GitGud ${method} ${endpoint}: HTTP ${response.status}`);
  if (response.status === 204) return null;
  return binary ? Buffer.from(await response.arrayBuffer()) : response.json();
}
module.exports = {api, BASE, REPOSITORY};
