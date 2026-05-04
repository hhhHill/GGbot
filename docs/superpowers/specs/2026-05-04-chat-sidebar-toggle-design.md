# Chat Sidebar Toggle Design

**Goal**

为 Web 聊天前端增加一个侧栏折叠/展开按钮，允许用户在桌面端和移动端收起会话记录栏，并在刷新后保留上次的展开状态。

**Scope**

- 在左侧会话栏顶部增加折叠/展开按钮
- 复用现有静态前端结构：`Sidebar.js`、`app.js`、`app.css`
- 通过 `localStorage` 持久化侧栏折叠状态
- 不修改后端接口，不改变会话数据结构

**Approach**

1. 在 `local-storage.js` 新增侧栏偏好读写函数：
   - `loadSidebarCollapsedPreference()`
   - `saveSidebarCollapsedPreference(collapsed)`
2. `app.js` 在初始化时读取偏好，持有 `sidebarCollapsed` 状态，并在变化时写回本地存储
3. `Sidebar.js` 接收：
   - `collapsed`
   - `onToggleCollapsed`
4. 侧栏顶部按钮在两种状态下都可见：
   - 展开时显示“收起”
   - 收起时显示“展开”
5. `app.css` 通过 `sidebar collapsed` 样式控制：
   - 展开时显示品牌、新建按钮、会话列表
   - 收起时缩窄为保留按钮入口的窄栏，并隐藏会话列表和次要文案

**Error Handling**

- `localStorage` 不可用时退化为仅运行时状态，不阻塞页面使用
- 初始值读取失败时默认按展开处理
- 收起状态下仍保留可点击入口，避免用户无法恢复侧栏

**Testing**

- `sidebar-preference.test.mjs`
  - 默认值为展开
  - 能持久化 `true/false`
  - 已存储值能正确恢复
- 保持现有 `app-merge-session.test.mjs` 继续通过，避免影响已有会话逻辑
