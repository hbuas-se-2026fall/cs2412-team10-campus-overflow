# Lab 6（Sprint 3）证据归档规范与清单

> 维护人：SM（左焱之 / Flagg32）｜对应任务：T-062 / T-063 / T-065
> 目录用途：存放 Sprint 3 的过程与验收证据。业务文档（Backlog、纪要）不放此处，见 `docs/backlog/`。

## 一、命名规范

| 类型 | 命名 | 说明 |
| --- | --- | --- |
| 演示录屏 | `sprint-3-demo-YYYYMMDD.mp4` | ≤5 min，含双会话通知演示；建议 1280×720 以上，画面内 URL 可辨 |
| FlowIT 转绿 | `flowit-verify-YYYYMMDD.png` | `mvn verify -Dit.test=CampusOverflowFlowIT` 的终端输出，需含 `BUILD SUCCESS` 与用例数 |
| CI 证据 | 直接引用运行链接 | `https://github.com/hbuas-se-2026fall/cs2412-team10-campus-overflow/actions/runs/<id>` |
| 归档说明 | 本节表格追加一行 | 每次归档必须登记：录制/截图人、时间、后端 commit、前端 commit |

## 二、待归档清单

| # | 证据 | 对应判据 | 责任人 | 状态 |
| --- | --- | --- | --- | --- |
| 1 | 演示录屏（≤5 min，无 500） | T-065：录屏可回放且无 500 | SM（Flagg32） | 待归档（依赖 T-063 全链路可演示） |
| 2 | FlowIT 转绿终端证据 | T-063：`mvn verify -Dit.test=CampusOverflowFlowIT` 通过 | SM（Flagg32） | 待归档（FlowIT 仍 `@Disabled`，依赖 T-052~T-057） |
| 3 | `git log` 截图 + tag `sprint-3-demo` | 表 9 ①「Sprint 3 增量在 main 可演示」 | SM（Flagg32） | 待归档（冻结后打标） |
| 4 | 回顾纪要 `docs/retro/sprint-3.md` | T-065：卡片汇总 + 2–3 项可执行改进 | SM（Flagg32） | 待产出（Retro 现场记录，不预填） |
| 5 | ADR-010 编号与口径审核结论 | T-064：README 登记可点 | SM（Flagg32） | **已归档**：[../adr-010-review.md](../adr-010-review.md) |

## 三、已可引用的现有证据（无需重复归档）

| 证据 | 位置 | 说明 |
| --- | --- | --- |
| PR #23 评审记录 | `pull/23#pullrequestreview-5967346050` | SM 对 T-049~051 与 ADR-010 的复核意见，state = APPROVED |
| PR #23 合并记录 | commit `c8404b25` | T-049 / T-050 / T-051 / T-064 的落地证据 |
| CI 运行 | run `44215165936` | 前端 success、后端 success；此前两次 failure 为环境性偶发，已 rerun 排除 |
| Lab 5 证据目录 | `docs/lab5/evidence/` | 提交物④ 端到端录屏为结转任务 T-066，见该目录 README 第三节 |

## 四、归档纪律

1. 证据只登记**本仓可复算**的事实：commit SHA、PR 号、CI 运行号、命令与实际输出。禁止使用「本地跑过、结果忘了」这类无法核查的表述。
2. 截图与录屏不得裁剪掉能证明结论的关键行（如 `BUILD SUCCESS`、用例数、时间戳）。
3. 未完成的证据保留「待归档」字样并写明阻塞原因与解锁条件，不提前打勾。
