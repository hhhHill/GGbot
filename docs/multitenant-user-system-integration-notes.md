# Multitenant User System Integration Notes

## Scope

This branch adds a persistent multitenant user and conversation domain for Web chat and Feishu bot access.

Implemented areas:

- tenant-scoped schema and JPA entities for `users`, `organizations`, `user_identities`, `user_orgs`, `subjects`, `group_members`, `conversations`, `messages`, and `memory`
- bootstrap services for Web and Feishu identities
- tenant-aware access control for org, subject, and conversation access
- persistent agent context based on conversation messages and subject global memory
- Web APIs for chat, organizations, conversations, messages, memory, and bind-token creation
- Feishu webhook handling through `tenant_key -> org -> subject -> conversation -> messages`

## Web APIs

- `POST /api/web/chat/send`
  - resolves the current user from `web_user_key` cookie, with `sessionId` fallback for compatibility
  - resolves `orgId` from request or falls back to the user's personal organization
  - persists user and assistant messages into the tenant-scoped conversation
- `GET /api/orgs`
  - lists active organizations from `user_orgs`
- `POST /api/orgs/switch`
  - validates active membership only
  - current org persistence remains a frontend concern
- `GET /api/conversations`
  - requires `orgId`
  - returns only conversations accessible through the current org's user subject or active group memberships
- `GET /api/conversations/{conversationId}/messages`
  - requires `orgId`
  - enforces conversation access through `AccessControlService`
- `GET /api/memory`
  - requires `orgId`
  - returns memory accessible through the same subject-access calculation
- `GET /api/subjects/{subjectId}/memory`
  - requires `orgId`
  - enforces subject access before returning tenant-scoped memory
- `POST /api/bind/feishu/token`
  - creates a temporary bind token for the current user and current org

## Feishu Flow

`/feishu/webhook` now handles `im.message.receive_v1` with the persistent multitenant flow:

1. Parse `tenant_key`, `open_id`, `chat_id`, `chat_type`, `message_id`, and text content.
2. Resolve or create the Feishu organization from `tenant_key`.
3. Resolve or create the platform-neutral user from `tenant_key + open_id`.
4. Resolve the current subject:
   - `p2p` -> current org's user subject
   - `group` -> current org's Feishu group subject, plus active group membership
5. Resolve or create the active Feishu conversation.
6. Persist the inbound user message.
7. Build agent context from persisted messages plus subject global memory.
8. Persist the assistant reply and send it back to Feishu.

## Current Boundaries

- `AccountBindingService` currently stores bind tokens in-memory.
  - production should move this to Redis with expiry
- `bindFeishuIdentity` and `mergeUsers` are intentionally left as TODO
  - merge will need both identity/user-org migration and per-org user-subject reconciliation
- Web chat currently creates a new conversation when `conversationId` is absent
  - continuing an existing Web conversation requires passing `conversationId`
- Feishu tenant display name is best-effort
  - when the webhook payload does not include a tenant name, the implementation falls back to `tenant_key`
- Feishu send currently replies by `chat_id`
  - this matches the current bot adapter and avoids introducing a second reply channel model in this step

## Verification

Verified with full project test suite:

```bash
mvn test
```

Latest full run in this branch passed with `90` tests and `0` failures.
