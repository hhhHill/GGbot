# 多租户用户系统设计

## 背景

GGbot 当前已经具备 Web Chat 和飞书机器人入口，但用户、租户、会话、消息、记忆仍未建立统一的持久化域模型。现有链路主要基于 `sessionId` 和内存态会话服务运行，无法满足以下要求：

- 飞书多企业租户隔离
- Web 个人空间与企业空间切换
- 群聊共享会话和共享 memory
- 平台无关的内部 `user_id`
- 后续 Web 与飞书账号绑定
- 基于 `org_id + subject` 的稳定权限模型

本设计目标是在不直接打断现有可运行链路的前提下，新增一套清晰的多租户持久化模型和访问控制模型，并让数据库成为 Agent 上下文的唯一业务真相源。

## 已确认决策

- 技术栈沿用现有 `Spring Boot + Spring Data JPA`
- 采用渐进式接入，不直接替换现有 `/api/chat/send` 和现有内存会话链路
- 新增独立的多租户会话域、新 API、新持久化表
- Agent 上下文从数据库中的 `conversation/message/memory` 组装，不再以内存 `ChatMemory` 作为业务真相源
- 第一版提示词上下文由“当前 conversation 最近 N 条 messages + 当前 subject 下 GLOBAL memory”构成
- `CONVERSATION` 级 memory 第一版先落表和查询，但不参与首版提示词装配
- Web 侧采用稳定匿名身份：服务端签发 `web_user_key`，接口层可兼容旧 `sessionId`
- 飞书身份严格按 `provider=feishu + tenant_key + open_id` 唯一确定
- 不进行任何自动账号合并，账号合并只允许通过后续绑定流程显式触发
- Web 需要显式新建会话能力；飞书 `p2p/group` 默认复用最近一个 active conversation

## 设计目标

1. 保证不同 `organization` 之间的数据完全隔离
2. 保证同一自然人在不同平台、不同租户下可以拥有不同 identity，但共享内部 `users.id` 的能力被保留下来
3. 保证所有 conversation、message、memory 查询必须显式带 `org_id`
4. 保证会话和记忆都挂载在 `subject`，而不是直接挂在 `user_id`
5. 保证群聊数据访问依赖 `group_members`，个人数据访问依赖当前 org 下的个人 `subject`
6. 为未来账号绑定、用户合并、更多平台接入保留明确扩展点

## 非目标

- 本期不直接替换旧 Web API 和旧前端
- 本期不实现完整账号绑定闭环，只提供服务骨架和接口
- 本期不实现基于向量召回的 memory 检索
- 本期不做自动账号合并
- 本期不做跨组织共享 subject 或共享 memory

## 领域模型

### User

`users` 代表系统中的“人”，是平台无关实体。

- 主键是内部 `user_id`
- 不使用手机号、邮箱、飞书 `open_id` 作为主键
- `users` 不直接携带租户语义

### UserIdentity

`user_identities` 代表用户在某个平台上的身份。

- Web 身份：`provider=web`
- 飞书身份：`provider=feishu`
- 唯一键：`(provider, tenant_key, provider_id)`

该设计避免以下风险：

- 只按 `open_id` 查人，造成跨飞书企业串号
- 把 Web 会话 ID 直接当成系统用户主键
- 后续引入更多平台时模型失真

### Organization

`organizations` 是系统唯一租户边界。

- 飞书企业通过 `tenant_key` 映射到 `organization`
- Web 用户若没有企业空间，则自动拥有一个 personal organization
- 所有业务数据必须显式属于某个 `org_id`

第一版组织类型：

- `feishu_tenant`
- `personal`
- `system`

### UserOrg

`user_orgs` 表示用户与组织之间的成员关系。

- 用于组织切换
- 用于离职/退出后的访问控制
- 历史数据保留，不因离职删除用户和会话

权限判断时必须要求 `status=active`。

### Subject

`subjects` 是业务数据的归属主体，是本设计的核心抽象。

存在两种主体：

- `type=user`：表示某个用户在某个组织下的个人上下文
- `type=group`：表示某个群在某个组织下的共享上下文

关键点：

- 同一个 `user_id` 在不同 `org_id` 下必须有不同的 `user subject`
- 飞书群的 `chat_id` 只作为 `group subject.ref_id`
- conversation 和 memory 统一挂到 `subject`

### GroupMember

`group_members` 表示某个用户是否属于某个群 subject。

- 用于群聊记录可见性
- 用于群聊共享 memory 可见性
- 用户退出群后，将 `status` 标记为 `left`

### Conversation / Message / Memory

三者都显式带 `org_id`，并且：

- `conversation` 归属于 `subject`
- `message` 归属于 `conversation`
- `memory` 归属于 `subject`

这样做的原因：

- 避免通过 `user_id` 直接查出跨组织数据
- 避免只靠 conversation 关联再反推 org，导致查询遗漏租户条件
- 让数据库查询天然具备多租户约束能力

## 数据模型与关键约束

### users

- 内部用户表
- `status` 使用枚举 `active/inactive`

### organizations

- 唯一约束：`unique(provider, tenant_key)`
- 飞书：`provider=feishu, org_type=feishu_tenant`
- Web 个人空间：`provider=web, org_type=personal`

### user_identities

- `tenant_key` 非空
- Web 固定为 `GLOBAL`
- 唯一约束：`unique(provider, tenant_key, provider_id)`

### user_orgs

- 唯一约束：`unique(user_id, org_id)`
- 退出组织时标记 `left`，不删除

### subjects

- 唯一约束：`unique(org_id, type, provider, ref_id)`
- 个人 subject：`provider=system, ref_id=String.valueOf(userId)`
- 飞书群 subject：`provider=feishu, ref_id=chatId`

### group_members

- 唯一约束：`unique(group_subject_id, user_id)`
- 冗余 `org_id`，便于权限与查询隔离

### conversations

- 必须带 `org_id, subject_id`
- 索引：
  - `(org_id, subject_id, last_message_at)`
  - `(org_id, last_message_at)`

### messages

- 必须带 `org_id, conversation_id`
- 索引：
  - `(org_id, conversation_id, created_at)`
- 对 `provider_message_id` 建立租户内唯一约束，用于飞书消息幂等

### memory

- 必须带 `org_id, subject_id`
- 索引：
  - `(org_id, subject_id)`
  - `(org_id, memory_type, scope)`

## 枚举设计

第一版引入以下枚举：

- `ProviderType`
- `OrganizationType`
- `OrgStatus`
- `SubjectType`
- `UserStatus`
- `UserOrgStatus`
- `UserOrgRole`
- `MemberStatus`
- `MessageRole`
- `ConversationStatus`
- `MemoryType`
- `MemoryScope`

JPA 中统一按字符串持久化，避免枚举顺序变更引起脏数据。

## 权限模型

权限统一收口到 `AccessControlService`，避免控制器和业务服务各自拼条件。

### 组织访问

用户可以访问组织，当且仅当：

- `user_orgs.user_id = currentUserId`
- `user_orgs.org_id = currentOrgId`
- `user_orgs.status = active`

### Subject 访问

前置条件：

- 用户必须是当前组织的 active 成员
- `subject.org_id` 必须等于当前 `org_id`

之后分两类判断：

- `subject.type=user`
  - 仅当 `subject.provider=system`
  - 且 `subject.ref_id = String.valueOf(currentUserId)` 时可访问
- `subject.type=group`
  - 仅当 `group_members.org_id = orgId`
  - 且 `group_members.group_subject_id = subjectId`
  - 且 `group_members.user_id = currentUserId`
  - 且 `group_members.status = active` 时可访问

### Conversation 访问

不能只根据 `conversationId` 返回消息。必须：

1. 先按 `id + org_id` 查到 conversation
2. 取出 `subject_id`
3. 走 `checkCanAccessSubject`

### 为什么所有查询必须带 org_id

仅凭 `user_id` 或 `conversation_id` 查询存在两个风险：

1. 同一用户可能属于多个组织，直接按 `user_id` 查会把不同组织的私聊、群聊混在一起
2. 即使 `conversation_id` 全局唯一，服务代码仍可能在 join 或缓存路径中误用其他组织数据

因此：

- conversation/message/memory 的 repository 方法必须显式接收 `orgId`
- 不提供无 `orgId` 参数的列表查询方法

## 租户解析模型

新增 `TenantContext`，至少包含：

- `userId`
- `orgId`
- `provider`
- `tenantKey`

### 飞书请求

- 不允许外部直接传 `orgId`
- 必须通过 `tenant_key -> organization` 映射得到 `orgId`
- `tenant_key` 是飞书租户的来源真相

### Web 请求

- 若前端传 `orgId`，必须校验 `user_orgs.status=active`
- 若未传 `orgId`，则默认回落到当前用户 personal organization
- 第一版不做全局默认组织共享

## 服务设计

### OrganizationService

职责：

- `tenant_key -> organization` 映射
- personal organization 自动创建
- 组织成员校验

关键方法：

- `getOrCreateFeishuOrganization`
- `getOrCreatePersonalOrganization`
- `checkUserActiveInOrg`

### IdentityService

职责：

- 平台身份解析为内部用户
- 自动补齐 personal org、user_org、user subject
- 维持 Web 与飞书身份模型一致性

关键方法：

- `getOrCreateUserByWebSession`
- `getOrCreateUserByFeishuOpenId`

注意事项：

- Web 持久化身份不直接使用短期 `sessionId`
- 飞书 identity 查询必须同时带 `tenant_key`
- 不基于昵称、邮箱、手机号自动合并账号

### SubjectService

职责：

- 维护个人 subject
- 维护飞书群 subject
- 维护群成员状态

关键方法：

- `getOrCreateUserSubject`
- `getOrCreateFeishuGroupSubject`
- `ensureGroupMember`
- `markGroupMemberLeft`

### AccessControlService

职责：

- 唯一权限入口
- 组织、subject、conversation 权限统一校验

### ConversationService

职责：

- 创建和复用会话
- 写入消息
- 查询当前用户在当前 org 下可访问的会话

第一版复用策略：

- Web：支持显式新建会话；没有指定时可创建新会话
- 飞书：按 `org + subject + source=feishu` 复用最近 active 会话

### MemoryService

职责：

- 写入 memory
- 查询当前用户在当前 org 下可访问的 memory
- 查询指定 subject 的 memory

### PersistentConversationContextService

新增上下文装配服务，职责如下：

- 根据 `orgId + conversationId` 查询最近 N 条消息
- 根据 `orgId + subjectId` 查询 `GLOBAL` memory
- 组装成 Agent 所需上下文

该服务让 Agent 输入不再依赖内存 `ChatMemory`。

### FeishuMessageHandler

职责：

- 从飞书事件中解析真实租户
- 建立飞书 identity、组织、subject、group member
- 查找或创建飞书会话
- 持久化消息并调用 Agent

关键规则：

- `orgId` 必须来自 `tenant_key` 映射
- `group chat_id` 只在当前 tenant 对应 org 范围内有效

### AccountBindingService

第一版只提供骨架：

- `createBindToken`
- `bindFeishuIdentity`
- `mergeUsers`

`mergeUsers` 暂不完整实现，但要清晰标注未来迁移范围：

- `user_identities.user_id`
- `user_orgs.user_id`
- `group_members.user_id`
- `messages.sender_user_id`
- 各组织下个人 `subject` 的 conversation / memory 迁移

## 控制器与 API

### 新增 API

- `POST /api/web/chat/send`
- `GET /api/orgs`
- `POST /api/orgs/switch`
- `GET /api/conversations`
- `GET /api/conversations/{conversationId}/messages`
- `GET /api/memory`
- `GET /api/subjects/{subjectId}/memory`
- `POST /api/bind/feishu/token`

### Web Chat

请求处理步骤：

1. 解析稳定 Web identity
2. 解析当前 org，优先使用前端传入 orgId，否则回退到 personal org
3. 校验用户在当前 org 下 active
4. 获取当前 org 下的个人 subject
5. 查找或创建 conversation
6. 持久化用户消息
7. 用数据库上下文调用 Agent
8. 持久化 assistant 回复

### Conversations 列表

列表查询不能按 `user_id` 直接查 conversation，必须先求当前 org 下用户可访问的 `subject_id` 集合：

- 个人 subject
- active 群成员对应的 group subject

之后再在 `conversations.org_id = currentOrgId` 范围内查询。

### Messages 列表

1. 先做 `checkCanAccessConversation`
2. 再按 `org_id + conversation_id` 查消息

### Memory 列表

1. 先求当前 org 下可访问的 `subject_id`
2. 再按 `org_id + subject_id in (...)` 查 memory

## 数据流

### Web 私聊

`web_user_key -> user -> personal/current org -> user subject -> conversation -> message -> agent -> assistant message`

### 飞书私聊

`tenant_key -> org -> (tenant_key, open_id) -> user -> user subject -> conversation -> message -> agent -> assistant message`

### 飞书群聊

`tenant_key -> org -> (tenant_key, open_id) -> user -> group subject(chat_id) -> ensureGroupMember -> conversation -> message -> agent -> assistant message`

## 异常模型

若项目没有统一异常体系，新增：

- `ForbiddenException`
- `NotFoundException`
- `BadRequestException`

并统一由全局异常处理器转成标准 API 响应。

## 迁移策略

### 阶段 1

- 新增表结构、实体、仓库、枚举、异常、服务
- 新增多租户 API
- 新增飞书消息处理服务，但不强制替换旧 webhook 主入口

### 阶段 2

- 让新 Web Chat API 使用数据库上下文调用 Agent
- 让飞书新链路使用数据库上下文调用 Agent

### 阶段 3

- 前端切换到新 API
- 评估并逐步下线旧内存 session / memory 作为业务主链路

## 测试策略

至少覆盖以下测试：

- Organization 创建与唯一约束
- Web personal organization 自动创建
- 飞书同 `open_id` 在不同 `tenant_key` 下不会串号
- 用户访问非所属 org 时被拒绝
- 用户访问其他人的 user subject 时被拒绝
- 用户退出群后无法再访问群会话和群 memory
- `listAccessibleConversations` 仅返回当前 org 下个人和所在群的数据
- `listMessages` 必须同时受 `org` 和 `subject` 权限控制
- 群聊消息写入时 `ensureGroupMember` 自动补齐

## 风险与约束

### 双链路并存

在渐进式迁移阶段，旧接口和新接口会并存。必须明确：

- 新持久化域是后续业务真相源
- 旧内存链路只作为过渡能力，不再承载新需求

### Web 匿名身份限制

第一版 Web 使用稳定匿名身份而非正式账号体系，因此：

- 清理浏览器 cookie 后用户仍可能失去身份关联
- 跨设备天然不会自动合并

这是可接受的阶段性限制，但后续若要强化账号体系，需要引入正式登录。

### 用户合并复杂度

由于 conversation 和 memory 挂在 `subject`，用户合并不能只改几张外键表。必须明确处理各组织下的个人 subject 收敛策略，否则会导致权限判断失效或历史遗失。

## 推荐实现结构

- `entity`
- `repository`
- `service`
- `controller`
- `dto`
- `exception`
- `enums`

包结构建议落在独立子域，例如：

- `org.example.ggbot.user`
- `org.example.ggbot.organization`
- `org.example.ggbot.subject`
- `org.example.ggbot.conversation`
- `org.example.ggbot.memory`
- `org.example.ggbot.access`
- `org.example.ggbot.adapter.web`
- `org.example.ggbot.adapter.feishu`

## 结论

第一版采用“新增独立多租户持久化域 + 新 API + 数据库上下文驱动 Agent”的渐进式方案，可以在不破坏现有可运行链路的情况下，把租户隔离、身份映射、subject 权限和群共享 memory 这些核心约束一次性做对。这是当前项目最稳妥、可维护、也最利于后续账号绑定和多平台扩展的路线。
