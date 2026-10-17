// phone-shots.mjs: full-page screenshots of posts at a real phone width (375px), for the T102 before/after
// checks. screenshots.sh can't do this: on macOS, headless Chrome won't make a window narrower than about
// 500px, so its "375px" shots are a 375px crop of a wider layout. This script drives Chrome over the
// DevTools protocol instead and emulates a 375px mobile viewport, which is what a phone gets.
//
// Usage: node --experimental-websocket phone-shots.mjs <out_dir> <base_url> <slug>...
//   base_url  where the build is served, for example http://localhost:8202 (jwebserver -p 8202 -d <build>)
//   slug      a post's base name ("java-live" shoots /blog/java-live.html), or "drafts/<name>-draft"
// Writes <out_dir>/<slug>-375.png and prints, per page, the document's scroll width against the viewport:
// "fits" or "SIDEWAYS SCROLL" (a page that scrolls sideways on a phone is a bug; T046 fixed 253 of them).
// Compare before/after directories with pixeldiff.py. Chrome is started on DevTools port 9333 with a
// throwaway profile in <out_dir>/.chrome-cdp and stopped at the end. Node 21+ (22+ needs no flag).
import { spawn } from "node:child_process";
import fs from "node:fs";
import path from "node:path";

const [out, base, ...slugs] = process.argv.slice(2);
if (!out || !base || slugs.length === 0) {
  console.error("usage: node --experimental-websocket phone-shots.mjs <out_dir> <base_url> <slug>...");
  process.exit(2);
}
const PORT = process.env.CDP_PORT || "9333";
const CHROME = process.env.CHROME || "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
fs.mkdirSync(path.join(out, ".chrome-cdp"), { recursive: true });
const chrome = spawn(CHROME, ["--headless=new", "--disable-gpu", "--no-first-run", "--no-default-browser-check",
  `--remote-debugging-port=${PORT}`, `--user-data-dir=${path.join(out, ".chrome-cdp")}`, "about:blank"],
  { stdio: "ignore" });
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
try {
  let ready = false;
  for (let i = 0; i < 100 && !ready; i++) {
    try { await fetch(`http://127.0.0.1:${PORT}/json/version`); ready = true; } catch { await sleep(100); }
  }
  if (!ready) throw new Error(`Chrome did not open DevTools on port ${PORT}`);
  let failures = 0;
  for (const slug of slugs) {
    const url = `${base.replace(/\/$/, "")}/blog/${slug}.html`;
    const file = path.join(out, `${slug.replace(/\//g, "-")}-375.png`);
    try {
      await shoot(url, file);
    } catch (e) {
      failures++;
      console.log(`${slug}: FAILED ${e.message}`);
    }
  }
  process.exitCode = failures ? 1 : 0;
} finally {
  chrome.kill();
}

async function shoot(url, file) {
  const target = await (await fetch(`http://127.0.0.1:${PORT}/json/new?about:blank`, { method: "PUT" })).json();
  const ws = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise((resolve, reject) => { ws.onopen = resolve; ws.onerror = reject; });
  let id = 0;
  const pending = new Map();
  let loaded = false;
  ws.onmessage = (m) => {
    const msg = JSON.parse(m.data);
    if (msg.id && pending.has(msg.id)) { pending.get(msg.id)(msg); pending.delete(msg.id); }
    else if (msg.method === "Page.loadEventFired") loaded = true;
  };
  const send = (method, params = {}) => new Promise((resolve) => {
    const i = ++id; pending.set(i, resolve); ws.send(JSON.stringify({ id: i, method, params }));
  });
  try {
    await send("Page.enable");
    await send("Network.enable");
    await send("Network.setCacheDisabled", { cacheDisabled: true });
    await send("Emulation.setDeviceMetricsOverride", { width: 375, height: 800, deviceScaleFactor: 1, mobile: true });
    await send("Page.navigate", { url });
    for (let i = 0; i < 200 && !loaded; i++) await sleep(50);
    if (!loaded) throw new Error("page did not load in 10 s");
    await sleep(1500); // web fonts and the deferred syntax highlighter
    const evaluate = async (expression) =>
      (await send("Runtime.evaluate", { expression, returnByValue: true })).result.result.value;
    const info = JSON.parse(await evaluate(`JSON.stringify({
      width: document.documentElement.clientWidth,
      scrollWidth: document.documentElement.scrollWidth,
      height: document.documentElement.scrollHeight })`));
    await send("Emulation.setDeviceMetricsOverride",
      { width: 375, height: Math.min(info.height, 16000), deviceScaleFactor: 1, mobile: true });
    await sleep(300);
    const shot = await send("Page.captureScreenshot", { format: "png", captureBeyondViewport: true });
    fs.writeFileSync(file, Buffer.from(shot.result.data, "base64"));
    const fit = info.scrollWidth <= info.width ? "fits" : `SIDEWAYS SCROLL (${info.scrollWidth}px wide)`;
    console.log(`${path.basename(file)}: ${info.width}x${info.height}, ${fit}`);
  } finally {
    await send("Target.closeTarget", { targetId: target.id });
    ws.close();
  }
}
