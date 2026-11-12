# CampusOverflow 前端工程（campus-overflow-student/frontend）

> 对应实验：Lab 5 前端工程骨架与对接（第 9 周，Sprint 2）
> 维护人：SM（Flagg32）负责工程化配置与门禁，业务页面由 Developer 迭代
> 最后更新：2026-11-11

---

## 一、技术栈与运行环境

| 项         | 版本 / 选择                           | 说明                                                            |
| ---------- | ------------------------------------- | --------------------------------------------------------------- |
| Node.js    | 20 LTS 及以上（本地实测 v22.12.0）    | CI 使用 Node 20                                                 |
| 包管理器   | **pnpm 9**（本地实测 9.15.9）         | 仓库只保留 `pnpm-lock.yaml`，不再使用 npm lock                  |
| 构建工具   | Vite 5                                | `vite.config.ts` 内含 `/api` → `http://localhost:8080` 开发代理 |
| 框架       | React 18 + TypeScript 5               | 函数组件 + Hooks                                                |
| 类型严格度 | `strict` + `noUncheckedIndexedAccess` | 见 `tsconfig.json`                                              |

> Node 版本过低会在安装依赖时出现 `EBADENGINE` 警告（如 eslint-visitor-keys 要求 `^20.19.0 || ^22.13.0 || >=24`），请升级 Node 后再执行 `pnpm install`。

---

## 二、目录结构

```
frontend/
├─ .editorconfig            # 编辑器统一缩进/换行/字符集
├─ .gitattributes           # 前端目录统一 LF，避免 Windows CRLF 与 Prettier 冲突
├─ .prettierrc.json         # 格式规则（单引号、分号、100 列、LF）
├─ .prettierignore          # 格式检查排除项（dist、node_modules、pnpm-lock.yaml）
├─ eslint.config.js         # ESLint 9 flat config（TS + react-hooks）
├─ .husky/pre-commit        # 提交前门禁（ESLint + Prettier）
├─ tsconfig.json            # strict + noUncheckedIndexedAccess + @/* 路径映射
├─ vite.config.ts           # @ 别名 + /api 开发代理
└─ src/
   ├─ main.tsx              # 入口
   ├─ App.tsx               # 路由与布局
   ├─ api.ts                # 后端请求封装
   ├─ auth.tsx              # 登录态
   ├─ types.ts              # 与后端契约对应的类型
   ├─ styles.css
   └─ pages/                # 页面组件（列表、提问、详情、登录、排行榜、通知）
```

Lab 5 规划中的 `features/`、`shared/` 目录由 Developer 在拆分业务模块时按需补充，SM 不再代建空目录。

---

## 三、常用命令

| 命令                | 作用                                             | 何时用                            |
| ------------------- | ------------------------------------------------ | --------------------------------- |
| `pnpm install`      | 安装依赖（会顺带执行 `prepare` 安装 husky 钩子） | 首次拉代码、`package.json` 变更后 |
| `pnpm dev`          | 启动开发服务器（默认 http://localhost:5173）     | 本地联调                          |
| `pnpm build`        | `tsc -b` 类型构建 + `vite build` 产出 `dist/`    | 发版前、CI                        |
| `pnpm preview`      | 预览 `dist/` 产物                                | 验证构建结果                      |
| `pnpm typecheck`    | `tsc --noEmit` 纯类型检查                        | 提交前、CI                        |
| `pnpm lint`         | ESLint 全量检查（`--max-warnings=0`，零容忍）    | 提交前、CI                        |
| `pnpm lint:fix`     | ESLint 自动修复                                  | 本地整改                          |
| `pnpm format`       | Prettier 覆盖率重写                              | 本地整改                          |
| `pnpm format:check` | Prettier 只检查不修改                            | CI                                |
| `pnpm verify`       | `lint` + `format:check` + `typecheck` 三连       | **提交前自检首选**                |

---

## 四、代码质量门禁

### 4.1 ESLint（`eslint.config.js`）

| 规则 / 插件                          | 配置                              | 目的                                                                 |
| ------------------------------------ | --------------------------------- | -------------------------------------------------------------------- |
| `@typescript-eslint` recommended     | 开启                              | TS 语法与常见错误                                                    |
| `@typescript-eslint/no-explicit-any` | **error**                         | 对应评分点「TypeScript 严格模式无 any（25%）」，`any` 逃逸直接拦提交 |
| `react-hooks/recommended`            | 开启                              | 对应评分点「函数式 + Hooks（25%）」，Hooks 依赖数组与调用位置合规    |
| `no-console`                         | `warn`（仅允许 `warn` / `error`） | 防止调试语句混入提交                                                 |
| `eslint-config-prettier`             | 末位置                            | 关闭与 Prettier 冲突的格式类规则，格式只由 Prettier 判               |
| 构建脚本 / 配置文件                  | Node 环境，关闭 `no-console`      | `vite.config.ts`、`eslint.config.js` 等不属于业务源码                |

### 4.2 Prettier + EditorConfig

- Prettier 3：单引号、分号、行宽 100、`endOfLine: lf`；
- `.prettierignore` 排除 `node_modules`、`dist`、`pnpm-lock.yaml`、`coverage`；
- `.editorconfig` 统一 UTF-8、LF、2 空格缩进，避免编辑器默认设置把文件改成 CRLF；
- `.gitattributes` 在**前端目录**内声明 `* text=auto eol=lf`，只约束前端，不改动后端 Java 文件的既有换行行为。

### 4.3 Husky pre-commit（`.husky/pre-commit`）

提交前依次执行 `eslint . --max-warnings=0` 与 `prettier --check .`，任一失败则中止提交。

**为什么不用 lint-staged**：课程模板默认方案是「Husky + lint-staged」，但 lint-staged v15 在 `Backing up original state` 阶段会去创建仓库索引锁，与 git 2.55（Windows）在提交过程中已持有的 `.git/index.lock` 冲突，实测导致**完全合规的代码也被 "lint-staged failed due to a git error" 拦下**。本团队改为让钩子直接对前端目录做全量检查：拦截效果等价（全量检查严于增量检查），且行为可预期、无版本兼容风险。CI 侧同样执行 `pnpm lint` / `pnpm format:check` 兜底，本地钩子只承担「提前反馈」。

**钩子生效条件**（队友首次拉代码后自检）：

```bash
git config --get core.hooksPath
# 期望输出：campus-overflow-student/frontend/.husky/_
```

若为空，在仓库根执行 `pnpm --dir campus-overflow-student/frontend install`（`prepare` 会自动安装钩子）。

**临时绕过**：`HUSKY=0 git commit -m "..."`（仅限紧急情况，绕过后的代码在 CI 仍会被拦）。

### 4.4 `@/` 路径别名

两处必须保持一致：

- `vite.config.ts`：`resolve.alias['@'] = ./src`
- `tsconfig.json`：`baseUrl: "."` + `paths: { "@/*": ["src/*"] }`

缺任一处都会出现「编辑器不报错但构建失败」或反之。

### 4.5 CI 门禁（仓库根 `.github/workflows/ci.yml`）

`frontend` job 流程：`pnpm/action-setup@v4` → `setup-node@v4`（Node 20，`cache: pnpm`）→ `pnpm install --frozen-lockfile`（带 `HUSKY: 0`，CI 无 git 钩子上下文）→ 依次 `pnpm lint`、`pnpm format:check`、`pnpm typecheck`、`pnpm build`，全部 `working-directory: campus-overflow-student/frontend`。

---

## 五、提交前自检清单

1. `pnpm verify` 全绿；
2. 新增文件已在 `src/` 对应目录，命名遵循 `PascalCase.tsx` 组件 / `camelCase.ts` 模块；
3. 无 `any`、无 `// @ts-ignore`（确需抑制请写明理由并走 review）；
4. 接口调用集中在 `api.ts`，页面不直接写 `fetch`；
5. 提交信息遵循 `feat|fix|docs|chore|ci(scope): 描述`。

---

## 六、常见问题

| 现象                                               | 原因                                         | 处理                                                                 |
| -------------------------------------------------- | -------------------------------------------- | -------------------------------------------------------------------- |
| 提交被拦，提示 ESLint 报错                         | 业务代码不合规                               | `pnpm lint:fix` 后再提交；`any` 需改真实类型                         |
| 提交被拦，`prettier --check` 报格式                | 文件未格式化                                 | `pnpm format`                                                        |
| `git commit` 报 `Unable to create .git/index.lock` | 上一次钩子异常退出残留锁                     | 确认无 git 进程后删除 `.git/index.lock`                              |
| 钩子提示 `frontend dependencies not installed`     | 未执行 `pnpm install`                        | 先装依赖（钩子会跳过检查，CI 仍会拦）                                |
| `pnpm build` 报 `tsc` 类型错误但编辑器无感         | 编辑器用了其他 TS 版本                       | 以 `pnpm typecheck` 为准                                             |
| Prettier 在 Windows 上误报换行                     | `core.autocrlf=true` 与 `endOfLine: lf` 冲突 | 已由 `frontend/.gitattributes` 解决；老文件可 `pnpm format` 重写一次 |

---

## 七、责任划分

| 范围                                                      | 责任人                                      |
| --------------------------------------------------------- | ------------------------------------------- |
| ESLint / Prettier / EditorConfig / Husky / 别名 / CI 门禁 | SM（Flagg32）                               |
| 页面与路由、状态管理、API 客户端、端到端联调              | Developer（团队内分工见 `docs/lab5/` 说明） |

工程化配置的改动请先与 SM 确认，避免各人本地规则不一致导致同一份代码在不同机器上结论不同。
