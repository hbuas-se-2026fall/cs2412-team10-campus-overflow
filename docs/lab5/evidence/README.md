# Lab 5 提交物证据目录说明

> 维护人：SM（左焱之 / Flagg32）｜ 对应任务：T-047、T-048
> 本目录存放 Lab 5「前端工程骨架与对接」的验收证据。

## 一、已归档证据

| 文件 | 对应提交物 | 说明 |
| --- | --- | --- |
| `checks-green.png` | 提交物② 质量门禁 | `pnpm lint / format:check / typecheck / build / dev` 全绿的终端证据 |
| `precommit-block-any.png` | 提交物② 质量门禁 | 含 `any` 的临时提交被 Husky pre-commit（ESLint）拦截的现场截图 |
| `e2e-*.mp4`（待归档） | 提交物④ 端到端演示 | 见下节录制规范 |

## 二、提交物④ 端到端录屏录制规范

### 2.1 前置条件（缺一不可）

1. 前端 T-044~T-046 已合并入 main：`@tanstack/react-query` 等依赖就位、列表页与详情页接 `useQuery`、全局 `ErrorBoundary` 生效；
2. 后端可启动：MySQL 中已建 `campus_overflow` 库并完成 Flyway 五个上下文的迁移，Redis 已启动（`spring-session-data-redis` 会话存储依赖 Redis）；
3. 前端 `pnpm dev` 启动成功（默认 http://localhost:5173），`/api` 代理指向 http://localhost:8080；
4. 录制分辨率建议 1280×720 以上，画面内需可辨认 URL 与页面内容。

### 2.2 录制内容（≤ 30 秒，三画面不可缺）

| 顺序 | 画面 | 验收点 |
| --- | --- | --- |
| 1 | 提问 | 在 `/ask` 填写标题、正文、标签并提交成功，页面跳转离开表单 |
| 2 | 列表刷新 | 回到 `/`，新提问出现在列表首位（证明走后端落库而非前端假数据） |
| 3 | 详情查看 | 点入 `/questions/{id}`，标题、正文、标签、回答区正确渲染 |

### 2.3 归档命名

- 文件名：`e2e-YYYYMMDD.mp4`（例：`e2e-20261115.mp4`）；
- 存放位置：本目录；
- 同时在该文件旁的提交记录中注明：录制人、录制时间、后端 commit（`git rev-parse --short HEAD`）、前端 commit。

## 三、当前阻塞说明（2026-11-12 由 SM 实测）

T-048 的提交物④尚无法录制，原因如下（均为外部依赖，非前端工程质量问题）：

1. **T-046 未完成**：远端分支 `feature/lab5-step3-frontend-jiangsida`（5493b25）仅含 OpenAPI 契约、axios 拦截器、QueryClient 与 `ErrorBoundary`，全部源码无一处 `useQuery`，列表页与详情页尚未改造，画面 1~3 的串联演示不成立；
2. **本地数据库账号不可用**：`campus` 用户连接 MySQL 8.4 返回 `ERROR 1045 Access denied`，`campus_overflow` 库与专用账号尚未创建；
3. **Redis 未运行**：`spring-session-data-redis` 为会话存储实现，6379 端口无监听，登录态无法建立；
4. 本机未安装 Docker，无法直接 `docker compose up` 拉起 MySQL / Redis。

解锁顺序建议：T-046 合入 → 准备后端依赖环境（仓库当前未提供 `docker-compose.yml`，需在本机 MySQL 建 `campus_overflow` 库与 `campus` 账号，并安装启动 Redis，或以容器方式另行拉起）→ SM 录制并归档。
