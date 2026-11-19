# Lab 1 – Lab 5 核对与补齐报告

> **核对对象**：`hbuas-se-2026fall/cs2412-team10-campus-overflow`，分支 `main`，基线 commit `010487c`（2026-11-18，`git fetch origin` 后本地与远程一致）
> **核对依据**：`CampusOverflow实验部分.pdf`（15 页，第五章实验教学详案，Lab 1 第 48–49 页至 Lab 5 第 55–56 页）
> **核对方法**：逐条比对 PDF「提交物」与「实验步骤」→ 拉取远程文件树 → 读取本地克隆源码实跑验证
> **核对日期**：2026-11-19　|　**执行**：Windows-mcp + GitHub 只读 API + 本地构建

---

## 一、结论摘要

**该仓库尚未完成 Lab 1 – Lab 5 的全部实验。** 五个 Lab 中仅 **Lab 3（领域驱动设计事件风暴）四项提交物齐全**；Lab 1、Lab 2、Lab 4、Lab 5 的**代码与文档类缺口已在本次补齐**，剩余缺口全部属于**真实性证据**（截图、访谈实录、录屏、本人签名），必须由团队本人现场产生，不能由工具代造。

按性质划分：

| 类别 | 状态 | 说明 |
| --- | --- | --- |
| 文档 / 索引类 | 已补齐 | ADR 决策记录、README、PR 台账、验收核对文档 |
| 代码类（Lab 5） | 已补齐并实跑验证 | 依赖、`@/` 别名、TanStack Query、zustand、AppShell、路由对齐 |
| 真实性证据类 | **仍缺，须本人完成** | 看板截图、访谈实录、PR 截图、e2e 录屏、角色承诺书签字 |
| 实跑证据类 | 部分缺 | Postman 运行证据、空库启动实跑（需 MySQL + Redis 环境） |

---

## 二、总览

判定：✅ 完成　⚠️ 部分完成（产物在，缺证据或待确认）　❌ 缺失

| Lab | PDF 提交物数 | ✅ | ⚠️ | ❌ | 结论 |
| --- | --- | --- | --- | --- | --- |
| Lab 1 团队组建与协作工具链 | 4 | 1 | 1 | 2 | 未完全完成 |
| Lab 2 需求建模工作坊 | 4 | 3 | 1 | 0（另有前置访谈 0/3 ❌） | 未完全完成 |
| Lab 3 领域驱动设计事件风暴 | 4 | 4 | 0 | 0 | **完成** |
| Lab 4 后端工程骨架与领域代码 | 4 | 1（补齐后 2） | 3（补齐后 2） | 0 | 未完全完成 |
| Lab 5 前端工程骨架与对接 | 4 | 3 | 1 | 0 | 未完全完成 |

---

## 三、逐条明细

### Lab 1（PDF 第 48–49 页）

| # | PDF 提交物 | 仓库实际 | 判定 |
| --- | --- | --- | --- |
| 1 | 团队代码仓库 URL（分支保护已配置） | 仓库存在；`main` 分支保护规则属 GitHub 设置，无法由文件内容验证，README 原标注「待确认」 | ⚠️ |
| 2 | 团队看板截图（初始 Backlog ≥5 条） | `docs/backlog/sprint-1-board.png` 不存在 | ❌ |
| 3 | 《团队章程 v1.0》Markdown | `docs/team/charter-v1.0.md`（5.4 KB，含价值观 / 沟通约定 / 开发规约 / DoD） | ✅ |
| 4 | 每位成员首次 PR 合并记录截图 | 无截图；仓库实际有 14 个 PR、13 个已合并、覆盖 5 位成员（见本次新增 `docs/team/first-pr-merge-records.md`） | ❌（台账已补） |

步骤层面：hello 文档 5/5 ✅（`hello-hexinyi.md` 仅 2 行，内容偏简）；角色承诺书 5 份，其中 3 份标注「待本人确认草稿」→ ⚠️；IM 群与看板创建属外部工具，无法由仓库验证。

### Lab 2（PDF 第 50–51 页）

| # | PDF 提交物 | 仓库实际 | 判定 |
| --- | --- | --- | --- |
| 1 | 用户故事地图 PNG / PDF | `docs/backlog/user-story-map.png`（396 KB）+ `.md` | ✅ |
| 2 | Product Backlog Markdown（ID / 描述 / 优先级 / SP） | `docs/backlog/product-backlog.md`（8 故事 / 27 SP） | ✅ |
| 3 | 至少 8 条 Gherkin 验收标准文件 | `docs/backlog/stories/` 下 `US-001/002/003/004/007/009/012/015.feature` 共 8 个、31 场景 | ✅ |
| 4 | Sprint 1 Backlog 截图（任务分配到人） | `sprint-1-backlog.md`（26 任务）+ `sprint-1-tasks.csv` + Planning 纪要齐全，**截图缺失** | ⚠️ |

前置条件：**每队至少 3 份真实访谈**。实际 `docs/backlog/interviews/` 下 3 个文件均为空白模板，文件头明确写有「当前状态：空白模板，不是真实访谈记录，不能作为 Lab 2 验收证据」→ **0/3 ❌**。这是 Lab 2 最实质的缺口。

### Lab 3（PDF 第 51–52 页）

| # | PDF 提交物 | 仓库实际 | 判定 |
| --- | --- | --- | --- |
| 1 | 事件风暴现场全景照片 | `docs/ddd/event-storming-board.png`（648 KB）+ `event-storming.md` | ✅ |
| 2 | 上下文映射图 PNG | `docs/ddd/context-map.png`（565 KB）+ `context-map.md` | ✅ |
| 3 | 聚合识别清单（聚合名 / 根实体 / 不变式） | `docs/ddd/aggregates.md`（19.8 KB）+ 声誉域三份清单与纪要 | ✅ |
| 4 | 统一语言术语表 v1.0（中英对照，≥30 术语） | `docs/ddd/glossary.md`（14 KB，132 行表格） | ✅ |

Lab 3 是五个实验中唯一四项提交物齐全、且附带主持脚本与研讨纪要的 Lab。

### Lab 4（PDF 第 53–54 页）

| # | PDF 提交物 | 补齐前 | 补齐后 | 判定 |
| --- | --- | --- | --- | --- |
| 1 | 可运行多模块 Maven 工程（`mvn clean install` 全绿） | 27 个模块（父 POM + bootstrap + shared-kernel + 4 上下文 × 6）；团队实跑基线 18 测试类 / 91 用例 / 0 失败；`CampusOverflowFlowIT` 因依赖 S3 能力 `@Disabled` | 未改动 | ⚠️（口径需教师确认） |
| 2 | `application.yml` 启动并连库 | 配置 + Flyway + docker-compose 齐全，团队标注「未实跑（T-031）」 | 未改动 | ⚠️ |
| 3 | `docs/adr/ADR-002-package-structure.md` | **不存在**（团队文档标注 T-036「未开始」）；工程内仅有 ADR 001–008 | 新建 `docs/adr/ADR-002-package-structure.md`（PDF 路径入口）+ `campus-overflow-student/docs/adr/009-包结构分层.md`（正文），ADR README 登记映射 | ✅ |
| 4 | Postman Collection 含 `POST /api/v1/questions` 成功用例 | `campus-overflow.postman_collection.json` 已有 16 个请求（含成功用例），**运行证据缺失（T-035）** | 未改动 | ⚠️ |

代码层面已具备 DDD 分层、`ArchitectureTest` FF-1~FF-6、聚合不变式保护、JPA 分离、Logback JSON 与全局异常处理，质量较高；缺的是「实跑证据」与「决策文档」，后者已补齐。

### Lab 5（PDF 第 55–56 页）

| # | PDF 提交物 | 补齐前 | 补齐后 | 判定 |
| --- | --- | --- | --- | --- |
| 1 | `pnpm dev` 可启动，首页可访问 | ✅（SM 实测 `checks-green.png`） | 保持 | ✅ |
| 2 | `pnpm lint` 与 `pnpm typecheck` 全绿 | ✅ | 本次实跑复验 exit 0 | ✅ |
| 3 | pre-commit 钩子可拦截不合规提交 | ✅（`precommit-block-any.png`） | 保持 | ✅ |
| 4 | 提问 → 列表 → 详情端到端 + ≤30 秒录屏 | 代码链路存在但**未接 TanStack Query**；录屏缺失 | `useMutation` 提问 → 跳详情 → 列表 `useQuery` 链路打通；**录屏仍缺** | ⚠️ |

**实验步骤 / 评分要点的代码缺口（本次全部补齐）**：

| 项 | PDF 要求 | 补齐前 | 补齐后 |
| --- | --- | --- | --- |
| Step 1 依赖 | `zustand`、`antd`（或 Tailwind）、`@tanstack/react-query`、`axios`、`react-router-dom` | 仅装后三者 | 新增 `zustand@5.1.0`、`antd@5.29.3`，`pnpm-lock.yaml` 增量同步 |
| Step 2 别名 | 配置 `@/` 并在业务代码使用 | 配置有，**业务代码 0 处使用** | 11 个文件统一改为 `@/...` 导入 |
| Step 3 服务器状态 | TanStack Query + axios 拦截器 + ErrorBoundary | 拦截器 / QueryClient / ErrorBoundary 已有，**`useQuery` 0 处** | 列表页、详情页 `useQuery`，提问页与写操作 `useMutation` |
| Step 4 客户端状态 | 区分服务器状态与客户端状态（PDF 陷阱） | 筛选条件散落 4 个 `useState` | `src/shared/store.ts` 用 zustand 统一收敛 |
| Step 4 AppShell | Header + Sidebar + Outlet | 仅 Header + `<main>` | `src/shared/AppShell.tsx` 用 Ant Design 5 `Layout` + `Sider` + `Outlet` |
| Step 4 路由 | `/questions`、`/questions/new`、`/questions/:id` | `/`、`/ask`、`/questions/:id` | 对齐课程命名；旧路径保留重定向 |
| 陷阱加固 | CI 中 `tsc --noEmit` | 已有 | 保留；另加 `manualChunks` 分包，最大 chunk 由 589 kB 降至 441 kB |

---

## 四、本次补齐的文件清单

**新增**

- `campus-overflow-student/docs/adr/009-包结构分层.md` — Lab 4 决策正文（背景 / 决策 / 后果 / 被否决方案 / 编号映射）
- `docs/adr/ADR-002-package-structure.md` — PDF 原始路径入口
- `docs/team/first-pr-merge-records.md` — 13 个已合并 PR 的真实台账
- `docs/lab-audit/证据补齐指引.md` — 真实性证据的可复现补齐步骤
- `docs/lab-audit/Lab1-5-核对与补齐报告.md` — 本文件
- `campus-overflow-student/frontend/src/shared/store.ts`、`src/shared/AppShell.tsx`

**修改**

- `README.md` — 重写，准确索引 Lab 1–5 并更新「当前待补」
- `campus-overflow-student/docs/adr/README.md` — 登记 009 与编号映射
- `docs/lab4/提交物与验收口径.md`、`docs/lab5/SM交付说明与验收口径.md` — 同步过时状态
- `campus-overflow-student/frontend/`：`package.json`、`pnpm-lock.yaml`、`vite.config.ts`、`src/main.tsx`、`src/App.tsx`、`src/api.ts`、`src/auth.tsx`、`src/components/GlobalLoading.tsx`、`src/pages/*.tsx`（共 11 个文件改 `@/` 别名，其中 5 个重构状态管理）

---

## 五、明确未代造的内容及原因

以下内容属于**学术真实性证据**，由工具生成等同伪造数据，故一律保留为待办，仅提供可复现的操作路径：

| 类别 | 原因 |
| --- | --- |
| 3 份真实用户访谈实录 | PDF 要求「真实访谈」并作为需求追溯依据；编造受访者原话属学术不端 |
| 团队看板 / Sprint 1 看板截图 | 必须来自 GitHub Projects 实时画面，工具无法生成真实界面截图 |
| 每位成员首次 PR 合并截图 | 同上；本次改以真实 API 数据台账替代，供教师核查 |
| 角色承诺书本人签字 | 需本人确认，不得代签 |
| Lab 5 ≤30 秒端到端录屏 | 需前端 + 后端 + MySQL + Redis 真实联调并录像 |
| Postman 201 响应与落库证据 | 需真实启动后端并调用接口 |

团队 `docs/lab5/evidence/README.md` 已记录 e2e 录屏的环境阻塞（MySQL 账号不可用、Redis 未运行、无 Docker），本次未改变该事实。

---

## 六、验证记录

| 项目 | 命令 / 方法 | 结果 |
| --- | --- | --- |
| 远程同步 | `git fetch origin` | `main...origin/main` 一致，基线 `010487c` |
| 环境 | Windows-mcp PowerShell | Node 22.22.2、JDK 21.0.10、Maven 3.9.13、pnpm 9.15.9（隔离目录生成锁文件） |
| 补齐前基线 | `npm run typecheck` / `lint` / `build` | 全部 exit 0 |
| 补齐后格式 | `npm run format`、`format:check` | `All matched files use Prettier code style!`，exit 0 |
| 补齐后类型 | `npm run typecheck`（`tsc --noEmit`） | exit 0 |
| 补齐后规范 | `npm run lint`（`--max-warnings=0`） | exit 0 |
| 补齐后构建 | `npm run build`（`tsc -b && vite build`） | exit 0；1550 modules，最大 `ui-vendor` 441.53 kB / gzip 143.54 kB，无体积警告 |
| 后端 | 未重跑 `mvn clean install` | 沿用团队 2026-11-12 实跑口径（18 测试类 / 91 用例 / 0 失败）；本机缺 MySQL / Redis，不具备完整实跑条件 |

---

## 七、结论与建议

1. **Lab 3 已完成**；Lab 1 / 2 / 4 / 5 的文档与代码缺口本次已补齐，但仍**不能判定为「全部完成」**。
2. 优先补齐顺序建议：① Lab 2 三份真实访谈（前置条件，影响故事地图可信度）→ ② Lab 5 端到端环境与录屏 → ③ Lab 4 Postman 运行证据 → ④ 看板与 PR 截图 → ⑤ 3 份角色承诺书本人签字。
3. Lab 4 的「`mvn clean install` 全绿」口径（集成测试 `@Disabled`）建议按 `docs/lab4/提交物与验收口径.md` 第六节的话术向教师书面确认。
4. 本次改动遵循团队分支保护约定，通过分支 + PR 合入，不直接推送 `main`。

---

**报告生成**：2026-11-19　|　**依据文件**：`CampusOverflow实验部分.pdf`
