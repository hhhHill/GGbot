import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";

const localStorageJsPath = path.resolve("src/main/resources/static/services/local-storage.js");
const localStorageSource = fs.readFileSync(localStorageJsPath, "utf8");

const transformedModule = localStorageSource.replace(/export function /g, "function ");

const createStorageHarness = (initialValues = {}) => {
    const store = new Map(Object.entries(initialValues));
    return {
        getItem(key) {
            return store.has(key) ? store.get(key) : null;
        },
        setItem(key, value) {
            store.set(key, value);
        },
        dump() {
            return Object.fromEntries(store.entries());
        }
    };
};

const storage = createStorageHarness();
const api = vm.runInNewContext(`
${transformedModule}
({
    loadSidebarCollapsedPreference,
    saveSidebarCollapsedPreference
});
`, {
    window: {
        localStorage: storage
    }
});

assert.equal(api.loadSidebarCollapsedPreference(), false);

api.saveSidebarCollapsedPreference(true);
assert.equal(api.loadSidebarCollapsedPreference(), true);

api.saveSidebarCollapsedPreference(false);
assert.equal(api.loadSidebarCollapsedPreference(), false);

const persistedValues = storage.dump();
assert.equal(persistedValues.ggbot_chat_sidebar_collapsed, "false");

const truthyStorage = createStorageHarness({
    ggbot_chat_sidebar_collapsed: "true"
});
const truthyApi = vm.runInNewContext(`
${transformedModule}
({
    loadSidebarCollapsedPreference,
    saveSidebarCollapsedPreference
});
`, {
    window: {
        localStorage: truthyStorage
    }
});

assert.equal(truthyApi.loadSidebarCollapsedPreference(), true);

console.log("sidebar-preference test passed");
