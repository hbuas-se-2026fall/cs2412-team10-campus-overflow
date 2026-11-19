# Sprint 3 计划与节奏（SM 收口版）

> 维护人：SM（左焱之 / Flagg32）｜任务：T-062 分支 / PR 节奏与看板同步、每日站会主持、main 合并
> 依据：《Lab6-Sprint3-分工方案 v1.1》表 7（三周节奏）、表 8（课内 120 分钟）、四节工程纪律
> 基线：main @ `c8404b25`（PR #23 合并后的快照）｜快照时间：2026-11-19

---

## 一、关键路径（每日站会唯一盯的指标）

分工方案 4.1 节排期约束原文：**T-049 必须先于 T-050 完成，否则查询无数据可验**，是全组唯一的关键路径。

| 节点 | 判据 | 当前状态 |
| --- | --- | --- |
| 写入侧：发帖 ≤2s 索引表出现对应行（T-049） | `dis_search_doc` 有数据，新增 / 编辑 / 删除三类事件有测试 | **已达成**（PR #23，`SearchIndexProjector` 9 类事件 + `ProcessedEventStore` 幂等去重） |
| 查询侧：四种组合返 200（T-050） | 关键词 / 标签 / 排序 / 未答 | **已达成**（`DiscoveryQueryService.search`、`SearchController` 非法排序回退默认） |
| 推荐侧：同标签优先、空结果返 `[]`（T-051） | 详情页右侧相关列表 | **已达成**（`RelatedQuestionPolicy` 标签 Jaccard） |
| 通知侧（T-052 ~ T-054） | 被采纳 / 被评论 / 被 @ 三类各一条通知 | **未开始**（仓库无对应分支与 PR） |

> 结论：关键路径的「搜索索引投影 + 查询 + 推荐」已于 T+0 前置打通（PR #23 在 Sprint 3 开始前提交并合并），D+5（11-24）里程碑的搜索侧判据已满足；通知投影仍是空白，是 D+5 里程碑剩余的唯一缺口。

---

## 二、任务状态快照（2026-11-19 实查 GitHub）

状态取值：Done = 已合并进 main；In Progress = 有分支或开放 PR；未开始 = 仓库无痕迹。

| 任务号 | 任务摘要 | 负责人 | 依赖 | 状态 | 证据 |
| --- | --- | --- | --- | --- | --- |
| T-049 | 搜索索引投影（SearchIndexProjector） | 江思达 | 无 | Done | PR #23 → merge `c8404b25` |
| T-050 | 搜索查询（SearchQuery / DiscoveryQueryService.search / SearchController） | 江思达 | T-049 | Done | 同上 |
| T-051 | 相关问题推荐（RelatedQuestionPolicy） | 江思达 | T-050 | Done | 同上 |
| T-052 | MentionParser.extract 与 Notification | 何欣怡 | 无 | 未开始 | 无分支、无 PR |
| T-053 | 通知投影与查询（NotificationProjector / NotificationService） | 何欣怡 | T-052 | 未开始 | 无分支、无 PR |
| T-054 | 通知端到端验证与前端联调（双会话） | 何欣怡 | T-053 | 未开始 | 无分支、无 PR |
| T-055 | 投票 / 评论 / 标签与 PO Feature 逐条对照 | 柯博 | T-059 | 未开始 | 无分支、无 PR |
| T-056 | 采纳与教师认证权限矩阵 | 柯博 | T-059 | 未开始 | 无分支、无 PR |
| T-057 | 声誉事件链路验收（采纳 +15、账本对账、排行榜） | 柯博 | T-055、T-056 | 未开始 | 无分支、无 PR |
| T-058 | 跨模块 BUG 修复 1–2 个 + 缺陷台账 | 柯博 | 随时 | 未开始 | 无缺陷台账条目 |
| T-059 | 补 4 条用户故事与 Gherkin（US-016 ~ US-019） | 唐志烽 | 无 | 未开始 | `docs/backlog/stories/` 现有至 US-015 |
| T-060 | 声誉计分规则三方对齐（实现 / 文档 / 测试） | 唐志烽 | T-059 | 未开始 | 无对应文档 |
| T-061 | Sprint 3 Backlog、Planning 纪要、Review 脚本 | 唐志烽 | 全部 | 未开始 | `docs/backlog/` 现有至 sprint-2 |
| T-062 | 分支 / PR 节奏与看板同步、站会、main 合并 | SM（Flagg32） | 无 | In Progress | 本文件 + `standup-log.md` |
| T-063 | FlowIT 解除 @Disabled 并转绿 | SM（Flagg32） | T-049 ~ T-057 | Blocked | 依赖 T-052 ~ T-058 未完成 |
| T-064 | ADR-010 关键设计 | SM（Flagg32，编号与审核） | T-050 | Done（审核归档） | `campus-overflow-student/docs/adr/010-搜索索引投影一致性.md` + `adr-010-review.md` |
| T-065 | 演示录屏（≤5 min）+ `docs/retro/sprint-3.md` + 主持 Retro | SM（Flagg32） | T-063 | Blocked | 依赖 T-063 |
| T-066 | 补录 Lab 5 端到端录屏（结转） | SM（Flagg32） | 搜索可用 | In Progress | 见 `docs/lab5/evidence/README.md` 第三节 |

### 2.1 需要 PO 定稿的两处口径冲突（SM 已识别，不擅自改分工）

1. **ADR-010 归属**：表 9 第④项写「江思达」，4.5 节 T-064 与九节速查表写「SM」。SM 按表 6「ADR 编号由 SM 统一分配」处理为：**实现与撰写随 T-049~051 由江思达落地，SM 负责编号分配与口径审核**，不重复产出第二份 ADR。若 PO 要求 SM 另写一份，需在 Planning 纪要中明确，避免重复交付。
2. **看板载体**：分工方案未指定看板工具（GitHub Projects / Issues / Markdown 表）。当前以 `docs/backlog/sprint-3-backlog.md`（T-061）为唯一看板来源，SM 每日以其为基准回填本文件第二节。PO 交付该文件前，看板同步以本文件为临时基线。

---

## 三、里程碑（照录分工方案表 7）

| 节点 | 建议日期 | 里程碑 | 出口判据 |
| --- | --- | --- | --- |
| T+0 | 11-19 | Sprint 3 Planning | Backlog 锁定；任务认领到人；分支建立 |
| D+5 | 11-24 | 关键路径打通：搜索索引投影 + 通知投影可用 | 索引表有数据；通知表有记录 |
| D+9 | 11-28 | 功能面收口：搜索 / 相关 / 通知全部可演示 | 三项均无 500；PO 的 4 条 Feature 入库并跑通 |
| D+12 | 12-01 | 集成转绿：FlowIT 解禁 | `mvn verify` 绿；ADR-010 提交 |
| D+14 | 12-03 | Lab 6 课内（60 + 35 + 25 分钟） | 演示完成、Retro 纪要产出、录屏归档 |

---

## 四、分支与 PR 节奏（SM 执行口径）

1. **分支命名**：`feature/lab6-<主题>-<姓名拼音>`（如 `feature/lab6-notify-hexinyi`）；文档类 `docs/lab6-<主题>`；修复类 `fix/<主题>`。分支一律从最新 `origin/main` 拉出。
2. **PR 纪律**（章程 3.4 与 DoD）：先写测试 → PR 获至少 1 个 LGTM → **由 SM 合并**；标题遵循 `<type>(<范围>): <说明>`，正文写清任务号与验证方式。
3. **评审时限**：PR 提出后 **24 小时内**必须有人评审（Sprint 末「24h 未评审 PR 为 0」是 T-062 的完成判据）。SM 每日站会点检开放 PR 的存活时长。
4. **合并方式**：保留 merge commit（`Merge pull request #N from ...`），与 PR #18 / #20 / #21 / #22 / #23 历史一致；合并后删除已合并的临时分支属可选动作，不强制。
5. **代码冻结**：课内第 50 分钟（表 8）。冻结后未合并内容不进演示，改作「下 Sprint 输入」说明，禁止现场改 main。

### 4.1 已执行动作（可核查）

| 时间 | 动作 | 结果 |
| --- | --- | --- |
| 2026-11-19 | 复核 PR #23（江思达 T-049~051）代码与 ADR-010 编号口径，提交 SM 评审意见并 LGTM | review `APPROVED`（#pullrequestreview-5967346050） |
| 2026-11-19 | 合并 PR #23 入 main | `c8404b25`，9 个文件、+558/−33 |
| 2026-11-19 | 排除 CI 偶发失败：对 run `44215165936` 触发 rerun | 前端 success、后端 success（此前两次 failure 为环境性偶发） |

---

## 五、CI 门禁现状（`.github/workflows/ci.yml`）

| Job | 阶段 | 本地复算（2026-11-19） | CI |
| --- | --- | --- | --- |
| backend | 编译 → 单元测试 → 架构约束（ArchUnit FF-1~FF-6） → 覆盖率门禁 → 集成测试 | `mvn -B clean compile`、`test`、`verify -DskipITs`、`verify -Dsurefire.skip=true` 均 BUILD SUCCESS | success |
| frontend | `--frozen-lockfile` 安装 → ESLint → Prettier → tsc → build | 未改动前端，PR #23 未触及 `frontend/` | success |
| images | 容器镜像（仅 push 事件） | 本机无 Docker，未复算 | pull_request 事件跳过 |

> 集成测试阶段目前不实际执行用例：`CampusOverflowFlowIT` 处于 `@Disabled`（团队决策 D1 / T-038），T-063 恢复。CI 保留该阶段，恢复时无需改流水线。

---

## 六、风险与应对（摘取与 SM 相关的三条）

| 风险 | 触发信号 | 应对（SM 动作） |
| --- | --- | --- |
| 第 50 分钟仍有未合并 PR，演示不完整 | 冻结时刻开放 PR 非 0 | 严格执行冻结；未合并内容改作「下 Sprint 输入」说明，不现场救火 |
| Testcontainers 无法拉起，S3-6 无法取证 | `mvn verify` 卡在容器启动 | 降级为手动 E2E + 录屏；FlowIT 保留 `@Disabled` 并写明新恢复条件（沿用 T-038 做法） |
| `api.ts` / `openapi.yaml` 三人同时改 | 同一文件多 PR 冲突 | SM 收口：改前群内报备 → 统一 `gen:api` → 一次提交 |

---

## 七、SM 任务台账（T-062 ~ T-066）

| 任务 | 完成判据（原文） | 现状 | 下一步 / 阻塞 |
| --- | --- | --- | --- |
| T-062 | 每人每日更新卡片；Sprint 末 24h 未评审 PR 为 0 | 进行中：节奏规则与本快照已入库；PR #23 于提出后 <24h 完成评审与合并 | 每日按第二节回填；对 T-052 ~ T-061 无分支的成员在站会点检 |
| T-063 | `mvn verify -Dit.test=CampusOverflowFlowIT` 通过，覆盖「注册→提问→回答→采纳→声誉 +15→搜索命中→收到通知」 | 阻塞：依赖 T-052 ~ T-057，通知与声誉链路尚未实现 | 待 T-052/T-053 与 T-055~T-057 合并后解除 `@Disabled`；当前不放宽判据 |
| T-064 | 含背景 / 决策 / 后果 / 被否决方案；ADR README 登记可点 | 达成：ADR-010 已合并且 README 索引可点；SM 已出编号与口径审核结论 | 若 PO 要求 SM 独立再写一份，按 Planning 纪要调整 |
| T-065 | 录屏可回放且无 500；纪要含卡片汇总与 2–3 项可执行改进 | 阻塞：依赖 T-063 与可演示的完整链路 | Retro 纪要与卡片汇总必须来自真实回顾会，不预填 |
| T-066 | 录屏可回放，三画面齐全（提问 → 列表 → 详情） | 进行中：前置条件（后端 MySQL/Redis、前端 dev）已在本地复算可用 | 录制 ≤30s 三画面并归档至 `docs/lab5/evidence/`，README 的「待归档」改实链 |
