import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";

const sidebarJsPath = path.resolve("src/main/resources/static/components/Sidebar.js");
const sidebarSource = fs.readFileSync(sidebarJsPath, "utf8");

assert.match(sidebarSource, /className="collapsed-brand-glyph">G</);
assert.match(sidebarSource, /collapsed-toggle-text">展开</);
assert.match(sidebarSource, /collapsed \? html`/);
assert.match(sidebarSource, /<button className="primary-button sidebar-create-button"/);

console.log("sidebar-collapsed-ui test passed");
