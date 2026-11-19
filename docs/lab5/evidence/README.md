# Lab 5 提交物证据目录说明

> 维护人：SM（左焱之 / Flagg32）｜ 对应任务：T-047、T-048
> 本目录存放 Lab 5「前端工程骨架与对接」的验收证据。

## 一、已归档证据

| 文件 | 对应提交物 | 说明 |
| --- | --- | --- |
| `checks-green.png` | 提交物② 质量门禁 | `pnpm lint / format:check / typecheck / build / dev` 全绿的终端证据 |
| `precommit-block-any.png` | 提交物② 质量门禁 | 含 `any` 的临时提交被 Husky pre-commit（ESLint）拦截的现场截图 |
| `e2e-20261119.mp4` | 提交物④ 端到端演示 | 登录 → 提问 → 详情 → 列表 → 详情，1280×720，23.9s，见 2.4 录制记录 |

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

## 三、录制记录（2.3 要求的旁注信息）

### 3.1 `e2e-20261119.mp4`

| 项 | 内容 |
| --- | --- |
| 录制人 | 左焱之（Flagg32，SM） |
| 录制时间 | 2026-11-19 10:00–10:09（CST） |
| 后端 commit | `8e6f715`（`co-bootstrap/target/campus-overflow.jar`，Spring Boot 3.4.5 / JDK 21） |
| 前端 commit | `8e6f715`（`frontend/`，Vite 5.4.21 dev server） |
| 规格 | 1280×720，H.264（yuv420p），25 fps，23.9 s，≤30 s 满足规范 |
| 运行环境 | 后端 `http://localhost:8080`；前端 `http://localhost:5173`（`/api` 代理至 8080）；MySQL 8.4 临时实例 `127.0.0.1:3307/campus_overflow`（Flyway 五上下文已迁移）；Redis 5.0.14 `127.0.0.1:6379` 承接 session |
| 演示账号 | `demo2026`（STUDENT，首次录制前经 `/api/v1/auth/register` 注册） |

### 3.2 时间轴与验收点对照

画面上沿常驻一条 URL 徽标（深色底、绿色等宽字），用于在无地址栏的录制窗口中辨认当前路由。

| 时间段 | 画面 | 实际 URL | 验收点 |
| --- | --- | --- | --- |
| 0.0–5.6 s | 登录 | `/login` | 学号+密码提交成功，跳回首页，头部出现 `演示同学 / STUDENT` |
| 5.6–16.1 s | 提问（画面 1） | `/questions/new` | 逐字键入标题、正文、标签后点「发布问题」，POST `/api/v1/questions` 成功并跳转离开表单 |
| 16.1–20.0 s | 详情（画面 3 首证） | `/questions/5` | 标题、正文、标签正确渲染，证明写库成功 |
| 20.0–23.6 s | 列表刷新（画面 2） | `/` | 回到首页重新拉取列表，新提问「红黑树插入旋转后父节点颜色判定总是出错该怎么排查」出现在列表首位，证明数据来自后端而非前端假数据 |
| 23.6–23.9 s | 详情（画面 3） | `/questions/5` | 从列表点击条目进入详情，标题、正文、标签与回答区正常渲染 |

> 说明：规范中「画面 1 在 `/ask` 提交」的旧地址已由 `App.tsx` 重定向（`/ask` → `/questions/new`），故录屏使用资源化后的真实路由 `/questions/new`，二者指向同一 `AskPage`。

## 四、历史阻塞说明（2026-11-12 记录，已于 2026-11-19 解除）

原 T-048 提交物④无法录制的四项原因现已全部消除：

1. T-046 已完成并经 PR #25 合入 main，`useQuery` 已落在 `QuestionListPage.tsx` / `QuestionDetailPage.tsx` / `AskPage.tsx` / `auth.tsx`；
2. 本机新建 `campus_overflow` 库与 `campus` 专用账号（MySQL 8.4 临时实例 3307），Flyway 五上下文迁移通过；
3. Redis 已启动并监听 6379，登录态写入 `co:session:*` 正常；
4. 无 Docker 条件下改为本机原生服务组合，录屏链路已跑通。

遗留非阻塞项：`/actuator/health` 顶层返回 503，根因为 `RedisReactiveHealthIndicator` 读取 Redis `INFO` 不兼容，属健康探针问题，不影响 API 与会话链路（可用 `--management.health.redis.enabled=false` 使探针转 UP）。

> 原解锁顺序建议（2026-11-12，已失效）：T-046 合入 → 准备后端依赖环境（仓库当前未提供 `docker-compose.yml`，需在本机 MySQL 建 `campus_overflow` 库与 `campus` 账号，并安装启动 Redis，或以容器方式另行拉起）→ SM 录制并归档。


## 五、2026-11-19 现状复核（柯博补记）

> 衔接说明：本节为柯博于 2026-11-19 录入的现状复核，保留原文以体现问题闭环过程。其中「本地环境未就绪、无法录制」与「录制分工为柯博」两项已被后续实际执行覆盖——录屏由 SM（左焱之 / Flagg32）于 2026-11-19 10:00–10:09 在本机原生 MySQL/Redis 环境完成，已归档为 `e2e-20261119.mp4`，规格与时间轴见 3.1、3.2。

对上节四条阻塞逐条复核，结论如下：

1. **原阻塞 1（T-046 未完成）已解除**：前端主链路已随 PR #18 合入 main。列表页 `QuestionListPage` 的 `listQuery` / `tagsQuery`、详情页 `QuestionDetailPage` 的 `detailQuery` / `relatedQuery` 均为 `useQuery`，提问、投票、回答等写操作走 `useMutation`（全前端 18 处 `useQuery` / `useMutation`）；`main.tsx` 已挂载 `QueryClientProvider`（内含 `ErrorBoundary` 与 `GlobalLoading`）；API 层为 `src/api/http.ts`（CSRF / Loading 请求拦截、Problem Details 响应拦截）配 `openapi-typescript` 生成的 `src/api/generated/schema.ts` 类型。2.1 节「前置条件 1」至此满足。
2. **原阻塞 2~4（本地环境）仍存在，且为当前唯一阻塞**：MySQL(3306)、Redis(6379)、后端(8080)、前端 `pnpm dev`(5173) 四个端口均未监听，本机仍无 Docker，无法 `docker compose up` 拉起依赖。即 2.1 节「前置条件 2、3」尚不满足，2.2 节三画面（提问 → 列表刷新 → 详情）无法串联录制。
3. **录制分工**：提交物④ 的录制人按团队分工为**柯博**，录制范围含 Step 4 的提问链路、登录流程与 30 秒端到端录屏；SM（Flagg32）负责规范性复核与归档到本目录（命名 `e2e-YYYYMMDD.mp4`，并在旁注记录录制人、录制时间、前后端 commit）。
4. **下一步动作**：① 在本机 MySQL 建 `campus_overflow` 库并建专用账号（替换上节报 `ERROR 1045` 的 `campus` 账号），完成 Flyway 五上下文迁移；② 安装并启动 Redis（`spring-session-data-redis` 依赖）；③ 启动后端 8080 与前端 5173；④ 柯博按 2.2 节三画面录制并归档，归档后 T-048 置 Done。

> 说明：本节只登记现状与分工，未改动上节原始记录；上节「阻塞 1」已失效，但保留以体现问题闭环过程。

---

**维护人**：SM（左焱之 / Flagg32）　|　**最后更新**：2026-11-19（T-066 录屏 `e2e-20261119.mp4` 归档，并合并柯博现状复核补记）
