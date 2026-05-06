import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";

const debugPanelJsPath = path.resolve("src/main/resources/static/components/DebugPanel.js");
const appJsPath = path.resolve("src/main/resources/static/app.js");
const localStorageJsPath = path.resolve("src/main/resources/static/services/local-storage.js");
const conversationPageJsPath = path.resolve("src/main/resources/static/components/ConversationPage.js");
const homePageJsPath = path.resolve("src/main/resources/static/components/HomePage.js");

const debugPanelSource = fs.readFileSync(debugPanelJsPath, "utf8");
const appSource = fs.readFileSync(appJsPath, "utf8");
const localStorageSource = fs.readFileSync(localStorageJsPath, "utf8");
const conversationPageSource = fs.readFileSync(conversationPageJsPath, "utf8");
const homePageSource = fs.readFileSync(homePageJsPath, "utf8");

assert.match(debugPanelSource, /collapsed = false/);
assert.match(debugPanelSource, /onToggleCollapsed/);
assert.match(debugPanelSource, /关闭 Debug Panel/);
assert.match(debugPanelSource, /展开 Debug Panel/);
assert.match(debugPanelSource, /debug-panel-summary/);
assert.match(conversationPageSource, /debug-drawer-toggle/);
assert.match(conversationPageSource, /debug-drawer-shell/);
assert.match(conversationPageSource, /debug-drawer-backdrop/);
assert.match(homePageSource, /debug-drawer-toggle/);
assert.match(homePageSource, /debug-drawer-shell/);
assert.match(appSource, /debugPanelCollapsed/);
assert.match(appSource, /loadDebugPanelCollapsedPreference/);
assert.match(appSource, /saveDebugPanelCollapsedPreference/);
assert.match(localStorageSource, /DEBUG_PANEL_COLLAPSED_STORAGE_KEY/);
assert.match(localStorageSource, /loadDebugPanelCollapsedPreference/);
assert.match(localStorageSource, /saveDebugPanelCollapsedPreference/);

console.log("debug-panel-collapse test passed");
