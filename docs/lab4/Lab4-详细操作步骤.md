# Lab 4 后端工程骨架与领域代码 · 详细操作步骤（逐条可执行版）

> 依据：`D:\softwareEngineering\CampusOverflow实验部分.pdf` 第五章 Lab 4（PDF 第 53—55 页），2 学时 · 第 7 周 · Sprint 2。
> 适用仓库：`D:\softwareEngineering\cs2412-team10-campus-overflow`（后端工程在 `campus-overflow-student\backend`）。
> 执行人：何心怡（Sprint 2 任务 T-031 / T-034 / T-035，答辩切片「基础设施与持久化」）。
> 整理日期：2026-11-25。文中「本机实测」均为该日在本机真实执行的结果。

---

## 0. 先明确：Lab 4 到底要交什么

| # | PDF 提交物 | 落到本仓库的位置 | 判定标准 |
| --- | --- | --- | --- |
| ① | 可运行的多模块 Maven 工程，`mvn clean install` 全绿 | `campus-overflow-student/backend/`（27 模块） | BUILD SUCCESS、单测 0 失败、架构测试通过 |
| ② | 通过 `application.yml` 即可启动并连库 | `co-bootstrap/src/main/resources/application.yml` + 仓库根 `docker-compose.yml` | 空库启动成功、Flyway 5 条 version 全 SUCCESS、`qa_question`/`qa_answer`/`id_user` 表存在 |
| ③ | `docs/adr/ADR-002-package-structure.md` | 顺延为 `campus-overflow-student/docs/adr/009-包结构分层.md` | 四段结构齐全 + ADR README 登记映射 |
| ④ | Postman Collection 含 `POST /api/v1/questions` 成功用例 | `campus-overflow-student/docs/campus-overflow.postman_collection.json` | 返回 201 + 数据库有记录 + 运行证据入 `docs/lab4/` |

评分：工程结构 30% / 聚合不变式 25% / 端到端接口 25% / 代码风格与文档 20%。

**本轮已替你完成的准备工作**（详见第 10 节）：根目录 `docker-compose.yml` 补了 Redis 服务；Postman 集合的登录账号由占位 `demo/demo12345` 改为演示账号 `2023001/Campus2026`，`courseId` 由占位的 100 改为 1；新增一键验证脚本 `lab4-verify.ps1`；本机实跑了标准构建与架构测试。

---

## 1. 准备阶段：把环境调到能跑的状态

### 1.1 启动 Docker Desktop（当前阻塞点）

本机检测命令与结果：

```
docker version --format "{{.Server.Version}}"
→ failed to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine;
  check if the path is correct and if the daemon is running
```

说明 Docker Desktop 尚未启动。**先在 Windows 开始菜单启动 Docker Desktop，等托盘图标变绿**，再执行 `docker info`，能看到 `Server Version` 即继续。后面的数据库、Flyway、Postman 验证都依赖它；构建和测试不依赖 Docker，可以先跑（第 2 节已实跑）。

### 1.2 起 MySQL + Redis

仓库根 `docker-compose.yml` 已包含两个服务（MySQL 8.0 + Redis 7），凭据与 `application.yml` 默认值一致：

```
cd /d D:\softwareEngineering\cs2412-team10-campus-overflow
docker compose up -d
docker ps
```

期望看到 `co-mysql (healthy)`、`co-redis (healthy)`，端口分别是 3306、6379。

> 为什么必须有 Redis：ADR-003 采用服务端会话（Spring Session + Redis），登录成功后 `SESSION` Cookie 对应的会话写在 Redis 里。只有 MySQL 时登录会失败或会话立刻失效。这是本轮补进 compose 的原因。

### 1.3 确认当前工作区状态

```
git branch --show-current      → docs/lab3-glossary-hexinyi
git status --short
```

当前有 3 处本地改动：`SecurityConfig.java`、`application.yml`、新增 `docker-compose.yml`。其中 `application.yml` 已把默认 profile 由 `dev` 改为 `demo`、凭据改为 `campusoverflow/co_user/co_pass`（与根 compose 一致），这是可用的路线，本文档按它继续。**建议**：先把这三处改动提交到 `feature/lab4-hexinyi` 分支，再开始后续验证，避免实验证据与未提交改动混在一起。

> 关于 `SecurityConfig.java` 里新增的两行（`/api/v1/questions` 的 `permitAll` 与 CSRF `ignoringRequestMatchers`）：`QuestionController.post()` 的参数是 `Actor`，`ActorArgumentResolver` 在未登录时会抛 `AuthenticationCredentialsNotFoundException("请先登录")`，所以这两行**并不能实现匿名发帖**，但会削弱安全约束。建议按第 5 节走正规「CSRF → 登录 → 发帖」流程，然后撤回这两行；若保留，答辩时需说明「放行仅为本地调试，最终版已恢复」。

---

## 2. Step 1 · Maven 多模块骨架（PDF 25 分钟）

### 2.1 逐项核对父 POM

打开 `campus-overflow-student/backend/pom.xml`，确认：

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.5</version>
</parent>
<packaging>pom</packaging>
<properties>
    <java.version>21</java.version>
    <logstash-encoder.version>8.0</logstash-encoder.version>
    <jacoco.version>0.8.12</jacoco.version>
    <domain.coverage.minimum>0.70</domain.coverage.minimum>
</properties>
<modules>
    <module>co-shared-kernel</module>
    <module>co-identity</module>
    <module>co-qa</module>
    <module>co-reputation</module>
    <module>co-discovery</module>
    <module>co-bootstrap</module>
</modules>
```

父 POM 还统一管理 JUnit 5、ArchUnit、JaCoCo、logstash-encoder 版本，并在 `dependencyManagement` 中登记所有子模块坐标——子模块 pom 里不写版本号。

### 2.2 查看并核对模块树

```
cd campus-overflow-student/backend
find . -name pom.xml -not -path "*/target/*" | sort
```

期望 1 个父 POM + 27 个子模块 pom。结构上每个业务上下文（`co-identity` / `co-qa` / `co-reputation` / `co-discovery`）下各有 5 个子模块：`*-api`、`*-domain`、`*-application`、`*-infrastructure`、`*-interfaces`；再加 `co-shared-kernel` 与 `co-bootstrap` 及 4 个上下文聚合 pom，合计 27。

| 层 | 允许做什么 | 禁止做什么 |
| --- | --- | --- |
| `domain` | 聚合根、实体、值对象、仓储接口、领域异常 | 出现 `org.springframework` / `jakarta.persistence` import |
| `application` | 用例编排、`@Transactional`、命令与查询服务 | 直接写 SQL、直接依赖其他上下文的 domain |
| `infrastructure` | JPA Entity、Repository 适配器、Redis、发件箱 | 把 JPA 注解写回领域对象 |
| `interfaces` | REST Controller、请求/响应 DTO | 写业务规则 |
| `api` | 跨上下文 DTO 与集成事件契约 | 引入实现代码 |

`co-bootstrap` 是唯一启动模块：`CampusOverflowApplication.java` 为入口，其 pom 依赖各上下文模块完成装配；`src/main/resources/` 集中放 `application.yml`、`logback-spring.xml`、`db/migration/platform/`。

### 2.3 构建与架构测试（本机已实跑）

```
cd campus-overflow-student/backend
mvn clean install -DskipITs
mvn test -pl co-bootstrap -Dtest=ArchitectureTest
```

**本机实测（2026-11-25 08:34，日志见工作区 `lab4-build.log` / `lab4-archunit.log`）**：

| 项 | 结果 |
| --- | --- |
| `mvn clean install -DskipITs` | `exit=0`、`BUILD SUCCESS`、Reactor 27 个模块全部 SUCCESS、耗时 03:35 |
| 单元测试 | 18 个测试类 / 91 个用例 / Failures 0 / Errors 0 / Skipped 0 |
| 产物 | `co-bootstrap/target/campus-overflow.jar`（约 79.5 MB）已生成 |
| `mvn test -pl co-bootstrap -Dtest=ArchitectureTest` | `exit=0`、`BUILD SUCCESS`、`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`（该测试类内封装 FF-1~FF-6 六条依赖方向规则） |

> 口径：`CampusOverflowFlowIT` 因依赖 Sprint 3 的声誉结算/搜索投影/通知投递而按决策 D1 临时 `@Disabled`，所以统一用 `-DskipITs`；理由与恢复条件写在 `campus-overflow-student/README.md` 第 9 节，向教师说明的话术在 `docs/lab4/提交物与验收口径.md` 第六节。**不要**删测试或用 `-Dmaven.test.failure.ignore=true` 掩盖失败。

### 2.4 写 ADR（提交物 ③）

新建 `campus-overflow-student/docs/adr/009-包结构分层.md`，按四段骨架写，每段至少 3 条要点：

```
# 009 包结构分层：按限界上下文物理分包

## 背景
- Lab 3 事件风暴识别出身份、问答、声誉、发现四个限界上下文；
- 模块化单体（ADR-001）下约束不能靠进程隔离，只能靠目录与构建期检查；
- 需要让「跨上下文只能经 api / 集成事件」变成编译期可见的事实。

## 决策
- 每个上下文一个顶层 Maven 模块，内部固定 domain / application / infrastructure / interfaces / api；
- domain 零框架依赖；仓储接口在 domain、实现在 infrastructure（ADR-008）；
- 用 ArchUnit FF-1~FF-6 把依赖方向写成构建期门禁。

## 后果
- 优点：边界清晰、可独立测试、违反分层在构建期失败；
- 代价：模块数 27，初次构建约 3.5 分钟，跨上下文调用要显式走 api；
- 证据：模块树、`ArchitectureTest`、本 ADR。

## 被否决的替代方案
- 按技术分层（controller/service/dao 大包）：业务边界被技术边界淹没，否决；
- 直接上微服务：10 周课程里分布式事务与运维成本不划算，见 ADR-001，否决；
- 领域对象与 JPA Entity 共用同一个类：聚合被持久化注解污染，见 ADR-008，否决。
```

最后在 `docs/adr/README.md` 追加一行：`[009](009-包结构分层.md) | 包结构与四层分层 | 已接受`，并注明「PDF 的 ADR-002-package-structure 对应本仓库 009，因 002 已用于《事务性发件箱》」。

**本步截图清单**：`lab4-step1-01-模块树.png`、`lab4-step1-02-build-success.png`、`lab4-step1-03-archunit.png`、`lab4-step1-04-adr009.png`。

---

## 3. Step 2 · 核心聚合代码实现（PDF 35 分钟）

### 3.1 要动的文件（完整路径）

```
co-qa/co-qa-domain/src/main/java/com/campusoverflow/qa/domain/Question.java        // 聚合根
co-qa/co-qa-domain/src/main/java/com/campusoverflow/qa/domain/Answer.java          // 实体
co-qa/co-qa-domain/src/main/java/com/campusoverflow/qa/domain/Vote.java            // 值对象
co-qa/co-qa-domain/src/main/java/com/campusoverflow/qa/domain/Title.java
co-qa/co-qa-domain/src/main/java/com/campusoverflow/qa/domain/MarkdownBody.java
co-qa/co-qa-domain/src/main/java/com/campusoverflow/qa/domain/Tags.java
co-qa/co-qa-domain/src/main/java/com/campusoverflow/qa/domain/QuestionRepository.java   // 接口
co-qa/co-qa-infrastructure/src/main/java/com/campusoverflow/qa/infrastructure/persistence/QuestionRepositoryAdapter.java  // 实现
co-qa/co-qa-domain/src/test/java/com/campusoverflow/qa/domain/QuestionTest.java
```

### 3.2 三条硬要求（对应评分项「避免贫血模型」25%）

1. **构造私有化**：`private Question(...)`，对外只暴露 `Question.post(...)` 工厂方法与 `Question.reconstitute(...)` 还原方法；
2. **无 public setter**：状态变更只能走 `edit()` / `accept()` / `softDelete()` 等聚合方法；
3. **不变式写在聚合内部**，例如现有 `accept()`：

```java
public void accept(Answer answer, Actor actor, boolean actorManagesCourse) {
    if (!actorManagesCourse && !actor.is(this.authorId)) {
        throw new ForbiddenException("QA-1008", "只有提问者或课程管理者可以采纳回答");
    }
    ensureNotDeleted();
    if (this.acceptedAnswerId != null) {
        throw new ConflictException("QA-1008", "每个问题只能采纳一个回答");
    }
    if (answer.questionId() != this.id) {
        throw new BusinessRuleException("QA-1008", "该回答不属于此问题");
    }
    this.acceptedAnswerId = answer.id();
}
```

`score / answerCount / viewCount` 是反规范化计数器，由仓储用原子 SQL 维护，**不要**在聚合里改。

### 3.3 验证命令

```
mvn test -pl co-qa/co-qa-domain -Dtest=QuestionTest,AnswerTest,VoteAndCommentTest
mvn test                        # 全量，期望 91 个用例 0 失败
```

不要修改已有测试的断言；新用例只能增加。把 Lab 3 定稿的 42 条不变式逐条映射到测试方法，更新 `docs/lab4/不变式-测试覆盖对照表.md`，未覆盖项写明取舍理由（T-039）。

**本步截图清单**：`lab4-step2-01-question-post.png`、`lab4-step2-02-单测全绿.png`、`lab4-step2-03-不变式对照表.png`。

---

## 4. Step 3 · 基础设施层接入（PDF 35 分钟，T-031 主战场）

### 4.1 核对 `application.yml`

打开 `co-bootstrap/src/main/resources/application.yml`，逐项确认（当前本地版本已把默认 profile 改为 `demo`、凭据改为根 compose 对应的值）：

```yaml
spring:
  profiles:
    default: demo                       # 会执行 DemoDataSeeder，生成演示账号与课程
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:campusoverflow}?...
    username: ${DB_USER:co_user}
    password: ${DB_PASSWORD:co_pass}
  jpa:
    hibernate:
      ddl-auto: none                    # 表结构只由 Flyway 管理
  flyway:
    enabled: true
    locations: classpath:db/migration/platform,classpath:db/migration/identity,
              classpath:db/migration/qa,classpath:db/migration/reputation,
              classpath:db/migration/discovery
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
```

### 4.2 三套 compose 不要混用

| 文件 | 库名 | 账号 | 密码 | 是否含 Redis |
| --- | --- | --- | --- | --- |
| 仓库根 `docker-compose.yml`（**推荐**） | `campusoverflow` | `co_user` | `co_pass` | 是（本轮已补） |
| `backend/docker-compose.yml` | `campus_overflow` | `campus` | `campus_dev_pwd` | 是 |
| `deploy/docker-compose.yml` | `campus_overflow` | `campus` | `campus_dev_pwd` | 是（还含 app/web） |

若坚持用后两套，启动时必须显式覆盖环境变量（PowerShell 写法）：

```
$env:DB_NAME="campus_overflow"; $env:DB_USER="campus"; $env:DB_PASSWORD="campus_dev_pwd"
$env:DB_HOST="localhost"; $env:REDIS_HOST="localhost"
```

否则会报 `Access denied for user 'co_user'` 或 `Unknown database 'campusoverflow'`。

### 4.3 启动应用

```
cd campus-overflow-student/backend
mvn clean install -DskipITs                       # 先构建，产出 jar
java -jar co-bootstrap/target/campus-overflow.jar              # 默认 demo profile
# 或： mvn spring-boot:run -pl co-bootstrap -Dspring-boot.run.profiles=dev   # 不跑 seeder
```

两条路线的差别：

- `demo`（当前默认）：`DemoDataSeeder` 创建 6 个账号（学生 `2023001`/`2023002`/`2023003`，教师 `T2001`，助教 `TA3001`，管理员 `admin01`），密码统一 `Campus2026`，并创建课程 `CS2001`（自增 id 通常为 1）、3 个问题。**Postman 集合已按此账号修好**。
- `dev`：不写演示数据，需要先 `POST /api/v1/auth/register` 自建账号（请求体见 5.3 备选步骤）。

健康检查：

```
curl http://localhost:8080/actuator/health        → {"status":"UP"}
curl -I http://localhost:8080/swagger-ui.html     → 200
```

### 4.4 Flyway 与表结构校验（T-031 的验收动作）

五个迁移脚本的位置：

```
co-bootstrap/src/main/resources/db/migration/platform/V202609010001__platform_init.sql
co-identity/co-identity-infrastructure/src/main/resources/db/migration/identity/V202609010100__identity_init.sql
co-qa/co-qa-infrastructure/src/main/resources/db/migration/qa/V202609010200__qa_init.sql
co-reputation/co-reputation-infrastructure/src/main/resources/db/migration/reputation/V202609010300__reputation_init.sql
co-discovery/co-discovery-infrastructure/src/main/resources/db/migration/discovery/V202609010400__discovery_init.sql
```

应用启动后执行：

```
docker exec -it co-mysql mysql -uco_user -pco_pass campusoverflow -e "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank; SHOW TABLES; DESC qa_question;"
```

期望：`flyway_schema_history` 有 5 行且 `success=1`；表清单里有 `qa_question`、`qa_answer`、`qa_question_tag`、`id_user`、`id_course`、`outbox_event` 等；`qa_question` 含 `title`、`body`、`author_id`、`course_id`、`deleted_at`、`version` 字段。

> **与 PDF 的口径差异（报告和答辩必须说明）**：PDF 写的是单一 `V1__init_schema.sql` 和 `question` / `answer` / `user` 表；本仓库按限界上下文拆成 5 个版本脚本、表名加 `qa_` / `id_` 前缀，语义一一对应，目的是禁止跨上下文 JOIN / 外键（见 `identity` 脚本首行注释）。不是「表名写错」，是 ADR 决策的结果。

### 4.5 统一异常与 JSON 日志（各一条验证）

```
# 参数校验失败 → 统一错误体 + 400（未登录时先按 5.3 登录）
curl -i -X POST http://localhost:8080/api/v1/questions -H "Content-Type: application/json" -d "{\"title\":\"\",\"body\":\"x\",\"tags\":[\"a\"]}"
# 期望 400，响应为统一 Problem 结构（含 code/message）

# JSON 日志（可复现步骤见 docs/lab4/JSON日志验证记录.md 第六节）
java -jar co-bootstrap/target/campus-overflow.jar --spring.profiles.active=local
tail -n 3 logs/campus-overflow.json
# 每行一个 JSON，字段含 @timestamp / level / logger / message / app / env / traceId
```

`GlobalExceptionHandler.java` 用 `@RestControllerAdvice` 把参数校验、未登录、无权限、领域异常映射为 400/401/403/409；`TraceIdFilter` 把 `traceId` 写入 MDC 并回写响应头 `X-Request-Id`。

**本步截图清单**：`lab4-step3-01-health.png`、`lab4-step3-02-flyway-5-success.png`、`lab4-step3-03-tables.png`、`lab4-step3-04-json-log.png`、`lab4-step3-05-统一错误体.png`。

---

## 5. Step 4 · 首个端到端接口贯通（PDF 15 分钟，T-034 / T-035）

### 5.1 调用链（答辩要能逐层讲出来）

```
POST /api/v1/questions
 → co-qa-interfaces/.../QuestionController.post(Actor, QuestionRequest)   // 校验 @NotBlank/@Size
 → co-qa-application/.../QuestionCommandService.post(...)                 // @Transactional
 → co-qa-domain/.../Question.post(...)                                    // 工厂方法 + 不变式
 → co-qa-domain/.../QuestionRepository.save(...)                          // 接口
 → co-qa-infrastructure/.../QuestionRepositoryAdapter                     // JPA 落库
 → qa_question / qa_question_tag / qa_tag + outbox_event(QuestionPostedEvent)
 ← 201 Created  {"id": N}
```

### 5.2 打开 Postman 并导入集合

`File → Import → campus-overflow-student/docs/campus-overflow.postman_collection.json`，确认集合变量 `baseUrl = http://localhost:8080`。集合共 16 个请求，按顺序只用前 4 个即可完成 Lab 4。

### 5.3 逐步执行（每一步都给出完整输入与期望）

**第 1 步 · 获取 CSRF Token**

```
GET {{baseUrl}}/api/v1/auth/csrf
```

期望 200，响应 `{"headerName":"X-XSRF-TOKEN","token":"..."}`；集合的 test 脚本会把 token 写入变量 `xsrfToken`，服务端同时写入 `XSRF-TOKEN` Cookie。**不要跳过这一步**，login/register 都受 CSRF 保护。

**第 2 步 · 登录（集合已修好账号）**

```
POST {{baseUrl}}/api/v1/auth/login
Header: X-XSRF-TOKEN: {{xsrfToken}}
        Content-Type: application/json
Body:   {"username":"2023001","password":"Campus2026"}
```

期望 200，返回当前用户（`MeView`）；Postman 自动保存 `SESSION` Cookie。若报 401，先确认应用是用 `demo` profile 启动的（seeder 才会建账号）。

*备选：用 `dev` profile 时先注册自己的账号*

```
POST {{baseUrl}}/api/v1/auth/register
Header: X-XSRF-TOKEN: {{xsrfToken}}, Content-Type: application/json
Body:   {"username":"2024117204","displayName":"何心怡","email":"he@campus.edu.cn",
         "password":"Campus2026","college":"计算机工程学院","className":"计科2412"}
```

注册账号状态为 `ACTIVE`，可直接登录；然后回到第 2 步换成自己的账号。

**第 3 步 · 发布问题（提交物 ④ 的成功用例）**

```
POST {{baseUrl}}/api/v1/questions
Header: X-XSRF-TOKEN: {{xsrfToken}}
        Content-Type: application/json
Body:   {"courseId":1,
         "title":"红黑树插入后为什么必须旋转？",
         "body":"仅靠变色为什么无法恢复平衡？希望有一个直观的解释，最好能给出一个反例。",
         "tags":["数据结构","红黑树"]}
```

约束：`title` 非空且 ≤150 字、`body` 非空且 ≤30000 字、`tags` 1—5 个。期望 **201 Created**，响应 `{"id":N}`；集合 test 脚本会断言状态码 201、id 为数字，并把 id 写入变量 `questionId`。`courseId` 允许为 `null`（`post()` 不校验课程存在，只有编辑/删除才查课程权限）；集合里已由占位的 100 改为 1，若你的库里课程 id 不是 1，改集合变量即可。

**第 4 步 · 列表回读**

```
GET {{baseUrl}}/api/v1/questions?page=1&size=20
```

期望 200，列表中出现刚发布的问题。

**第 5 步 · 数据库落库校验（T-034 的硬证据）**

```
docker exec -it co-mysql mysql -uco_user -pco_pass campusoverflow -e "SELECT id,title,author_id,course_id,created_at FROM qa_question ORDER BY id DESC LIMIT 1; SELECT * FROM qa_question_tag WHERE question_id=N; SELECT event_type,attempts,last_error FROM outbox_event ORDER BY id DESC LIMIT 3;"
```

把 `N` 换成第 3 步返回的 id。期望：问题记录的标题/作者/课程正确；`qa_question_tag` 有对应标签行；`outbox_event` 有 `QuestionPostedEvent`。

> **预期现象，不要当成 bug**：`OutboxRelay` 每 500ms 把事件分发给 `NotificationProjector` 等 `@EventListener`，这些投影是 Sprint 3 的 `TODO(S3)`，会抛异常并把 `last_error` 写回 `outbox_event`。它是异步的，**不影响本次 201 响应和落库**，答辩时说明「S3 前通知投影未实现，事件可靠留在发件箱里待重投」即可。

**第 6 步 · 留存证据（T-035）**

Postman 右侧 `... → Export` 导出 Collection；`Runner` 运行「认证 Auth + 问题 Question」两组的 4 个请求并导出 Run report。把以下文件放进 `docs/lab4/`：`lab4-step4-01-postman-201.png`、`lab4-step4-02-run-report.png`、`lab4-step4-03-db-record.png`、`lab4-step4-04-flyway-history.png`。

### 5.4 不用 Postman 的等价 curl 序列（便于脚本化留证）

```
curl -s -c cookies.txt http://localhost:8080/api/v1/auth/csrf > csrf.json
# 从 csrf.json 取 token 赋给 TOKEN（PowerShell: $t=(Get-Content csrf.json|ConvertFrom-Json).token）
curl -s -b cookies.txt -c cookies.txt -H "X-XSRF-TOKEN: $TOKEN" -H "Content-Type: application/json" -d "{\"username\":\"2023001\",\"password\":\"Campus2026\"}" http://localhost:8080/api/v1/auth/login
curl -i -b cookies.txt -H "X-XSRF-TOKEN: $TOKEN" -H "Content-Type: application/json" -d "{\"courseId\":1,\"title\":\"Why must a red-black tree rotate after insertion?\",\"body\":\"Explain with a counterexample.\",\"tags\":[\"data-structure\"]}" http://localhost:8080/api/v1/questions
```

最后一条应返回 `HTTP/1.1 201` 与 `{"id":N}`。

---

## 6. 提交物逐条验收清单（实验结束前打勾）

- [ ] ① `mvn clean install -DskipITs` → BUILD SUCCESS、27 模块；`mvn test` → 91 用例 0 失败；`mvn test -pl co-bootstrap -Dtest=ArchitectureTest` → 6 条规则通过；`README.md` 第 9 节写明 `-DskipITs` 口径与 IT 恢复条件。
- [ ] ② `docker compose up -d` 两个容器 healthy；应用 `UP`；`flyway_schema_history` 5 条 `success=1`；`qa_question`/`qa_answer`/`id_user` 存在且字段与聚合一致。
- [ ] ③ `009-包结构分层.md` 存在且含「被否决的替代方案」；`docs/adr/README.md` 登记 002→009 映射、链接无 404。
- [ ] ④ Postman 集合可导入即用；「发布问题（成功用例）」返回 201 且含 id；数据库能查到该问题；截图 + run report 已入 `docs/lab4/`。
- [ ] Logback 输出单行 JSON 且含 `traceId`；`docs/lab4/JSON日志验证记录.md` 可复现。
- [ ] `docs/lab4/不变式-测试覆盖对照表.md` 已更新；`sprint-2-backlog.md` 中 T-031/T-034/T-035 状态改为 Done。
- [ ] 所有改动经 `feature/lab4-hexinyi` 分支 → PR → 队友 LGTM → SM 合并。

---

## 7. 可直接写进实验报告的「实验步骤」正文

> 本实验按「结构核对—领域实现—基础设施接入—端到端贯通」四步完成。首先，在 IntelliJ IDEA 中打开 `campus-overflow-student/backend` 的 Maven 多模块工程，核对父 POM 中 Spring Boot 3.4.5、JDK 21 与 JUnit 5 的统一版本管理，确认工程按身份、问答、声誉、发现四个限界上下文物理分包，每个上下文内部划分 domain、application、infrastructure、interfaces 与 api 五层，共 27 个模块；随后执行 `mvn clean install -DskipITs` 完成构建，结果显示 27 个模块全部构建成功、18 个测试类共 91 个用例零失败，并通过 `ArchitectureTest` 的 6 条依赖方向约束。
>
> 第二步实现问答上下文的核心聚合：在 `co-qa-domain` 中编写 `Question` 聚合根、`Answer` 实体及 `Vote`、`Title`、`MarkdownBody`、`Tags` 值对象，将构造函数私有化、不提供 public setter，通过 `Question.post()` 工厂方法与 `edit()`、`accept()`、`softDelete()` 等聚合方法守护不变式；`QuestionRepository` 接口定义在 domain 层，JPA 适配实现放在 infrastructure 层，从而保持领域对象不被持久化框架污染。运行 `mvn test -pl co-qa/co-qa-domain` 验证聚合行为，并把 Lab 3 的 42 条不变式逐条映射到测试方法。
>
> 第三步接入基础设施：启动 Docker Compose 中的 MySQL 8 与 Redis 7，核对 `application.yml` 的数据源、Redis 会话、Flyway 五个迁移目录与 `ddl-auto=none` 配置；以 `demo` profile 启动应用后访问 `/actuator/health` 得到 `UP`，查询 `flyway_schema_history` 确认 platform、identity、qa、reputation、discovery 五条迁移全部成功，并检查 `qa_question`、`qa_answer`、`id_user` 等表结构；同时验证 `@RestControllerAdvice` 统一错误响应与 Logback 单行 JSON 日志（含 traceId）。
>
> 最后贯通首个端到端接口：在 Postman 中依次执行「获取 CSRF Token → 登录 → 发布问题 → 问题列表」，`POST /api/v1/questions` 经 `QuestionController` → `QuestionCommandService` → `Question` 聚合 → `QuestionRepository` 落库，返回 201 Created 与问题 id；再在 MySQL 中查询 `qa_question` 与 `qa_question_tag` 确认数据写入，导出 Collection 与 Run report 作为验收证据。实验中遇到的主要问题是三套 compose 的库名与凭据不一致、以及 Postman 集合中的演示账号为占位值，已分别通过统一使用根目录 compose 并补 Redis、把登录账号改为演示账号 `2023001/Campus2026` 解决。

---

## 8. 一键验证脚本（Docker 启动后运行）

`docs/lab4/lab4-verify.ps1` 会自动完成：检查 Docker 守护进程 → `docker compose up -d` → 等待 MySQL/Redis healthy → `mvn clean install -DskipITs` → 查询 Flyway 版本记录与表清单 → 把结果写入 `docs/lab4/lab4-verify-report.md`。运行方式：

```
powershell -ExecutionPolicy Bypass -File docs\lab4\lab4-verify.ps1
```

脚本只做「起容器 + 构建 + 用例统计」这一半，接口部分仍按第 5 节用 Postman 手工跑并截图；脚本会把下一步要执行的命令打印到 `lab4-verify-report.md`。若报 `Docker Desktop is not running`，先启动 Docker Desktop 再重跑。本机当前 Docker 守护进程未启动，因此该脚本**尚未实跑验证**，若执行报错，按第 4、5 节逐条手工执行即可，步骤本身已逐条写明。

---

## 9. 常见坑与排查（按错误信息定位）

| 错误信息 / 现象 | 原因 | 处理 |
| --- | --- | --- |
| `failed to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine` | Docker Desktop 未启动 | 启动客户端等托盘变绿，再 `docker info` |
| `Access denied for user 'co_user'` / `Unknown database 'campusoverflow'` | 用了 `backend/` 或 `deploy/` 的 compose | 统一用根目录 compose，或按 4.2 覆盖 5 个环境变量 |
| 登录成功但马上又变未登录 / `SESSION` 不生效 | 会话存 Redis（ADR-003），缺 Redis 容器 | 根 compose 已补 `co-redis`，`docker compose up -d` 后确认 healthy |
| 登录 401 `BadCredentialsException` | Postman 里还是占位 `demo/demo12345` | 已改为 `2023001/Campus2026`；`dev` profile 下需先 `/api/v1/auth/register` |
| `403 AUTH-403-CSRF` | 写操作缺 `X-XSRF-TOKEN` 或没先取 token | 先 `GET /api/v1/auth/csrf`，头带 `{{xsrfToken}}` |
| `AuthenticationCredentialsNotFoundException: 请先登录` | `Actor` 参数要求登录，`permitAll` 不解决 | 按 5.3 顺序登录；建议撤回 `SecurityConfig` 的两行调试改动 |
| 表不存在 / 字段对不上 | `ddl-auto=none`，只由 Flyway 建表 | 查 5 个 migration 目录是否都在 `spring.flyway.locations`；表名带 `qa_`/`id_` 前缀 |
| 启动即退出，日志停在数据源初始化 | MySQL/Redis 未就绪 | `docker ps` 确认 healthy 后再启动；参见 `JSON日志验证记录.md` 第五节 |
| `mvn clean install` 被集成测试拖红 | `CampusOverflowFlowIT` 依赖 S3 | 用 `-DskipITs`，不要删测试或放宽断言 |
| 日志里出现 `TODO(S3)` / `outbox_event.last_error` 非空 | 通知与搜索投影属 Sprint 3 | 异步中继失败，不影响 201 与落库；答辩说明即可 |
| ADR 链接 404 / 文件名乱码 | 编号顺延与历史乱码 | 按 `提交物与验收口径.md` 第三节映射核对 |

---

## 10. 本轮已经替你做的事与尚未做的事

**已完成**：

1. 仓库根 `docker-compose.yml` 增加 `redis:7-alpine` 服务（`co-redis`，6379，带 healthcheck）——修掉「登录会话无处可存」的阻塞点。
2. `campus-overflow-student/docs/campus-overflow.postman_collection.json`：登录账号 `demo/demo12345` → `2023001/Campus2026`；3 处 `courseId: 100` / `"value": "100"` → `1`。
3. 新增 `docs/lab4/Lab4-详细操作步骤.md`（本文件）与 `docs/lab4/lab4-verify.ps1`。
4. 本机实跑两条构建命令：`mvn clean install -DskipITs` → `BUILD SUCCESS`、27 模块、18 测试类 / 91 用例 / 0 失败、jar 已产出（79.5 MB）；`mvn test -pl co-bootstrap -Dtest=ArchitectureTest` → `BUILD SUCCESS`、`Tests run: 1, Failures: 0`。日志存于工作区 `.workbuddy/pdf-read/lab4-build.log` 与 `lab4-archunit.log`。
5. 生成同内容的 Word 版《Lab4-详细操作步骤（逐条可执行版）.docx》便于提交。

**需要你动手**（Docker 未启动，本机无法代跑）：

- 启动 Docker Desktop → 运行 `lab4-verify.ps1` → 按第 5 节跑 Postman 并截图；
- 写 `009-包结构分层.md`（T-036 是江思达的任务，若由你代写需和 PO 打个招呼）；
- 把 T-031/T-034/T-035 在 `sprint-2-backlog.md` 里改为 Done，走 PR 合并。

**未改动**：`SecurityConfig.java`、`application.yml`（保留你的本地决策，仅在文档中给出建议）。
