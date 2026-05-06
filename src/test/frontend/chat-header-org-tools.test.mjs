import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";

const debugPanelJsPath = path.resolve("src/main/resources/static/components/DebugPanel.js");
const chatAreaJsPath = path.resolve("src/main/resources/static/components/ChatArea.js");
const chatHeaderJsPath = path.resolve("src/main/resources/static/components/ChatHeader.js");

const debugPanelSource = fs.readFileSync(debugPanelJsPath, "utf8");
const chatAreaSource = fs.readFileSync(chatAreaJsPath, "utf8");
const chatHeaderSource = fs.readFileSync(chatHeaderJsPath, "utf8");

assert.match(debugPanelSource, /Debug Panel/);
assert.match(debugPanelSource, /账号信息/);
assert.match(debugPanelSource, /Workspace 选择/);
assert.match(debugPanelSource, /API Key 管理/);
assert.match(debugPanelSource, /className="chat-org-switch"/);
assert.match(debugPanelSource, /className="chat-auth-input"/);
assert.match(debugPanelSource, /生成飞书绑定码/);
assert.match(debugPanelSource, /退出登录/);
assert.match(chatAreaSource, /className="chat-area"/);
assert.match(chatAreaSource, /<\$\{ChatHeader\} title=\$\{title\}/);
assert.doesNotMatch(chatHeaderSource, /生成飞书绑定码/);
assert.doesNotMatch(chatHeaderSource, /chat-org-switch/);

console.log("debug-panel-and-chat-area structure test passed");
