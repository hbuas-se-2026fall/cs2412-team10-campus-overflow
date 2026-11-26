# Lab 6 Sprint3 任务认领（Ke-1226）

> Sprint：Sprint 3　|　实验：Lab 6　|　认领人：柯博（GitHub：Ke-1226）
> 角色：Dev　|　模块范围：`co-qa/**`、`co-reputation/**`　|　认领日期：2026-11-26
> 依据：Lab6-Sprint3-分工方案-v1.1（PO 唐志烽，基线 main @010487c）

## 一、认领范围

| 任务编号 | 任务名称 | 交付判据 | 依赖 | 状态 |
| --- | --- | --- | --- | --- |
| T-055 | 投票 / 评论 / 标签与 Feature 逐条对照，补齐重复投票、自赞限制、标签上限校验 | 每条 Gherkin 有对应测试或 Postman 用例，差异登记为缺陷或文档修订 | T-059 | 对照表见 `docs/lab6/acceptance-qa.md` |
| T-056 | 采纳与教师认证（endorse）权限矩阵，越权返回 403 | 权限矩阵四类身份全组合有测试覆盖 | T-059 | 见 `US019PermissionMatrixTest` |
| T-057 | 声誉事件链路验收（采纳 +15、投票 ±、账本与账户一致、排行榜刷新） | 账本条目与账户余额可对照，`ReconciliationService` 校验无差异 | T-055、T-056 | 见 `ReputationChainAcceptanceTest` |
| T-058 | 跨模块 BUG 修复 1–2 个并登记缺陷台账 | 每个缺陷含复现步骤 / 根因 / 修复 PR / 回归测试 | 随时 | 缺陷台账见 `docs/lab6/defects.md` |

## 二、Sprint 3 内已完成的 Reputation 侧工作

`co-reputation/**` 的领域模型（`AcademicTerm`、`BadgePolicy`、`Bounty`、`ReputationAccount`、`ReputationRulebook`）已于 2026-11-05 由本人在 `4d34d99` 实现完毕，本轮只做验收，不再改动模型。

## 三、本轮交付物

1. `co-qa/**`、`co-reputation/**` 的验收测试与缺陷修复代码（本 PR）。
2. `docs/lab6/defects.md`：缺陷台账，按 T-058 判据逐条记录。
3. `docs/lab6/acceptance-qa.md`：T-055 的 Gherkin ↔ 测试用例对照表。

本文件用于满足分工方案「以一次 PR 的合并记录作为任务认领签字」的要求，认领 T-055 至 T-058 四项任务。
