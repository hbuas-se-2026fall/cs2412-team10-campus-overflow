# T-063 取证记录：CampusOverflowFlowIT 实跑（2026-11-19）

> 任务：T-063（解除 `@Disabled` 并转绿）｜责任人：SM（左焱之 / Flagg32）
> 结论：**未转绿，且不能在当前依赖状态下转绿**。实跑把阻塞点从「S3 范围未开工」收敛到「T-052 ~ T-057 通知投递未实现」，并附带纠正了一处与错误码规范不符的断言。

## 一、结论先说

| # | 事实 | 证据 |
| --- | --- | --- |
| 1 | FlowIT 可在无 Docker 的本机实跑，用例集真实执行 | 本节第三节 failsafe 报告：`Tests run: 3, Failures: 1, Errors: 1, Skipped: 0` |
| 2 | 失败根因是 `NotificationProjector` 5 个 `on(...)` 仍为 `TODO(S3)` 并抛 `UnsupportedOperationException` | 本节第四节 `outbox_event` 表：3 类事件 `attempts=4`、`published_at IS NULL`、`last_error` 直接打印该 TODO 文案 |
| 3 | 搜索投影拿不到 `answer_count` / `accepted`，是上面异常的下游后果，不是 T-049~T-051 的缺陷 | 第四节 `dis_search_doc`：两条文档 `answer_count=0`、`accepted=0`；而 `AnswerSubmitted`/`AnswerAccepted` 均未投递成功 |
| 4 | 主链路用例以外的「不能给自己的内容投票」失败是**断言写错**，实现符合规范 | `docs/ddd/aggregates.md` 第 154–155 行、`docs/lab4/不变式-测试覆盖对照表.md` 第 109 行：I6-2 的错误码是 `QA-1006`；原断言写 `QA-1015` |
| 5 | 判据命令在多模块 reactor 下需补参数才有效 | 不加 `-Dfailsafe.failIfNoSpecifiedTests=false` 时，父 POM 模块无匹配测试即 `BUILD FAILURE`（`No tests matching pattern "CampusOverflowFlowIT" were executed!`） |

## 二、运行环境（可复算）

| 项 | 值 |
| --- | --- |
| 操作系统 | Windows 11（Build 26100） |
| JDK | Eclipse Temurin 21.0.10（`JAVA_HOME` 指向该 JDK） |
| Maven | 3.9.16（`C:\Program Files (x86)\apache-maven-3.9.16\bin\mvn.cmd`） |
| Docker | **无**（`docker` 命令不存在，`\\.\pipe\docker_engine` 不存在），Testcontainers 无法拉起容器 |
| MySQL | 8.4.8 临时实例，端口 `3307`，`ngram_token_size=2`，字符集 `utf8mb4` / `utf8mb4_0900_ai_ci`，库 `campus_overflow_it` |
| Redis | 5.0.14.1（`tporadowski/redis` 免安装包），端口 `6379`，`--save "" --appendonly no` |
| 基线 commit | `82c17ed`（PR #24 合并后） |

> 本机无 Docker 是既定事实（Lab 5 已记录），因此本次没有放宽任何断言去迁就环境，而是给测试类增加了「外部服务模式」。

## 三、执行命令与实际输出

外部服务模式（本次取证所用）：

```
mvn -B -ntp verify \
  -Dit.test=CampusOverflowFlowIT \
  -Dfailsafe.failIfNoSpecifiedTests=false \
  -Dco.it.external=true \
  -Dco.it.jdbc-url=jdbc:mysql://127.0.0.1:3307/campus_overflow_it \
  -Dco.it.db-user=root -Dco.it.db-password= \
  -Dco.it.redis-host=127.0.0.1 -Dco.it.redis-port=6379
```

- surefire（单元测试）：`Tests run: 91, Failures: 0, Errors: 0, Skipped: 0`（18 个报告文件，20 个模块）
- failsafe（集成测试）：`Tests run: 3, Failures: 1, Errors: 1, Skipped: 0, Time elapsed: 35.12 s`
- Spring 上下文启动：`Started CampusOverflowFlowIT in 9.879 seconds`；Flyway 5 个上下文迁移全部应用至 `v202609010400`

失败明细：

| 用例 | 结果 | 关键输出 |
| --- | --- | --- |
| 提问到采纳的完整链路会异步结算声誉并更新搜索与通知 | ERROR | `ConditionTimeoutException: JSON path "$.items[0].accepted" expected:<true> but was:<false> within 20 seconds`（`CampusOverflowFlowIT.java:157`） |
| 不能给自己的内容投票 | FAILURE | `JSON path "$.code" expected:<QA-1015> but was:<QA-1006>` |
| 未登录不能提问且缺少CSRF令牌会被拒绝 | PASS | — |

## 四、根因证据（库内状态，取实跑后快照）

`outbox_event`（节选，`last_error` 原文）：

| id | event_type | attempts | published_at | last_error |
| --- | --- | --- | --- | --- |
| 2 | `qa.api.event.QuestionPostedEvent` | 0 | 已发布 | — |
| 6 | `qa.api.event.AnswerSubmittedEvent` | 4 | NULL | `TODO(S3)：NotificationProjector.on 尚未实现` |
| 7 | `qa.api.event.AnswerAcceptedEvent` | 4 | NULL | `TODO(S3)：NotificationProjector.on 尚未实现` |
| 8 | `reputation.api.event.BadgeAwardedEvent` | 4 | NULL | `TODO(S3)：NotificationProjector.on 尚未实现` |
| 10 | `reputation.api.event.BadgeAwardedEvent` | 4 | NULL | `TODO(S3)：NotificationProjector.on 尚未实现` |

`processed_event`（消费者维度）：

| consumer | 已处理事件数 |
| --- | --- |
| `discovery.search-index` | 2（仅两条 `QuestionPosted`） |
| `leaderboard` | 1 |

`dis_search_doc`：

| question_id | answer_count | accepted |
| --- | --- | --- |
| 1 | 0 | 0 |
| 2 | 0 | 0 |

代码位置：`co-discovery/co-discovery-application/.../NotificationProjector.java` 的
`on(AnswerSubmittedEvent)` / `on(AnswerAcceptedEvent)` / `on(AnswerEndorsedEvent)` / `on(CommentPostedEvent)` / `on(MentionedEvent)`
全部为 `throw new UnsupportedOperationException("TODO(S3)：…尚未实现")`。
发件箱中继在同一批事件上先执行该监听器，异常使后续监听器不生效并触发重试，`max-attempts=10`，本次运行到 `attempts=4` 时用例已超时结束。

## 五、本次改动与边界

| 改动 | 文件 | 说明 |
| --- | --- | --- |
| 增加「外部服务模式」 | `backend/co-bootstrap/src/test/java/com/campusoverflow/CampusOverflowFlowIT.java` | 新增 `-Dco.it.external=true` 分支：跳过 Testcontainers，改用 `-Dco.it.*` 指定的 MySQL/Redis；默认路径（CI 与有 Docker 的本机）行为不变，仍拉 `mysql:8.0.39` + `redis:7-alpine` |
| 修正错误码断言 | 同上 | `QA-1015` → `QA-1006`，依据 I6-2 规范 |
| 保留 `@Disabled` 并重写理由 | 同上 | 新理由写明依赖 T-052 ~ T-057 与 `last_error` 现象，恢复条件三项；**不提交红的集成测试到 main**，CI 维持绿色 |

边界说明：`NotificationProjector` 与声誉链路属于 T-052 ~ T-057，责任人为对应模块 owner，SM 不在本次改动中代为实现；本记录只提供可复算的阻塞证据与解锁条件。

## 六、解锁条件与下一步

1. T-052 ~ T-057 合并后，SM 在 24h 内按本文件第三节命令重跑，三个用例全绿；
2. 删除 `@Disabled`，把 `README.md` 的构建口径由 `mvn clean install -DskipITs` 改回含集成测试；
3. 归档终端截图 `flowit-verify-YYYYMMDD.png`（含 `BUILD SUCCESS` 与用例数），本文件作为其文字版附注。

建议同步给 PO 的表单口径修订：表 4.5 中 T-063 的判据命令应写为
`mvn verify -Dit.test=CampusOverflowFlowIT -Dfailsafe.failIfNoSpecifiedTests=false`，
否则在多模块 reactor 下父 POM 模块会因「无匹配测试」直接失败，与实现质量无关。
