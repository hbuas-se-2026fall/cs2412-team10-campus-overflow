# Lab 6 Sprint3 缺陷台账

> Sprint：Sprint 3　|　实验：Lab 6　|　维护人：柯博（Ke-1226）
> 模块范围：`co-qa/**`、`co-reputation/**`　|　最近更新：2026-11-26
> 来源：T-058 优先排查清单三项（搜索投影重试、采纳后声誉与通知事务边界、删除后索引与通知残留）的只读调研，以及 T-055 逐条对照过程中发现的不一致。

## 一、状态说明

| 状态 | 含义 |
| --- | --- |
| 已修复 | 修复代码与回归测试已在本 PR 内，`mvn test` 通过 |
| 待排期 | 已登记根因与复现路径，尚未修复，需在后续 Sprint 或随对应模块负责人排期 |
| 口径留档 | 非缺陷，但验收时易误判，记录判定口径 |

## 二、已修复（本 PR）

### D-QA-01　标签使用计数在编辑时重复累加、删除时不回收

| 项 | 内容 |
| --- | --- |
| 严重度 | 中 |
| 位置 | `co-qa/co-qa-application/.../command/QuestionCommandService.java` 的 `edit`、`delete` 方法 |
| 复现步骤 | 1) 以标签 `算法`、`图论` 发布问题，`qa_tag.question_count` 各 +1；2) 编辑问题，标签改为 `算法`、`图论`、`动态规划`；3) 观察 `question_count`：预期仅 `动态规划` +1，实际三个标签都被再加一次；4) 删除该问题，预期三个标签各 -1，实际三个标签保持不变 |
| 实际结果 | `question_count` 随编辑次数线性虚高，删除后永不回落，标签排序与「热门标签」失去参考价值 |
| 根因 | `edit` 对提交的完整标签集合无差别调用 `adjustUsage(tags, +1)`，未与编辑前的集合做差集；`delete` 只在软删问题时回收回答计数，完全没有对标签做减量 |
| 修复 | 新增私有方法 `adjustTagUsage(Set<String> before, List<String> after)`，按差集处理：仅新增的标签 +1、仅被摘掉的标签 -1、未变化的标签不产生变更；`edit` 与 `delete` 均改为调用该方法 |
| 修复 PR | 本 PR（`fix(qa,reputation): 修正标签计数漂移与已删回答认证校验`） |
| 回归测试 | `TagUsageAndEndorseRegressionTest`：`editOnlyCountsNewlyAddedTags`、`editReclaimsDroppedTags`、`editWithSameTagsTouchesNoCounter`、`deletingQuestionReclaimsTagUsage` |

### D-QA-02　已被软删除的回答仍可被教师认证

| 项 | 内容 |
| --- | --- |
| 严重度 | 中 |
| 位置 | `co-qa/co-qa-domain/.../domain/Answer.java` 的 `endorse(Actor, boolean)` |
| 复现步骤 | 1) 提交一条回答；2) 由作者或课程管理者软删除该回答；3) 课程管理者调用教师认证接口 `POST /api/v1/answers/{id}/endorse`；4) 预期返回业务错误，实际认证成功并发出索引与声誉事件 |
| 实际结果 | 已下线的回答获得认证标记，同时触发 `AnswerEndorsedEvent`，使已删除内容进入搜索与声誉链路，与「软删除后内容不可再参与交互」的规则（`QA-2004` 已在采纳、投票、评论路径拦截）不一致 |
| 根因 | `endorse` 只校验操作者是否为课程管理者（`identity.canManageCourse`），未校验回答自身是否已被软删除；同类校验在同聚合的 `accept` 路径已有 `ensureNotDeleted()`，认证路径遗漏 |
| 修复 | `endorse` 入口补 `ensureNotDeleted()`，已删除回答抛出 `DomainException("QA-2004")` |
| 修复 PR | 本 PR（同上） |
| 回归测试 | `TagUsageAndEndorseRegressionTest`：`deletedAnswerCannotBeEndorsed`（断言错误码 `QA-2004` 且未写入认证人）、`activeAnswerCanStillBeEndorsed` |

## 三、已登记待排期

| 编号 | 缺陷 | 严重度 | 位置 | 根因摘要 | 处置建议 |
| --- | --- | --- | --- | --- | --- |
| D-QA-03 | 父文档缺失时搜索增量投影静默丢失 | 中 | `JdbcSearchIndex.adjustAnswerCount`；`SearchIndexProjector.on(AnswerSubmittedEvent)` | `UPDATE` 影响 0 行时返回值被丢弃，事件仍被 `processed_event` 记为已处理；后续 `QuestionPostedEvent` 的 `INSERT ... ON DUPLICATE KEY UPDATE` 不修正 `answer_count`，该问题回答数永久为 0 且无告警 | 投影失败应抛异常交 relay 重试，或在插入时补算增量 |
| D-QA-04 | 死信后无搜索侧重建 / 对账 / 重放入口 | 中 | `OutboxRelay.claim`；`ProcessedEventStore` | `attempts >= maxAttempts` 的行被永久排除，`processed_event` 无按 consumer 清理接口，搜索侧没有类似声誉 `ReconciliationService` 的补偿通道 | 提供按事件 ID 重放与按 consumer 重置的运维入口 |
| D-QA-05 | 毒事件阻塞整批投递 | 中 | `OutboxRelay.relay`、`release` | 遇首个失败即 `break` 并释放其后行，单个持续失败事件可把后续投影与通知阻塞到退避封顶（累计约 15 分钟） | 属一致性优先的取舍，需在 ADR-010 中显式留档 |
| D-QA-06 | 死信导致「声誉成功 / 通知缺失」半边完成且无检测 | 高 | `ReputationEventHandlers`、`NotificationProjector` | 事件进死信后被永久跳过，各监听器事务相互独立，通知与搜索侧无对账服务 | 需补死信告警与跨上下文对账任务 |
| D-QA-07 | 排行榜 Redis 写与去重记录非原子 | 中 | `LeaderboardService.on(ReputationChangedEvent)` | `processed_event` 插入与 Redis `ZINCRBY` 不在同一事务，MySQL 提交失败时去重行回滚而 Redis 已累加，重投后分值虚高 | 改为先写 Redis 记录待重算标记，或由定时任务整体重算 |
| D-QA-08 | 删除问题后通知文案与链接永久残留 | 高（可演示） | `NotificationProjector`、`NotificationService.list` | `ContentDeletedEvent` 仅有 `SearchIndexProjector` 一个消费者，通知侧未订阅；通知在创建时固化 `link=/questions/{id}`，问题删除后点击返回 `QA-1000` | 通知侧订阅删除事件置失效态，或查询时过滤 |
| D-QA-09 | 管理者删除有回答的问题后回答成孤儿 | 低 | `Question.softDelete` | 不级联软删 `qa_answer`，回答不可从详情页到达但仍存于表内 | 软删语义下可接受，需在 ADR-006 明确口径 |
| D-QA-10 | 记账顺序与 ADR-004 不变式相反 | 低 | `ReputationLedgerService.record` | 实现为「先改账户余额后追加流水」，文档要求先写流水再改账户；同事务下不产生脏数据 | 随下次改动调整顺序，保持与文档一致 |

## 四、口径留档

| 编号 | 事项 | 结论 |
| --- | --- | --- |
| N-QA-01 | 自问自答被采纳时声誉不加分 | `ReputationRulebook.acceptedDelta` 对「回答者＝提问者」返回 0，`ACCEPTED=15` 仅对他人采纳生效。T-055 / T-057 验收造数必须由第二人回答后再采纳，否则会把 0 增量误判为 +15 未生效 |
| N-QA-02 | 采纳后声誉、索引、通知不在同一事务 | 按 ADR-001 / ADR-002 / ADR-009，本地事务 + 事务性发件箱 + 至少一次投递是既定架构，验收时须轮询等待，不能在同一次 HTTP 响应后立即断言三者一致 |

## 五、验证方式

```
cd backend
mvn -o -B test -pl co-qa/co-qa-domain,co-qa/co-qa-application -am
```

本 PR 提交时结果：`co-qa-domain` 22 个用例、`co-qa-application` 22 个用例全部通过（含新增 `TagUsageAndEndorseRegressionTest` 6 个用例）。集成类用例（D-QA-03 至 D-QA-08 的复现）依赖本机 MySQL 与 Redis，本次未启动，未纳入本 PR 验证范围。
