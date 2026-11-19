# CampusOverflow

CampusOverflow 是面向高校课程的校园问答平台。本仓库用于《软件工程》课程实验，覆盖 **Lab 1 – Lab 5** 的 Scrum 交付物、领域建模、后端多模块工程与前端工程。工程代码位于 `campus-overflow-student/`，实验与团队文档位于根目录 `docs/`。

> 逐条符合性核对见 [Lab 1–Lab 5 核对与补齐报告](docs/lab-audit/Lab1-5-核对与补齐报告.md)；尚缺证据的补齐步骤见 [证据补齐指引](docs/lab-audit/证据补齐指引.md)。

## 团队

| 角色 | 成员 | GitHub |
| --- | --- | --- |
| PO（产品负责人） | 唐志烽 | [luck970](https://github.com/luck970) |
| SM（Scrum Master） | 左焱之 | [Flagg32](https://github.com/Flagg32) |
| Developer | 江思达 | [houhai6](https://github.com/houhai6) |
| Developer | 何欣怡 | [hehe309](https://github.com/hehe309) |
| Developer | 柯博 | [Ke-1226](https://github.com/Ke-1226) |

项目看板：[GitHub Projects #3](https://github.com/users/luck970/projects/3/views/1)

## 实验交付物

### Lab 1：团队组建与协作工具链

- [团队章程 v1.0](docs/team/charter-v1.0.md)
- 成员自我介绍 5/5：[唐志烽](docs/team/hello-luck970.md)、[左焱之](docs/team/hello-flagg32.md)、[江思达](docs/team/hello-jiangsida.md)、[何欣怡](docs/team/hello-hexinyi.md)、[柯博](docs/team/hello-kebo.md)
- 角色承诺书 5 份：[江思达](docs/team/role-commitment-jiangsida.md)、[柯博](docs/team/role-commitment-kebo.md) 已签；[PO](docs/team/role-commitment-luck970.md)、[SM](docs/team/role-commitment-flagg32.md)、[何欣怡](docs/team/role-commitment-hexinyi.md) 为**待本人确认草稿**，需本人签字，不得代签
- [首次 PR 合并记录台账](docs/team/first-pr-merge-records.md)：13 个已合并 PR，覆盖 5 位成员（真实数据，替代截图供核查）

### Lab 2：需求建模工作坊

- [用户故事地图](docs/backlog/user-story-map.md)（[PNG](docs/backlog/user-story-map.png)）
- [Product Backlog](docs/backlog/product-backlog.md)：8 个 Sprint 1 故事，27 SP
- [Sprint 1 Backlog](docs/backlog/sprint-1-backlog.md)：26 个任务及责任人；[任务 CSV](docs/backlog/sprint-1-tasks.csv)、[Planning 纪要](docs/backlog/sprint-1-planning-纪要.md)
- [Gherkin 验收标准](docs/backlog/stories/)：8 个 `.feature`，31 个场景
- [Sprint 2 Backlog](docs/backlog/sprint-2-backlog.md) 与 [Planning 纪要](docs/backlog/sprint-2-planning-纪要.md)
- [用户访谈记录](docs/backlog/interviews/README.md)：**3 份仍为空白模板，未完成真实访谈，不能作为验收证据**

### Lab 3：领域驱动设计事件风暴

- [事件风暴全景图](docs/ddd/event-storming-board.png) 与 [事件清单](docs/ddd/event-storming.md)、[纪要](docs/ddd/lab3-event-storming-纪要.md)、[主持脚本](docs/ddd/lab3-workshop-主持脚本.md)
- [上下文映射](docs/ddd/context-map.md)（[PNG](docs/ddd/context-map.png)）
- [聚合识别清单](docs/ddd/aggregates.md)（含根实体与不变式）、[声誉域聚合清单](docs/ddd/声誉域-聚合识别清单.md)
- [统一语言术语表 v1.0](docs/ddd/glossary.md)（中英对照，远超 30 个术语）

### Lab 4：后端工程骨架与领域代码

- `campus-overflow-student/backend/`：27 个 Maven 模块，4 个限界上下文 × 5 层 + `co-shared-kernel` + `co-bootstrap`；`ArchitectureTest` FF-1~FF-6 在构建期强制分层
- 核心聚合（Question / Answer / Vote）、JPA 与领域对象分离、Flyway 迁移、`@RestControllerAdvice`、Logback JSON 日志
- `POST /api/v1/questions` 与 [Postman Collection](campus-overflow-student/docs/campus-overflow.postman_collection.json)
- ADR：[PDF 路径入口](docs/adr/ADR-002-package-structure.md) → [009-包结构分层](campus-overflow-student/docs/adr/009-包结构分层.md)（002 号已被「事务性发件箱」占用）
- 验收口径：[提交物与验收口径](docs/lab4/提交物与验收口径.md)、[不变式-测试覆盖对照表](docs/lab4/不变式-测试覆盖对照表.md)、[JSON 日志验证记录](docs/lab4/JSON日志验证记录.md)

### Lab 5：前端工程骨架与对接

- React 18 + TypeScript 5 严格模式（`strict` + `noUncheckedIndexedAccess`） + Vite 5 + pnpm 9
- ESLint 9 / Prettier 3 / EditorConfig / Husky pre-commit；[CI 前端门禁](.github/workflows/ci.yml)（lint → format:check → typecheck → build）
- [OpenAPI 契约](campus-overflow-student/frontend/openapi.yaml) → `pnpm gen:api` 生成类型；[axios 拦截器](campus-overflow-student/frontend/src/api/http.ts)（CSRF / 错误统一 / 全局 Loading）；`QueryClientProvider` 与 `ErrorBoundary`
- TanStack Query `useQuery` / `useMutation` 驱动列表、详情、提问页；[zustand 客户端状态](campus-overflow-student/frontend/src/shared/store.ts)；[Ant Design 5 AppShell](campus-overflow-student/frontend/src/shared/AppShell.tsx)（Header + Sidebar + Outlet）；业务代码统一 `@/` 别名；路由 `/questions`、`/questions/new`、`/questions/:id`
- 证据：[质量门禁全绿](docs/lab5/evidence/checks-green.png)、[pre-commit 拦截](docs/lab5/evidence/precommit-block-any.png)、[SM 交付说明](docs/lab5/SM交付说明与验收口径.md)

## 当前待补

以下均为**真实性证据**，必须由团队本人现场产生，不可由工具代造（原因与操作步骤见[证据补齐指引](docs/lab-audit/证据补齐指引.md)）：

| 项目 | 状态 | 说明 |
| --- | --- | --- |
| Lab 1 团队看板截图 | 缺 | 需含初始 Backlog ≥5 条，存 `docs/backlog/sprint-1-board.png` |
| Lab 1 首次 PR 合并截图 | 缺（已有真实台账） | 每人一张，见 [PR 台账](docs/team/first-pr-merge-records.md) |
| Lab 1 角色承诺书 | 2/5 已签 | 3 份草稿待本人签字确认 |
| Lab 2 真实用户访谈 | 0/3 | 3 份文件均为空白模板；须完成真实访谈后填写 |
| Lab 2 Sprint 1 看板截图 | 缺 | 需显示 26 张任务卡已分配到人 |
| Lab 4 Postman 运行证据 | 缺 | 集合已有，需实跑并归档 201 响应与落库证据 |
| Lab 4 空库启动实跑 | 未实跑 | 配置齐全，需 MySQL + Redis 环境实测 |
| Lab 5 端到端录屏 | 缺 | ≤30 秒，需前端 + 后端 + MySQL + Redis 联调，存 `docs/lab5/evidence/e2e-YYYYMMDD.mp4` |
| Project 外部可见性 | 待确认 | 确认教师与队员均可访问 |

## 本地运行

后端：

```bash
cd campus-overflow-student/backend
mvn -DskipTests install
mvn spring-boot:run -pl co-bootstrap
```

前端（团队统一使用 pnpm 9，CI 以 `--frozen-lockfile` 保证可复现）：

```bash
cd campus-overflow-student/frontend
pnpm install --frozen-lockfile
pnpm dev
```

质量门禁：`pnpm lint`、`pnpm format:check`、`pnpm typecheck`、`pnpm build`。

默认地址：前端 `http://localhost:5173`，后端 `http://localhost:8080`，Swagger `http://localhost:8080/swagger-ui.html`。完整后端需要 MySQL 与 Redis。
