"use strict";
const fs = require("node:fs");
const crypto = require("node:crypto");
for (const name of ["CODEBERG_TOKEN", "MORPHE_SIGNING_KEY", "SHARED_MAPS_API_KEY", "BRIGHT_ARTICLE_SIGNING_KEY"]) {
  if (!process.env[name]?.trim()) throw new Error(`Missing required secret: ${name}`);
}
// Morphe uses base64 raw Ed25519 seeds/public keys, not PEM/SPKI files.
const seed = Buffer.from(process.env.MORPHE_SIGNING_KEY.trim(), "base64");
if (seed.length !== 32) throw new Error("Morphe private key must contain a raw Ed25519 seed");
const privateKey = crypto.createPrivateKey({key: Buffer.concat([Buffer.from("302e020100300506032b657004220420", "hex"), seed]), format: "der", type: "pkcs8"});
const derived = Buffer.from(crypto.createPublicKey(privateKey).export({format: "jwk"}).x, "base64url");
const expected = Buffer.from(fs.readFileSync("morphe-signing.pub", "utf8").trim(), "base64");
if (!derived.equals(expected)) throw new Error("Morphe signing key does not match the existing source public key");
console.log("Required secrets exist; the Morphe signing identity is unchanged");
