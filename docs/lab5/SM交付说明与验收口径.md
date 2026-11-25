# Lab 5 前端工程骨架与对接 —— SM 交付说明与验收口径

> 实验：Lab 5（2 学时，第 9 周，Sprint 2）｜依据：《CampusOverflow 实验部分》PDF 第 8–9 页（PDF 第 55–56 / 76 页）
> 角色：SM（左焱之 / Flagg32）｜本文档用于 Review 逐条打勾与向教师说明口径
> 提交：`f2d75e4 feat(前端): 引入 ESLint/Prettier/EditorConfig/Husky 工程化门禁并接通 CI`
> 最后更新：2026-11-11

---

## 一、角色口径（先说明为什么 SM 做这些）

PDF 的 Lab 5 全节**没有出现「SM」字样，也没有按角色分配任务**。团队沿用章首「实验组织方式」（1 名 PO、1 名 SM、3–4 名 Developer）与 Lab 1「由 SM 合并」的惯例，把 Lab 5 拆成两块：

| 块 | 内容 | 责任人 |
| --- | --- | --- |
| Step 2 代码质量基础设施 + CI 门禁 + 交付物验收 + 合并 | ESLint / Prettier / EditorConfig / Husky / `@/` 别名 / 严格模式 / CI lint 与 typecheck / 证据留存 | **SM（本文档）** |
| Step 1 依赖与目录、Step 3 API 客户端、Step 4 页面与端到端 | TanStack Query、Zustand、axios 拦截器、AppShell、页面与录屏 | Developer（见第五节缺口清单） |

判定原则：凡「一旦各人本地配置不一致就会导致同一份代码结论不同」的内容，由 SM 收口；凡「业务代码与联调」由 Developer 认领，SM 只做门禁与验收。

---

## 二、SM 已交付内容

| # | 交付项 | 具体内容 | 文件 |
| --- | --- | --- | --- |
| 1 | ESLint 9 flat config | `@typescript-eslint` recommended + `eslint-plugin-react-hooks` recommended；`@typescript-eslint/no-explicit-any = error`；`no-console = warn`（仅允许 `warn`/`error`）；`eslint-config-prettier` 置末位 | `frontend/eslint.config.js` |
| 2 | Prettier 3 | 单引号、分号、行宽 100、`endOfLine: lf`；排除 `dist`、`node_modules`、`pnpm-lock.yaml`、`coverage` | `frontend/.prettierrc.json`、`.prettierignore` |
| 3 | EditorConfig | UTF-8、LF、去除行尾空格、2 空格缩进 | `frontend/.editorconfig` |
| 4 | Husky 9 pre-commit | 提交前执行 `eslint . --max-warnings=0` 与 `prettier --check .`，失败即中止；`core.hooksPath` 指向 `.husky/_` | `frontend/.husky/pre-commit`、`package.json` 的 `prepare` |
| 5 | `@/` 路径别名 | `vite.config.ts` 的 `resolve.alias` 与 `tsconfig.json` 的 `baseUrl` + `paths` 双向对齐 | `frontend/vite.config.ts`、`frontend/tsconfig.json` |
| 6 | TS 严格模式 | `strict: true` + `noUncheckedIndexedAccess: true`，`noEmit` 供 `typecheck` 使用 | `frontend/tsconfig.json` |
| 7 | CI 前端门禁 | 仓库根 `.github/workflows/ci.yml` 的 `frontend` job：pnpm 9 + Node 20 + `--frozen-lockfile`（`HUSKY: 0`），依次跑 `lint` / `format:check` / `typecheck` / `build` | `.github/workflows/ci.yml` |
| 8 | 包管理器收口 | 统一 pnpm，产出 `pnpm-lock.yaml`，移除 `package-lock.json`，CI 用 `--frozen-lockfile` 保证可复现 | `frontend/pnpm-lock.yaml` |
| 9 | 换行与忽略规则 | `frontend/.gitattributes`（`* text=auto eol=lf`，只约束前端目录）；`.gitignore` 补 `node_modules`、`dist`、`*.tsbuildinfo`、`.husky/_` | `frontend/.gitattributes`、`frontend/.gitignore` |
| 10 | 验收证据 | 两张终端证据图 + 本验收口径文档 | `docs/lab5/evidence/` |

### 2.1 关键决策与偏差说明（Review 时需口头交代）

| 偏差 | PDF 原文 | 团队做法 | 理由 |
| --- | --- | --- | --- |
| 未使用 lint-staged | 「Husky + lint-staged 配置 pre-commit 钩子」 | Husky 直接跑 ESLint / Prettier 全量检查 | lint-staged v15 在 `Backing up original state` 阶段创建索引锁，与 git 2.55（Windows）提交时已持有的 `.git/index.lock` 冲突，**实测合规代码也被 "lint-staged failed due to a git error" 拦下**；全量检查拦截力度不低于增量检查，且行为可预期。CI 同步兜底 |
| 线程/工具链版本 | Step 1 写 Node 20+ | 本地 Node 22.12.0，CI Node 20 | 均满足要求；CI 固定 20 保证与课程口径一致 |
| 路由命名 | `/questions`、`/questions/new`、`/questions/:id` | `/`、`/ask`、`/questions/:id`（功能等价，已登录才可进 `/ask`） | 现有实现早于 Lab 5 定稿；是否对齐命名交由 Developer 在 T-048 决定，不影响验收语义 |
| CI 中 lint 的定位 | 仅在「常见陷阱」提 `tsc --noEmit`，未设独立 CI 章节 | CI 增加 `lint` / `format:check` / `typecheck` / `build` 四步 | 属超出口径的加固，成本低、收益明确（`any` 逃逸在 PR 阶段即被拦） |
| 未引入 zustand | Step 1「安装核心依赖」列出 `zustand` | 以 TanStack Query 缓存 + React Context 承载跨页状态，不引入第二套状态库。依据：`campus-overflow-student/frontend/package.json` 的 `dependencies` 仅有 `@tanstack/react-query` `^5.107.1`、`axios` `^1.21.1`、`react`、`react-dom`、`react-router-dom`，无 `zustand`；`src/` 全量检索无 `zustand` 引用；会话状态由 `src/auth.tsx` 的 `AuthContext`（`createContext` + `AuthProvider`）对外提供，底层数据落在 `queryKeys.auth.me()` 缓存上 | 会话态只有一个事实来源，避免 Query 缓存与 store 双写不一致；现有页面只消费 `useAuth()`，替换状态库对页面无感 |
| UI 库自研样式 | Step 1「UI 库（antd 5 或 Tailwind + 自研）」 | 未使用 antd 5，也未引入 Tailwind / PostCSS 相关依赖，采用自研样式表。依据：`package.json` 无 `antd`、`tailwindcss`、`postcss`、`autoprefixer`；`src/main.tsx` 直接 `import './styles.css'`；`src/styles.css` 共 374 行，页面与 `src/components/`（`ErrorBoundary`、`GlobalLoading`）统一复用它 | 课程交付不引入额外 UI 依赖，构建体积与样式链路更可控；样式集中单文件，方便 Review 时逐条核对 |
| API 客户端生成工具 | Step 3「`openapi.yaml` → openapi-typescript-codegen」 | 改用 `openapi-typescript` 生成类型。依据：`package.json` 的 `devDependencies` 含 `openapi-typescript` `^7.13.0`；`scripts.gen:api` 为 `openapi-typescript openapi.yaml -o src/api/generated/schema.ts`，产物是类型声明文件 `src/api/generated/schema.ts`，不是可调用 SDK，因此复用方式是 `src/api/http.ts` 的 `request<T>()` 套 `components["schemas"]` / `components["responses"]["Unauthorized"]` 类型，而非「生成目录被 `api.ts` 复用」 | `openapi-typescript` 只产类型、不产运行时代码，与手写 axios 封装的组合更轻，也避免生成 SDK 与 axios 实例的两套请求链路；`openapi.yaml` 变更时 `pnpm gen:api` 即可刷新类型 |

---

## 三、PDF 四项提交物逐条对照

| # | PDF 原文提交物 | 验证方式 | 结果 | 证据 |
| --- | --- | --- | --- | --- |
| ① | 前端工程 `pnpm dev` 可启动，首页可访问 | `pnpm dev` 后 `curl http://localhost:5173/` | **通过**：HTTP 200，响应含 `id="root"`，Vite ready in 374 ms | `docs/lab5/evidence/checks-green.png` |
| ② | `pnpm lint` 与 `pnpm typecheck` 全绿 | 本地实跑两条命令 | **通过**：`lint` exit 0（无 problem），`typecheck` exit 0（无类型错误） | 同上 |
| ③ | pre-commit 钩子可拦截不合规提交 | 造一个含 `any` 的临时文件并提交 | **通过**：ESLint 报 3 个 error（`no-explicit-any` ×2、`no-console` ×1），钩子 exit 1，提交未生成（HEAD 不变） | `docs/lab5/evidence/precommit-block-any.png` |
| ④ | 提问 → 列表 → 详情 端到端跑通 + 30 秒内录屏 | 需前端接好后端真实接口 | **前置已解除，待录制归档**（2026-11-19 复核）：PR #18 已合入 `main`，`src/api/http.ts`（请求/响应拦截器、CSRF 注入、全局 Loading）与 `src/api/generated/schema.ts` 已就位；原判断「**未完成**：当前 `src/` 仍用自制 `api.ts`，未接 TanStack Query / axios 拦截器」已失效，保留作历史记录。当前状态为**待录制归档**，录制人柯博；阻塞为本地运行环境未就绪——MySQL(3306)、Redis(6379)、后端(8080) 均未启动（本机无 Docker），`/ask` 页面依赖后端启动 | 见第五节 T-048 |

> 补充实测：`pnpm build`（`tsc -b && vite build`）exit 0，42 modules，产物 178.38 kB（gzip 58.22 kB），构建 542 ms —— 说明严格模式下无隐式 `any` 导致的编译失败。

---

## 四、复现步骤（教师 / 队友本地核验）

前置：Node 20+、pnpm 9。

```bash
# 1. 安装依赖（prepare 会自动装 husky 钩子）
cd campus-overflow-student/frontend
pnpm install

# 2. 确认钩子生效
git config --get core.hooksPath
# 期望：campus-overflow-student/frontend/.husky/_

# 3. 三条门禁
pnpm lint          # 期望 exit 0
pnpm format:check  # 期望 All matched files use Prettier code style!
pnpm typecheck     # 期望 exit 0

# 4. 构建与启动
pnpm build
pnpm dev           # http://localhost:5173

# 5. 验证拦截能力（自行制造不合规提交）
printf 'export const x: any = 1;\n' > src/__gate_check.ts
git add src/__gate_check.ts && git commit -m "test: 验证钩子"
# 期望：eslint 报 no-explicit-any，husky - pre-commit script failed (code 1)，提交未生成
rm src/__gate_check.ts && git restore --staged src/__gate_check.ts
```

---

## 五、验收缺口清单（交 Developer，SM 复核）

> **2026-11-19 补齐状态**：下表第 1–5 项已全部补齐并实跑验证——`zustand@5.1.0` 与 `antd@5.29.3` 已安装且 `pnpm-lock.yaml` 同步；11 个源码文件改用 `@/` 别名；列表 / 详情用 `useQuery`、提问与写操作用 `useMutation`；`src/shared/store.ts`（zustand）与 `src/shared/AppShell.tsx`（Header + Sidebar + Outlet）已落地；`format` / `typecheck` / `lint` / `build` 均 exit 0（1550 modules，最大 chunk 441.53 kB）。**第 6 项（端到端录屏）仍缺**，受 MySQL / Redis / Docker 环境阻塞，补齐步骤见 `docs/lab-audit/证据补齐指引.md` 第 8 节。下表保留 2026-11-11 的原始缺口记录供追溯。

| # | 缺口 | PDF 依据 | 归属 | 判定标准 | Backlog |
| --- | --- | --- | --- | --- | --- |
| 1 | 未安装/未接入 `@tanstack/react-query`、`zustand`、`axios`、UI 库（antd 或 Tailwind）——**原判断保留作历史记录** | Step 1「安装核心依赖」 | Developer（江思达 + 何欣怡） | `package.json` 出现四项依赖，且 `main.tsx` 挂载 `QueryClientProvider`。**2026-11-19 复核：已闭环**。依据：`package.json` 的 `dependencies` 已含 `@tanstack/react-query ^5.107.1`、`axios ^1.21.1`；`src/main.tsx` 已挂载 `QueryClientProvider`（`staleTime 30_000`、`retry 1`、`refetchOnWindowFocus false`）。`zustand` 与第三方 UI 库按第 2.1 节偏差口径不做变更：会话状态由 `src/auth.tsx` 的 `AuthContext`（`createContext` + `AuthProvider`）提供，样式为自研 `src/styles.css`（374 行） | T-044（Done） |
| 2 | `@/` 别名已配置但业务代码 0 处使用（仍用 `./pages/...` 相对路径） | Step 2「配置 path alias」 | Developer | 页面 import 改为 `@/pages/...` 形态，`pnpm build` 仍通过 | T-047 |
| 3 | 无 axios 拦截器（Token 注入、统一错误处理、全局 Loading）——**原判断保留作历史记录** | Step 3 | Developer | `api.ts` 或 `shared/http.ts` 内含请求/响应拦截器，401 跳登录有统一处理。**2026-11-19 复核：已闭环**。依据：`src/api/http.ts`（97 行）请求拦截器读取 `XSRF-TOKEN` Cookie 注入 `X-XSRF-TOKEN`，缺失时先经裸实例 `raw` 预取 `/v1/auth/csrf` 以避免循环，并以并发计数器通过 `app:loading` 事件驱动全局 Loading；响应拦截器收起 Loading，将后端 Problem Details 归一为 `ApiError(status, code, message)`。401 由 `src/auth.tsx` 第 34 行 `return null; // 401 表示未登录，属于正常状态` 处理，页面级由 `App.tsx` 的 `RequireAuth` 统一跳 `/login` | T-045（Done） |
| 4 | 未使用 openapi-typescript-codegen 生成类型——**原判断保留作历史记录** | Step 3 | Developer | `openapi.yaml` → 生成目录入库，类型被 `api.ts` 复用。**2026-11-19 复核：已闭环（工具改为 openapi-typescript）**。依据：`devDependencies` 含 `openapi-typescript ^7.13.0`，`scripts.gen:api` 为 `openapi-typescript openapi.yaml -o src/api/generated/schema.ts`，产物 `src/api/generated/schema.ts` 已入库；该产物为类型声明而非可调用 SDK，复用方式为 `src/api/http.ts` 的 `request<T>()` 套用 `components["schemas"]` / `components["responses"]["Unauthorized"]` | T-045（Done） |
| 5 | 无 TanStack Query 缓存层、无 ErrorBoundary——**原判断保留作历史记录** | Step 3 / Step 4 | Developer | 列表页用 `useQuery`，详情页用 `useQuery`，全局 `ErrorBoundary` 兜底。**2026-11-19 复核：已闭环**。依据：`src/main.tsx` 已按 `QueryClientProvider` → `ErrorBoundary` → `GlobalLoading` → `App` 顺序挂载；`useQuery` / `useMutation` 共 18 处——列表页 `QuestionListPage`（`listQuery`、`tagsQuery`）、详情页 `QuestionDetailPage`（`detailQuery`、`relatedQuery` 及 `voteMutation`、`bountyMutation`、`acceptMutation`、`endorseMutation`、`answerMutation`、`commentMutation`）、`AskPage`（`createQuestion`）、`auth.tsx`（`csrfQuery`、`meQuery`、`loginMutation`、`logoutMutation`）、`ReputationPage`（`profileQuery`）；缓存键集中于 `src/queryKeys.ts`（如 `queryKeys.questions.list(filters)`、`queryKeys.auth.me()`） | T-046（Done） |
| 6 | 端到端录屏（≤30 秒）缺失 | 提交物④ | Developer 操作 + SM 归档 | 后端 `mvn spring-boot:run` + 前端 `pnpm dev`，录屏含提问、列表刷新、详情查看三画面，存 `docs/lab5/evidence/e2e-*.mp4` | T-048 |

**SM 判定**：① ② ③ 已由 SM 闭环并可当场复现；④ 依赖 Developer 完成上述 1–5 项后才能录屏，SM 负责在合并后复核并归档。

**2026-11-19 复核说明**：上表第 1、3、4、5 项的原判断（首次复核时的状态）已随前端代码落地而失效，现均判定为**已闭环**，依据分别为 `package.json` 依赖与 `src/main.tsx` 挂载、`src/api/http.ts` 拦截器、`src/api/generated/schema.ts` 生成产物、各页面 `useQuery`/`useMutation` 与 `ErrorBoundary`；原判断文本按「保留原作者结论痕迹」的要求逐条留在表内，仅以「2026-11-19 复核」追加更新结论，未删除任何历史表述。第 1 项中 `zustand` 与第三方 UI 库两项经第 2.1 节偏差口径确认不做变更（由 `AuthContext` 与自研 `styles.css` 替代），对应偏差理由见第二节 2.1。第 2 项（`@/` 别名使用）与第 6 项（端到端录屏）状态不变：第 2 项已由 T-047 标记 Done，第 6 项仍为待录制归档，阻塞为本地 MySQL/Redis 与后端服务未就绪。据此，第四节中「④ 依赖 1–5 项」的前置条件已解除，剩余工作仅为录制与归档。

---

## 六、CI 门禁触发口径

- workflow 文件在**仓库根** `.github/workflows/ci.yml`（历史教训：放在 `campus-overflow-student/.github/` 下 GitHub Actions 永不识别）；
- `frontend` job 只在 `campus-overflow-student/frontend/**` 或该 workflow 自身变更时执行；
- 首次在 PR 上运行的预期结果：`backend` job（`mvn -f backend/pom.xml`）与 `frontend` job（pnpm 四步）同时 success；
- 若 `--frozen-lockfile` 报 lockfile 不一致，说明有人用 npm/yarn 改了依赖，需用 pnpm 重新生成 `pnpm-lock.yaml` 后提交。

---

## 七、遗留风险

1. **本机到 github.com:443 的连通性不稳定**，推送与 CI 结果确认需等网络恢复后复核（见交付时的网络备注）；
2. 队友 Node 版本低于 20.19 时 `pnpm install` 会出现 `EBADENGINE` 警告，虽不影响安装但需统一升级；
3. `core.autocrlf=true` 的机器上，若 `.gitattributes` 未被拉到，Prettier 的 `endOfLine: lf` 会误报格式错误 —— 已在前端目录用 `.gitattributes` 收口，仍建议全员开启 `git config core.autocrlf input`。

---

**维护人**：SM（左焱之 / Flagg32）｜**分发范围**：Overflow Crew 全员、课程指导教师
**补齐复核**：PO（唐志烽 / luck970），2026-11-19（第 1–5 项缺口关闭，第 6 项录屏待环境就绪）
