# Lab 1 首次 PR 合并记录台账

> 数据来源：GitHub API `list_pull_requests`（`state=all`），拉取时间 2026-11-19
> 仓库：`hbuas-se-2026fall/cs2412-team10-campus-overflow`
> 用途：替代 PDF Lab 1 提交物 4「每位成员的首次 PR 合并记录截图」的**可核查文字台账**

---

## 一、口径说明（重要）

本台账记录的是仓库**真实的 PR 合并历史**：共 **14 个 PR，13 个已合并**，5 位团队成员均有已合并 PR，可证明「branch → commit → PR → merge」协作流程确实跑通。

但它**不等于** Lab 1 Step 3 要求的完整证据：PDF 要求每位成员从 `feature/<姓名拼音>-hello` 分支提交 `docs/team/hello-<name>.md`、经一名队友 review + LGTM、由 SM 合并，并留存「首次 PR 合并记录截图」。现有 PR 中：

- #1–#6 集中于 2026-11-05 由 PO（luck970）创建，多为文档补齐与 CI 修复，并非各成员本人的 hello 分支 PR；
- 其余 PR 为 Lab 3 / Lab 4 / Lab 5 的功能与文档交付（2026-11-10 起）；
- **评审人 LGTM 记录与截图未归档**。

因此：本台账可作为「团队协作流程已运行」的证据，**但 PDF 要求的截图仍需团队在 PR 页面逐人截取归档**，见 [证据补齐指引](../lab-audit/证据补齐指引.md) 第 2 节。

---

## 二、已合并 PR（13 个）

| PR | 标题 | 发起人 | 创建时间 | 合并时间 | 链接 |
| --- | --- | --- | --- | --- | --- |
| #1 | docs: 补齐 Lab1/2 材料并统一 Sprint1 分工 | luck970（PO 唐志烽） | 2026-11-05 | 2026-11-12 | [PR #1](https://github.com/hbuas-se-2026fall/cs2412-team10-campus-overflow/pull/1) |
| #2 | Feature/lab4 sm acceptance | luck970 | 2026-11-05 | 2026-11-05 | [PR #2](https://github.com/hbuas-se-2026fall/cs2412-team10-campus-overflow/pull/2) |
| #3 | docs(lab3): 江思达校对问答域 4 聚合，补不变式到 QuestionTest 精确映射 | luck970 | 2026-11-05 | 2026-11-05 | [PR #3](https://github.com/hbuas-se-2026fall/cs2412-team10-campus-overflow/pull/3) |
| #4 | chore(docs): 补齐 Lab 1 团队组建缺失的 8 份文档 | luck970 | 2026-11-05 | 2026-11-05 | [PR #4](https://github.com/hbuas-se-2026fall/cs2412-team10-campus-overflow/pull/4) |
| #5 | ci: 把工作流挪到仓库根并适配嵌套脚手架路径 | luck970 | 2026-11-05 | 2026-11-05 | [PR #5](https://github.com/hbuas-se-2026fall/cs2412-team10-campus-overflow/pull/5) |
| #6 | Fix/ci first run | luck970 | 2026-11-05 | 2026-11-05 | [PR #6](https://github.com/hbuas-se-2026fall/cs2412-team10-campus-overflow/pull/6) |
| #7 | Lab3: 声誉域 DDD 落地（25 个 TODO 实现 + 3 份清单 + 覆盖率门禁 0.70） | Ke-1226（柯博） | 2026-11-10 | 2026-11-10 | [PR #7](https://github.com/hbuas-se-2026fall/cs2412-team10-campus-overflow/pull/7) |
| #8 | feat(lab4): JSON 结构化日志、TraceId 链路与不变量测试补齐 | Ke-1226 | 2026-11-10 | 2026-11-10 | [PR #8](https://github.com/hbuas-se-2026fall/cs2412-team10-campus-overflow/pull/8) |
| #17 | docs(lab3): add glossary v1.0 and identity aggregates - xinyi | hehe309（何欣怡） | 2026-11-11 | 2026-11-11 | [PR #17](https://github.com/hbuas-se-2026fall/cs2412-team10-campus-overflow/pull/17) |
| #18 | feat(前端): Lab 5 前端工程化门禁与 SM 交付文档 | Flagg32（SM 左焱之） | 2026-11-11 | 2026-11-12 | [PR #18](https://github.com/hbuas-se-2026fall/cs2412-team10-campus-overflow/pull/18) |
| #19 | docs(backlog): 同步 Lab 4 任务状态为 Done 并更新 Lab 3 前置依赖 | Ke-1226 | 2026-11-12 | 2026-11-12 | [PR #19](https://github.com/hbuas-se-2026fall/cs2412-team10-campus-overflow/pull/19) |
| #20 | feat(frontend): Step3 OpenAPI 契约与 gen:api、axios 拦截器、QueryClient 与 ErrorBoundary | houhai6（江思达） | 2026-11-12 | 2026-11-12 | [PR #20](https://github.com/hbuas-se-2026fall/cs2412-team10-campus-overflow/pull/20) |
| #21 | docs(lab4): 勘误不变式覆盖口径并对齐验收基线 | Ke-1226 | 2026-11-12 | 2026-11-12 | [PR #21](https://github.com/hbuas-se-2026fall/cs2412-team10-campus-overflow/pull/21) |
| #22 | docs(lab5): 补充提交物证据目录说明与 T-048 端到端录屏规范 | Flagg32 | 2026-11-12 | 2026-11-18 | [PR #22](https://github.com/hbuas-se-2026fall/cs2412-team10-campus-overflow/pull/22) |

## 三、按成员汇总

| 成员 | 角色 | 已合并 PR | 首个 PR |
| --- | --- | --- | --- |
| 唐志烽 / luck970 | PO | 6（#1–#6） | #1 |
| 柯博 / Ke-1226 | Developer | 4（#7、#8、#19、#21） | #7 |
| 何欣怡 / hehe309 | Developer | 1（#17） | #17 |
| 左焱之 / Flagg32 | SM | 2（#18、#22） | #18 |
| 江思达 / houhai6 | Developer | 1（#20） | #20 |

> 另有 1 个未合并 PR：[#23 feat(discovery): 搜索索引投影、搜索查询与相关问题推荐](https://github.com/hbuas-se-2026fall/cs2412-team10-campus-overflow/pull/23)（houhai6，2026-11-18 创建，仍 open，属 Lab 6 / Sprint 3 范围，不计入 Lab 1–5）。

---

**维护人**：PO（唐志烽 / luck970）　|　**生成**：2026-11-19　|　**关联**：[Lab 1–5 核对与补齐报告](../lab-audit/Lab1-5-核对与补齐报告.md)
