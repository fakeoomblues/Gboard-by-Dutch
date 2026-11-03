"use strict";
process.env.GITLAB_DESTINATION = "gitlab";
const assert = require("node:assert/strict");
const {test} = require("node:test");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const crypto = require("node:crypto");
const {execFileSync} = require("node:child_process");
const {load} = require("js-yaml");
function signedFixture() {
  const {privateKey, publicKey} = crypto.generateKeyPairSync("ed25519");
  const key = Buffer.from(publicKey.export({format:"jwk"}).x,"base64url").toString("base64");
  const zip = Buffer.alloc(22); zip.writeUInt32LE(0x06054b50);
  const signature = crypto.sign(null,Buffer.concat([Buffer.from("MorpheSig1"),crypto.createHash("sha256").update(zip).digest()]),privateKey);
  const comment = Buffer.from(`MorpheSig1 ${key} ${signature.toString("base64")}`); zip.writeUInt16LE(comment.length,20);
  return {key,bytes:Buffer.concat([zip,comment])};
}
test("GitLab chooses versions with existing source guards and never loads another forge publisher", () => {
  const config=require("../.gitlab/gitlab-com.release.config.cjs");
  assert.equal(config.repositoryUrl,"https://gitlab.com/johninnld/nl-morphe-patches.git");
  assert.deepEqual(config.branches,["main",{name:"dev",prerelease:"dev",channel:"dev"}]);
  const plugins=config.plugins.map(p=>Array.isArray(p)?p[0]:p);
  assert.ok(plugins.includes("./.github/scripts/verify_release_notes.cjs"));
  assert.equal(plugins[plugins.indexOf("./.github/scripts/public_git.cjs")-1],"./.gitlab/scripts/identity.cjs");
  assert.ok(plugins.includes("./.gitlab/scripts/publish.cjs"));
  assert.ok(plugins.includes("./.gitlab/scripts/changelog.cjs"));
  assert.ok(!plugins.some(p=>p.includes(".forgejo/")||p.includes("public_github")));
  const ci=load(fs.readFileSync(path.join(__dirname,"../.gitlab-ci.yml"),"utf8"));
  const commands=ci.release.script.join("\n");
  assert.ok(commands.indexOf("check_secrets.cjs")<commands.indexOf("publish_release.cjs"));
  assert.ok(commands.indexOf("publish_release.cjs")<commands.indexOf("discovery.cjs"));
  assert.deepEqual(ci.release.needs,["verify"]);
});
test("GitLab Pages deploys only the post-release site to its public Pages URL", () => {
  const ci=load(fs.readFileSync(path.join(__dirname,"../.gitlab-ci.yml"),"utf8"));
  assert.deepEqual(ci.stages,["verify","release","deploy"]);
  assert.equal(ci.pages.stage,"deploy");
  assert.deepEqual(ci.pages.needs,[{job:"release",artifacts:false}]);
  assert.equal(ci.pages.pages.publish,"public");
  assert.deepEqual(ci.pages.rules,[{if:'$CI_COMMIT_BRANCH == "main"'}]);
  const commands=ci.pages.script.join("\n");
  assert.match(commands,/apt-get install -y --no-install-recommends git python3 python3-venv/);
  assert.ok(commands.indexOf("apt-get install")<commands.indexOf("git fetch origin"));
  assert.match(commands,/CI_PAGES_URL/);
  assert.match(commands,/GITLAB_REPOSITORY="\$CI_PROJECT_PATH" SITE_URL=/);
  assert.deepEqual(ci.pages.before_script,[]);
  assert.match(commands,/mkdocs build --strict --site-dir public/);
  assert.match(commands,/test -s public\/index\.html/);
  assert.equal(ci.pages.environment.name,"production");
  const mkdocs=fs.readFileSync(path.join(__dirname,"../mkdocs.yml"),"utf8");
  assert.match(mkdocs,/site_url: !ENV \[SITE_URL, 'https:\/\/nl-morphe-patches-ed90fa\.gitlab\.io\/'\]/);
  assert.match(fs.readFileSync(path.join(__dirname,"../scripts/check_docs_site.py"),"utf8"),/os\.environ\.get\('SITE_URL'/);
});
test("GitLab source metadata uses its public release permalink and existing signing key", async () => {
  const cwd=fs.mkdtempSync(path.join(os.tmpdir(),"gitlab-native-notes-"));
  try {
    const {key}=signedFixture();fs.writeFileSync(path.join(cwd,"morphe-signing.pub"),key);
    await require("../.gitlab/scripts/changelog.cjs").prepare({}, {cwd,nextRelease:{version:"1.2.3",notes:"Release notes."}});
    const manifest=JSON.parse(fs.readFileSync(path.join(cwd,"patches-bundle.json")));
    assert.equal(manifest.public_key,key);assert.equal(manifest.version,"1.2.3");
    assert.equal(manifest.download_url,"https://gitlab.com/johninnld/nl-morphe-patches/-/releases/v1.2.3/downloads/patches-1.2.3.mpp");
  } finally {fs.rmSync(cwd,{recursive:true,force:true});}
});
test("GitLab release branding uses its immutable source and restores only known public links", () => {
  const {renderNotes,restorePublicLinks}=require("../.github/scripts/branded_release_notes.cjs");
  const head="c".repeat(40);
  const notes=renderNotes("Release.",{env:{GITLAB_REPOSITORY:"johninnld/nl-morphe-patches",CODEBERG_REPOSITORY:"JohninNL/nl-morphe-patches"},branch:{name:"main"},nextRelease:{version:"1.2.3",gitHead:head}});
  assert.ok(notes.includes(`https://gitlab.com/johninnld/nl-morphe-patches/-/raw/${head}/docs/site/assets/brand/release-banner.png`));
  assert.ok(notes.includes(`https://gitlab.com/johninnld/nl-morphe-patches/-/commit/${head}`));
  assert.ok(!notes.includes("codeberg.org"));
  assert.equal(restorePublicLinks("https://gitlab.com/[secure]d/nl-morphe-patches/-/commit/a token=[secure]"),"https://gitlab.com/johninnld/nl-morphe-patches/-/commit/a token=[secure]");
  assert.equal(restorePublicLinks("https://gitlab.com/[secure]/other-project"),"https://gitlab.com/[secure]/other-project");
});
test("GitLab publishes and verifies exact signed public bytes, retries without overwriting and rejects wrong source", async () => {
  const cwd=fs.mkdtempSync(path.join(os.tmpdir(),"gitlab-native-publish-"));
  const {key,bytes}=signedFixture();
  const hash="a".repeat(40);let stored=null,release=null;const writes=[];
  const context={cwd,env:{GITLAB_TOKEN:"fixture"},branch:{name:"main"},nextRelease:{version:"1.2.3",gitTag:"v1.2.3",gitHead:hash,notes:"Release."}};
  const request=async(endpoint,options={})=>{
    if(endpoint==="/user")return {username:"johninnld"};
    assert.ok(endpoint.startsWith("/projects/johninnld%2Fnl-morphe-patches"));
    if(endpoint.includes("/packages/generic/")){
      if(options.method==="PUT"){stored=options.body;writes.push("package");return {message:"created"};}return stored;
    }
    if(options.method==="POST"){
      writes.push("release");release={...options.body,author:{username:"johninnld"},commit:{id:hash}};return release;
    }
    return release;
  };
  const reads=[];const reader=async(url,size)=>{assert.ok(url.startsWith("https://gitlab.com/"));assert.equal(size,bytes.length);reads.push(url);return stored;};
  try {
    fs.mkdirSync(path.join(cwd,"patches/build/libs"),{recursive:true});
    fs.writeFileSync(path.join(cwd,"patches/build/libs/patches-1.2.3.mpp"),bytes);
    fs.writeFileSync(path.join(cwd,"morphe-signing.pub"),key);
    const publish=require("../.gitlab/scripts/publish.cjs").publish;
    await publish({},context,request,reader);assert.deepEqual(writes,["package","release"]);
    assert.ok(reads[0].includes("/packages/generic/"));assert.ok(reads[1].includes("/-/releases/v1.2.3/downloads/"));
    await publish({},context,request,reader);assert.deepEqual(writes,["package","release"]);
    release.commit.id="b".repeat(40);await assert.rejects(publish({},context,request,reader),/source commit/);
    release.commit.id=hash;stored=Buffer.from(bytes);stored[4]^=1;await assert.rejects(publish({},context,request,reader),/signature/);
    await assert.rejects(publish({}, {...context,branch:{name:"dev"}},request,reader),/channel/);
  } finally {fs.rmSync(cwd,{recursive:true,force:true});}
});
test("GitLab recovery reuses the exact signed upload only at its authoritative release tag", async () => {
  const cwd=fs.mkdtempSync(path.join(os.tmpdir(),"gitlab-native-recovery-"));const previous=process.cwd();
  const {key,bytes}=signedFixture();
  const env={...process.env,GIT_AUTHOR_NAME:"johninnl",GIT_AUTHOR_EMAIL:"349981574+johninnl@users.noreply.github.com",GIT_COMMITTER_NAME:"johninnl",GIT_COMMITTER_EMAIL:"349981574+johninnl@users.noreply.github.com"};
  const git=(...args)=>execFileSync("git",args,{cwd,env,encoding:"utf8",stdio:["ignore","pipe","pipe"]}).trim();
  try {
    fs.writeFileSync(path.join(cwd,"morphe-signing.pub"),key);
    fs.writeFileSync(path.join(cwd,"patches-bundle.json"),JSON.stringify({version:"1.2.3",public_key:key,description:"Release."}));
    git("init","-b","main");git("add",".");git("-c","commit.gpgsign=false","commit","-m","test: recovery fixture");process.chdir(cwd);
    const recover=require("../.gitlab/scripts/publish_release.cjs").recoverTaggedRelease;
    const {verifyCurrentHead}=require("../.gitlab/scripts/publish_release.cjs");
    let requests=0,published=0;
    const options={request:async()=>{requests++;return bytes;},build:()=>{throw new Error("Must reuse signed upload");},publish:async()=>{published++;}};
    assert.equal(await recover("main",options),null);assert.equal(requests,0);
    const source=git("rev-parse","HEAD");
    assert.doesNotThrow(()=>verifyCurrentHead("main",()=>`${source}\trefs/heads/main\n`));
    assert.throws(()=>verifyCurrentHead("main",()=>`${"b".repeat(40)}\trefs/heads/main\n`),/Stale release checkout.*recovery pipeline/);
    git("tag","v1.2.3");assert.deepEqual(await recover("main",options),{version:"1.2.3",branch:"main"});assert.equal(published,1);
    assert.deepEqual(fs.readFileSync("patches/build/libs/patches-1.2.3.mpp"),bytes);
    const bad=Buffer.from(bytes);bad[4]^=1;await assert.rejects(recover("main",{...options,request:async()=>bad}),/signature/);
    await assert.rejects(recover("dev",options),/channel/);
  } finally {process.chdir(previous);fs.rmSync(cwd,{recursive:true,force:true});}
});
test("GitLab anonymous downloads and discovery reject wrong hosts, channels and signing identities", async () => {
  const {key,bytes}=signedFixture();const {download}=require("../.gitlab/scripts/bundle.cjs");
  await assert.rejects(download("https://codeberg.org/asset",bytes.length),/host/);
  assert.deepEqual(await download("https://gitlab.com/asset",bytes.length,async(_url,opts)=>{assert.equal(opts.headers,undefined);return {ok:true,arrayBuffer:async()=>bytes};}),bytes);
  const {verifyDiscovery}=require("../.gitlab/scripts/discovery.cjs");
  const manifest={version:"1.2.3",public_key:key,download_url:"https://gitlab.com/johninnld/nl-morphe-patches/-/releases/v1.2.3/downloads/patches-1.2.3.mpp"};
  const release={tag_name:"v1.2.3",author:{username:"johninnld"},assets:{links:[{name:"patches-1.2.3.mpp",url:"https://gitlab.com/api/v4/projects/johninnld%2Fnl-morphe-patches/packages/generic/morphe-patches/1.2.3/patches-1.2.3.mpp"}]}};
  const options={branch:"main",version:"1.2.3",publicKey:key};
  assert.ok(verifyDiscovery(manifest,release,options).url.includes("gitlab.com"));
  assert.throws(()=>verifyDiscovery(manifest,release,{...options,branch:"dev"}),/channel/);
  assert.throws(()=>verifyDiscovery({...manifest,public_key:signedFixture().key},release,options),/signing/);
  assert.throws(()=>verifyDiscovery(manifest,{...release,author:{username:"bot"}},options),/source/);
});
