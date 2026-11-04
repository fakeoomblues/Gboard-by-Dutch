"use strict";
const assert=require("node:assert/strict");
const {test}=require("node:test");
const fs=require("node:fs");
const path=require("node:path");
const os=require("node:os");
const crypto=require("node:crypto");
const script=path.join(__dirname,"../scripts/local-release/mirrors.cjs");
function api(){assert.ok(fs.existsSync(script),"Parallel mirror implementation is required");return require(script);}
function fixture(t,branch="main"){
  const cwd=fs.mkdtempSync(path.join(os.tmpdir(),"local-release-mirrors-"));t.after(()=>fs.rmSync(cwd,{recursive:true,force:true}));
  const {privateKey,publicKey}=crypto.generateKeyPairSync("ed25519");const key=Buffer.from(publicKey.export({format:"jwk"}).x,"base64url").toString("base64");
  const zip=Buffer.alloc(22);zip.writeUInt32LE(0x06054b50);const signature=crypto.sign(null,Buffer.concat([Buffer.from("MorpheSig1"),crypto.createHash("sha256").update(zip).digest()]),privateKey);
  const comment=Buffer.from(`MorpheSig1 ${key} ${signature.toString("base64")}`);zip.writeUInt16LE(comment.length,20);const bytes=Buffer.concat([zip,comment]);
  const alternateZip=Buffer.alloc(22);alternateZip.writeUInt32LE(0x06054b50);alternateZip[4]=1;
  const alternateSignature=crypto.sign(null,Buffer.concat([Buffer.from("MorpheSig1"),crypto.createHash("sha256").update(alternateZip).digest()]),privateKey);
  const alternateComment=Buffer.from(`MorpheSig1 ${key} ${alternateSignature.toString("base64")}`);alternateZip.writeUInt16LE(alternateComment.length,20);const alternateBytes=Buffer.concat([alternateZip,alternateComment]);
  const head="a".repeat(40),tagSha="b".repeat(40),version=branch==="dev"?"1.2.3-dev.1":"1.2.3",tag=`v${version}`,filename=`patches-${version}.mpp`;
  const receipt={version,branch};const manifest={version,public_key:key,description:"Canonical GitLab notes",download_url:`https://gitlab.com/johninnld/nl-morphe-patches/-/releases/${tag}/downloads/${filename}`};
  fs.mkdirSync(path.join(cwd,"build/release-safety"),{recursive:true});fs.mkdirSync(path.join(cwd,"patches/build/libs"),{recursive:true});
  fs.writeFileSync(path.join(cwd,"build/release-safety/gitlab-result.json"),JSON.stringify(receipt));fs.writeFileSync(path.join(cwd,"patches-bundle.json"),JSON.stringify(manifest));fs.writeFileSync(path.join(cwd,"morphe-signing.pub"),key);fs.writeFileSync(path.join(cwd,"patches/build/libs",filename),bytes);
  const calls=[],targets={codeberg:{refs:{},assets:[],release:null},gitgud:{refs:{},stored:null,release:null}};
  const env={CODEBERG_TOKEN:"codeberg-fixture",GITGUD_TOKEN:"gitgud-fixture",GITLAB_TOKEN:"canonical-fixture"};
  const git=(args,options={})=>{
    calls.push({kind:"git",args,env:options.env});
    if(args[0]==="branch")return branch;
    if(args[0]==="var")return "johninnl <349981574+johninnl@users.noreply.github.com> 1 +0000";
    if(args[0]==="log")return head+"\0johninnl\0"+"349981574+johninnl@users.noreply.github.com\0"+"johninnl\0"+"349981574+johninnl@users.noreply.github.com\0fix: fixture\0";
    if(args[0]==="rev-parse")return args[1]==="--is-shallow-repository"?"false":args.at(-1).includes("refs/tags/")&&!args.at(-1).includes("^{commit}")?tagSha:head;
    if(args[0]==="show")return args[1].endsWith("morphe-signing.pub")?key:JSON.stringify(manifest);
    if(args[0]==="for-each-ref")return `${tagSha} refs/tags/${tag}`;
    if(args[0]==="ls-remote"){
      const remote=args.find(value=>value.startsWith("https://")||value.startsWith("git@"));
      const refs=remote.includes("gitlab.com")?{[`refs/heads/${branch}`]:head,[`refs/tags/${tag}`]:tagSha,[`refs/tags/${tag}^{}`]:head}:targets[remote.includes("codeberg.org")?"codeberg":"gitgud"].refs;
      return Object.entries(refs).map(([ref,sha])=>`${sha}\t${ref}`).join("\n");
    }
    if(args[0]==="fetch"||args[0]==="merge-base")return "";
    if(args[0]==="push"){
      assert.ok(args.includes("--atomic"));assert.ok(!args.some(arg=>arg.startsWith("+")||arg.startsWith("--force")));
      const name=args.find(value=>value.startsWith("https://")||value.startsWith("git@")).includes("codeberg.org")?"codeberg":"gitgud";
      targets[name].refs={[`refs/heads/${branch}`]:head,[`refs/tags/${tag}`]:tagSha,[`refs/tags/${tag}^{}`]:head};return "";
    }
    throw new Error("Unexpected fixture git command: "+args.join(" "));
  };
  const request=async(name,endpoint,options={})=>{
    calls.push({kind:"api",name,endpoint,method:options.method||"GET"});const target=targets[name];
    if(endpoint==="/user")return name==="codeberg"?{login:"JohninNL"}:{username:"JohninNL"};
    if(endpoint==="/personal_access_tokens/self")return {active:true,revoked:false,scopes:["api"]};
    if(endpoint===`/repos/JohninNL/nl-morphe-patches`)return {full_name:"JohninNL/nl-morphe-patches",owner:{login:"JohninNL"},permissions:{push:true}};
    if(endpoint===`/projects/JohninNL%2Fnl-morphe-patches`)return {path_with_namespace:"JohninNL/nl-morphe-patches",namespace:{path:"JohninNL"},permissions:{project_access:{access_level:40}}};
    if(endpoint.includes("/packages/generic/")){if(options.method==="PUT"){target.stored=options.body;assert.deepEqual(target.stored,bytes);return {};}return target.stored;}
    if(name==="codeberg"){
      if(endpoint.includes("/assets")){
        if(options.method==="POST"){const file=options.body.get("attachment");assert.deepEqual(Buffer.from(await file.arrayBuffer()),bytes);const asset={id:1,name:filename,size:bytes.length,browser_download_url:`https://codeberg.org/JohninNL/nl-morphe-patches/releases/download/${tag}/${filename}`};target.assets.push(asset);return asset;}
        return target.assets;
      }
      if(options.method==="POST"){target.release={...options.body,id:1,author:{login:"JohninNL"}};return target.release;}
      if(options.method==="PATCH"){Object.assign(target.release,options.body);return target.release;}
      return target.release;
    }
    if(options.method==="POST"){target.release={...options.body,author:{username:"JohninNL"},commit:{id:head}};return target.release;}
    return target.release;
  };
  const download=async(name,url,size)=>{calls.push({kind:"download",name,url});assert.equal(new URL(url).origin,name==="codeberg"?"https://codeberg.org":"https://gitgud.io");assert.equal(size,bytes.length);return name==="codeberg"?bytes:targets.gitgud.stored;};
  const canonicalDownload=async(url,size)=>{calls.push({kind:"canonical-download",url});assert.equal(url,manifest.download_url);assert.equal(size,bytes.length);return bytes;};
  return {cwd,head,tagSha,version,tag,filename,bytes,alternateBytes,key,manifest,receipt,env,calls,targets,git,request,download,canonicalDownload};
}
function options(f,more={}){return {cwd:f.cwd,env:f.env,git:f.git,request:f.request,download:f.download,canonicalDownload:f.canonicalDownload,...more};}

test("read-only preflight checks the owner/project and compatible refs without uploads or pushes",async t=>{
  const f=fixture(t);assert.equal((await api().preflight(options(f))).complete,true);
  assert.equal(f.calls.filter(row=>row.kind==="api"&&row.endpoint==="/user").length,1);
  assert.ok(!f.calls.some(row=>row.method&&row.method!=="GET"||row.kind==="git"&&row.args[0]==="push"));
});
test("the active mirror publishes identical signed bytes without rewriting metadata",async t=>{
  const f=fixture(t);const started=[];let release;const barrier=new Promise(resolve=>{release=resolve;});
  const request=async(name,endpoint,options)=>{if(endpoint==="/user"){started.push(name);if(started.length===1)release();await barrier;}return f.request(name,endpoint,options);};
  const result=await api().mirrorReleases(options(f,{request}));assert.equal(result.complete,true);assert.deepEqual(started.sort(),["codeberg"]);
  assert.deepEqual(Object.keys(result.targets).sort(),["codeberg"]);assert.ok(Object.values(result.targets).every(row=>row.status==="fulfilled"));
  const before=f.calls.filter(row=>row.method&&row.method!=="GET"||row.kind==="git"&&row.args[0]==="push").length;
  await api().mirrorReleases(options(f));assert.equal(f.calls.filter(row=>row.method&&row.method!=="GET"||row.kind==="git"&&row.args[0]==="push").length,before);
  assert.deepEqual(JSON.parse(fs.readFileSync(path.join(f.cwd,"patches-bundle.json"))),f.manifest);
  assert.equal(JSON.parse(fs.readFileSync(path.join(f.cwd,"build/release-safety/mirrors-result.json"))).complete,true);
});
test("a failed target records a durable rejected result without leaking details",async t=>{
  const f=fixture(t);const result=await api().mirrorReleases(options(f,{request:async(name,endpoint,options)=>{if(name==="codeberg")throw new Error("codeberg-fixture failed");return f.request(name,endpoint,options);}}));
  assert.equal(result.complete,false);assert.equal(result.targets.codeberg.status,"rejected");
  const text=fs.readFileSync(path.join(f.cwd,"build/release-safety/mirrors-result.json"),"utf8");assert.ok(!text.includes("codeberg-fixture"));assert.deepEqual(JSON.parse(text),result);
});
test("conflicting tags and non-fast-forward branches are refused before publishing",async t=>{
  const f=fixture(t);f.targets.codeberg.refs[`refs/tags/${f.tag}`]="c".repeat(40);
  assert.equal((await api().mirrorReleases(options(f))).targets.codeberg.status,"rejected");assert.equal(f.targets.codeberg.release,null);
  f.targets.codeberg.refs={[`refs/heads/main`]:"d".repeat(40)};
  const git=(args,opts)=>{if(args[0]==="merge-base")throw new Error("not ancestor");return f.git(args,opts);};
  assert.equal((await api().preflight(options(f,{git}))).targets.codeberg.status,"rejected");
});
test("wrong owners and feature/channel mismatches are rejected",async t=>{
  const f=fixture(t);const request=async(name,endpoint,options)=>endpoint==="/user"?{login:"bot",username:"bot"}:f.request(name,endpoint,options);
  assert.equal((await api().preflight(options(f,{request}))).complete,false);
  const git=(args,opts)=>args[0]==="branch"?"feature/work":f.git(args,opts);await assert.rejects(api().mirrorReleases(options(f,{git})),/main.*dev/);
  fs.writeFileSync(path.join(f.cwd,"build/release-safety/gitlab-result.json"),JSON.stringify({...f.receipt,branch:"dev"}));await assert.rejects(api().mirrorReleases(options(f)),/channel|branch/);
});
test("a conflicting existing signed asset is never replaced",async t=>{
  const f=fixture(t);await api().mirrorReleases(options(f));const changed=Buffer.from(f.bytes);changed[4]^=1;
  const result=await api().mirrorReleases(options(f,{download:async()=>changed}));assert.equal(result.complete,false);assert.ok(Object.values(result.targets).every(row=>row.status==="rejected"));
});
test("target-specific API credentials and Git headers never cross destination hosts",async()=>{
  const env={CODEBERG_TOKEN:"codeberg-only",GITGUD_TOKEN:"gitgud-only",GITLAB_TOKEN:"canonical-only"};const urls=[];
  const request=api().createRequest({env,fetcher:async(url,options)=>{urls.push(url);assert.equal(options.redirect,"error");if(url.startsWith("https://codeberg.org/")){assert.ok(options.headers.Authorization==="token codeberg-only","Codeberg auth must match its fixture");assert.equal(options.headers["PRIVATE-TOKEN"],undefined);}else{assert.ok(url.startsWith("https://gitgud.io/"));assert.ok(options.headers["PRIVATE-TOKEN"]==="gitgud-only","GitGud auth must match its fixture");assert.equal(options.headers.Authorization,undefined);}return {ok:true,status:200,json:async()=>({})};}});
  await request("codeberg","/user");await request("gitgud","/user");assert.equal(urls.length,2);
  await assert.rejects(request("codeberg","//host.invalid/user"),/path/);
  const credentials=api().gitEnvironment("gitgud",env);assert.ok(!JSON.stringify(credentials).includes("codeberg-only"));assert.equal(credentials.GIT_CONFIG_KEY_0,"http.https://gitgud.io/.extraheader");
  assert.equal(credentials.GIT_CONFIG_KEY_1,"http.followRedirects");assert.equal(credentials.GIT_CONFIG_VALUE_1,"false");
});

test("ref pushes pin immutable canonical object IDs rather than mutable local ref names",async t=>{
  const f=fixture(t);await api().mirrorReleases(options(f));
  for(const row of f.calls.filter(row=>row.kind==="git"&&row.args[0]==="push")){
    assert.ok(row.args.includes(`${f.head}:refs/heads/main`));
    assert.ok(row.args.includes(`${f.tagSha}:refs/tags/${f.tag}`));
  }
});
test("uncommitted canonical metadata and preview receipts cannot publish or retain stale success",async t=>{
  const f=fixture(t);await api().mirrorReleases(options(f));const file=path.join(f.cwd,"patches-bundle.json");
  fs.writeFileSync(file,JSON.stringify({...f.manifest,description:"Uncommitted different notes"}));
  await assert.rejects(api().mirrorReleases(options(f)),/committed|canonical/i);
  assert.equal(JSON.parse(fs.readFileSync(path.join(f.cwd,"build/release-safety/mirrors-result.json"))).complete,false);
  fs.writeFileSync(file,JSON.stringify(f.manifest));fs.writeFileSync(path.join(f.cwd,"build/release-safety/gitlab-result.json"),JSON.stringify({...f.receipt,preview:true,published:false}));
  const writes=f.calls.filter(row=>row.kind==="git"&&row.args[0]==="push"||row.method&&row.method!=="GET").length;
  await assert.rejects(api().mirrorReleases(options(f)),/preview/i);
  assert.equal(f.calls.filter(row=>row.kind==="git"&&row.args[0]==="push"||row.method&&row.method!=="GET").length,writes);
});
test("dev prereleases retain their channel and no-release receipts perform no mirror writes",async t=>{
  const f=fixture(t,"dev");assert.equal((await api().mirrorReleases(options(f))).complete,true);assert.equal(f.targets.codeberg.release.prerelease,true);
  fs.writeFileSync(path.join(f.cwd,"build/release-safety/gitlab-result.json"),JSON.stringify({published:false}));
  const before=f.calls.length,result=await api().mirrorReleases(options(f));assert.equal(result.skipped,"no-release");
  assert.ok(!f.calls.slice(before).some(row=>row.kind==="api"||row.args?.[0]==="push"));
});
test("authenticated draft redirects drop credentials before an external download",async()=>{
  const env={CODEBERG_TOKEN:"codeberg-private"};let calls=0;
  const download=api().createDownload({env,fetcher:async(url,options)=>{
    calls++;if(calls===1){assert.ok(options.headers.Authorization==="token codeberg-private","Draft auth must match its fixture");return {status:302,headers:{get:()=>"https://storage.invalid/signed-asset"}};}
    assert.equal(url,"https://storage.invalid/signed-asset");assert.equal(options.headers,undefined);return {ok:true,status:200,arrayBuffer:async()=>Buffer.from("abc")};
  }});
  assert.deepEqual(await download("codeberg","https://codeberg.org/asset",3,{authenticated:true}),Buffer.from("abc"));
});

test("shallow history cannot pass the complete Git identity audit",async t=>{
  const f=fixture(t),git=(args,options)=>args[0]==="rev-parse"&&args[1]==="--is-shallow-repository"?"true":f.git(args,options);
  await assert.rejects(api().preflight(options(f,{git})),/complete|shallow/i);
});

test("SSH ref transports select only the destination key and safely quote private paths",async t=>{
  const f=fixture(t);Object.assign(f.env,{
    GITLAB_SSH_KEY:"/private/keys/gitlab key's identity",
    CODEBERG_SSH_KEY:"/private/keys/codeberg key's identity",
    GITGUD_SSH_KEY:"/private/keys/gitgud key's identity",
    GIT_SSH_COMMAND:"inherited-untrusted-command",GIT_SSH:"inherited-ssh",
    GIT_CONFIG_COUNT:"1",GIT_CONFIG_KEY_0:"http.https://untrusted.invalid/.extraheader",GIT_CONFIG_VALUE_0:"old-header-secret",
  });
  const expected={gitlab:"git@gitlab.com:johninnld/nl-morphe-patches.git",codeberg:"git@codeberg.org:JohninNL/nl-morphe-patches.git",gitgud:"git@ssh.gitgud.io:JohninNL/nl-morphe-patches.git"};
  for(const name of Object.keys(expected)){
    assert.equal(api().gitRemote(name,f.env),expected[name]);
    const env=api().gitEnvironment(name,f.env),key=name.toUpperCase();
    assert.equal(env.GIT_SSH_COMMAND,`ssh -i '/private/keys/${name} key'\\''s identity' -o IdentitiesOnly=yes -o BatchMode=yes -o StrictHostKeyChecking=yes`);
    assert.equal(env.GIT_SSH_VARIANT,"ssh");assert.equal(env.GIT_CONFIG_COUNT,"0");
    for(const other of Object.keys(expected).filter(value=>value!==name))assert.ok(!JSON.stringify(env).includes(`/private/keys/${other}`));
    for(const variable of ["CODEBERG_TOKEN","GITGUD_TOKEN","GITLAB_TOKEN","GIT_CONFIG_KEY_0","GIT_CONFIG_VALUE_0","GIT_SSH",`${key}_SSH_KEY`])assert.equal(env[variable],undefined);
  }
  assert.equal((await api().preflight(options(f))).complete,true);
  for(const row of f.calls.filter(value=>value.kind==="git"&&value.args[0]==="ls-remote")){
    assert.ok(Object.values(expected).includes(row.args[1]));assert.ok(row.env.GIT_SSH_COMMAND.startsWith("ssh -i "));assert.equal(row.env.GITLAB_TOKEN,undefined);
  }
});

test("a different valid signed canonical upload refuses all target mutations",async t=>{
  const f=fixture(t),verify=require("../.forgejo/scripts/bundle.cjs").verifyBundle;
  assert.notEqual(verify(f.alternateBytes,f.key),verify(f.bytes,f.key));
  await assert.rejects(api().mirrorReleases(options(f,{canonicalDownload:async()=>f.alternateBytes})),/canonical|published/i);
  assert.ok(!f.calls.some(row=>row.kind==="api"||row.kind==="git"&&row.args[0]==="push"));
  assert.equal(JSON.parse(fs.readFileSync(path.join(f.cwd,"build/release-safety/mirrors-result.json"))).complete,false);
});

test("GitGud accepts fine-grained token metadata bound to the authenticated owner",async t=>{
  for(const granular of [{granular:true},{granular_scopes:["read_user","read_project"]}]){
    const f=fixture(t),request=async(name,endpoint,options)=>{
      const value=await f.request(name,endpoint,options);
      if(name==="gitgud"&&endpoint==="/user")return {...value,id:42};
      if(name==="gitgud"&&endpoint==="/personal_access_tokens/self")return {active:true,revoked:false,user_id:42,...granular};
      return value;
    };
    assert.equal((await api().preflight(options(f,{request,targets:["gitgud"]})) ).complete,true);
  }
});
test("typed fine-grained metadata denial is optional after owner and Maintainer checks",async t=>{
  const f=fixture(t),metadata=api().createRequest({env:f.env,fetcher:async()=>({status:403,ok:false,json:async()=>({error:"insufficient_granular_scope",error_description:"fixture-sensitive-detail"})})});
  await assert.rejects(metadata("gitgud","/personal_access_tokens/self"),error=>error.status===403&&error.errorCode==="insufficient_granular_scope"&&!error.message.includes("fixture-sensitive-detail"));
  const request=async(name,endpoint,options)=>name==="gitgud"&&endpoint==="/personal_access_tokens/self"?metadata(name,endpoint,options):f.request(name,endpoint,options);
  assert.equal((await api().preflight(options(f,{request,targets:["gitgud"]}))).complete,true);
  for(const failure of ["owner","maintainer"]){
    const denied=async(name,endpoint,options)=>{
      if(name==="gitgud"&&endpoint==="/user"&&failure==="owner")return {username:"other-user"};
      if(name==="gitgud"&&endpoint.startsWith("/projects/")&&failure==="maintainer")return {path_with_namespace:"JohninNL/nl-morphe-patches",namespace:{path:"JohninNL"},permissions:{project_access:{access_level:30}}};
      return request(name,endpoint,options);
    };
    assert.equal((await api().preflight(options(f,{request:denied,targets:["gitgud"]}))).targets.gitgud.status,"rejected");
  }
});
test("opaque metadata 403 responses remain failures without leaking response bodies",async t=>{
  for(const json of [async()=>({error:"insufficient_scope",message:"fixture-sensitive-detail"}),async()=>{throw new Error("fixture-sensitive-detail");}]){
    const f=fixture(t),metadata=api().createRequest({env:f.env,fetcher:async()=>({status:403,ok:false,json})});
    await assert.rejects(metadata("gitgud","/personal_access_tokens/self"),error=>error.status===403&&error.errorCode===undefined&&!error.message.includes("fixture-sensitive-detail"));
    const request=async(name,endpoint,options)=>name==="gitgud"&&endpoint==="/personal_access_tokens/self"?metadata(name,endpoint,options):f.request(name,endpoint,options);
    const result=await api().preflight(options(f,{request,targets:["gitgud"]}));assert.equal(result.complete,false);assert.equal(result.targets.gitgud.status,"rejected");
  }
});
test("inactive, revoked, foreign-user and read-api-only metadata are rejected",async t=>{
  for(const metadata of [{scopes:["api"],active:false},{scopes:["api"],revoked:true},{scopes:["api"],user_id:43},{scopes:["read_api"]}]){
    const f=fixture(t),request=async(name,endpoint,options)=>{
      if(name==="gitgud"&&endpoint==="/personal_access_tokens/self")return metadata;
      const value=await f.request(name,endpoint,options);return name==="gitgud"&&endpoint==="/user"?{...value,id:42}:value;
    };
    assert.equal((await api().preflight(options(f,{request,targets:["gitgud"]}))).targets.gitgud.status,"rejected");
  }
});
