/**
 * 会话记忆语义层。
 *
 * <p>当前阶段保留项目自己的 memory 接口，但底层实现已经迁移到 Spring AI {@code ChatMemory}，
 * 以便后续平滑接入 Advisor 和模型上下文注入。
 */
package org.example.ggbot.memory;
