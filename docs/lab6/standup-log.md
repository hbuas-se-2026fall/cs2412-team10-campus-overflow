# Sprint 3 每日站会记录

> 主持：SM（左焱之 / Flagg32）｜任务：T-062 每日站会主持
> 记录规则：每人三问（昨日完成 / 今日计划 / 阻塞），只记录本人陈述与可核查证据（分支名、PR 号、CI 运行号、commit SHA）；未提供陈述的成员记「未回报」，不代为填写。

## 记录模板

```
日期：
出席 / 缺席：
关键路径状态（T-049 索引投影是否可用）：
| 成员 | 昨日完成 | 今日计划 | 阻塞 | 证据（分支/PR/SHA） |
待升级问题（需 PO 或跨团队协调）：
开放 PR 存活时长点检（>24h 未评审项）：
```

---

## 2026-11-19（周四，Sprint 3 T+0 / Planning 日）

出席 / 缺席：SM 在岗；其余成员今日进度以仓库痕迹为准（Planning 当日未收到书面回报，记「未回报」）。

关键路径状态：**搜索侧已打通**——`dis_search_doc` 投影、查询、相关问题推荐均已在 main 上，索引表可写入。

| 成员 | 昨日完成 | 今日计划 | 阻塞 | 证据 |
| --- | --- | --- | --- | --- |
| 江思达（T-049~051） | 提交搜索索引投影、搜索查询与相关推荐，含 ADR-010 与 11 例单测 | 按 PR 评审意见处理后续；D+5 前配合验证索引时延 | 无 | PR #23，merge `c8404b25` |
| 何欣怡（T-052~054） | 未回报 | 未回报（通知链路 T-052~054 尚无分支） | 未回报 | — |
| 柯博（T-055~058） | 未回报 | 未回报（依赖 T-059 的 Gherkin 基线） | 未回报 | — |
| 唐志烽（T-059~061） | 未回报 | 未回报（US-016~019 与 sprint-3-backlog 尚未入库） | 未回报 | — |
| SM（Flagg32，T-062~066） | 复核并合并 PR #23；排除 CI 偶发失败；建立 Sprint 3 节奏基线与认领 | 补齐 T-066 的 Lab 5 端到端录屏；跟催 T-052~054 分支建立 | T-063 / T-065 依赖未就绪 | 本目录各文件；review `APPROVED` |

待升级问题（需 PO 或跨团队协调）：

1. **ADR-010 责任人口径冲突**：表 9 第④项（江思达）与 4.5 节 T-064（SM）不一致，需在 Planning 纪要定稿；SM 已按表 6 编号分配权临时收口为「江思达撰写、SM 审核归档」。
2. **看板载体未指定**：待 PO 交付 `docs/backlog/sprint-3-backlog.md`；在此之前以 `docs/lab6/sprint-3-plan.md` 第二节为临时基线。
3. **D+5 里程碑剩余缺口**：通知投影（T-052/T-053）尚无分支，是 11-24「索引表有数据 + 通知表有记录」判据的唯一未覆盖项。

开放 PR 存活时长点检：当前**无开放 PR**（PR #23 已于今日合并关闭），`24h 未评审 PR = 0` 达标。

### 补充：SM 对 T-063 的依赖取证（本人动作记录，非他人陈述）

本机无 Docker（Lab 5 已记录），Testcontainers 无法拉起 `mysql:8.0.39` / `redis:7-alpine`。SM 在 T+0 当日为 FlowIT 增加「外部服务模式」（`-Dco.it.external=true`，接本机 MySQL 8.4.8:3307 与 Redis 5.0.14.1:6379，默认路径与 CI 行为不变），从而把 T-063 从「无法验证」变为「可实跑取证」：

- 单元测试：`Tests run: 91, Failures: 0, Errors: 0, Skipped: 0`；
- 集成测试：3 例中 1 通过、1 失败、1 报错；
- 根因：`NotificationProjector` 的 5 个 `on(...)` 仍抛 `TODO(S3)`，`AnswerSubmittedEvent` / `AnswerAcceptedEvent` / `BadgeAwardedEvent` 在 `outbox_event` 中 `published_at IS NULL`、`attempts=4`；下游表现为 `dis_search_doc.answer_count=0`、`accepted=0`；
- 附带纠正：FlowIT 中「不能给自己的内容投票」断言写 `QA-1015`，与 `docs/ddd/aggregates.md` I6-2 的 `QA-1006` 不符，已按规范修正。

结论与承诺不变：T-063 仍 `@Disabled`，等 T-052 ~ T-057 合并后 24h 内重跑。取证全文见 [evidence/flow-it-run-2026-11-19.md](evidence/flow-it-run-2026-11-19.md)。

**给何欣怡的口径**：通知投影是 T-063 的唯一硬阻塞项，D+5（11-24）判据「通知表有记录」与 T-063 转绿共用同一条链路，建议优先落 `AnswerSubmittedEvent` 与 `AnswerAcceptedEvent` 两个处理器。
