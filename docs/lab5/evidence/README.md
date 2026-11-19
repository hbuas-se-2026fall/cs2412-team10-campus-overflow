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
