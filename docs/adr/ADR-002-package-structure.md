# ADR-002 包结构分层（课程 PDF 路径入口）

> **本文是路径对齐文档，不是另一份独立决策。** 决策正文见 [`009-包结构分层.md`](../../campus-overflow-student/docs/adr/009-包结构分层.md)。
>
> 《软件工程》Lab 4 提交物③ 要求 `docs/adr/ADR-002-package-structure.md`。本仓库的 002 号已用于「事务性发件箱」，且 ADR 目录随工程位于 `campus-overflow-student/docs/adr/`，故在此保留 PDF 原始路径作为入口，避免教师按 PDF 检索时出现 404。

---

## 一、编号与路径映射

| 课程 PDF 写法 | 本仓库实际位置 | 说明 |
| --- | --- | --- |
| `docs/adr/ADR-002-package-structure.md` | **本文件**（入口） | 指向决策正文 |
| 包结构决策正文 | `campus-overflow-student/docs/adr/009-包结构分层.md` | 002 号被占用，顺延至 009 |
| ADR 索引 | `campus-overflow-student/docs/adr/README.md` | 已登记 009 及本映射 |

## 二、决策摘要（详见正文）

1. **物理分离**：每个限界上下文是独立 Maven 聚合模块，内部再分 `api / domain / application / infrastructure / interfaces` 五层；共 27 个 POM（父 POM + `co-bootstrap` + `co-shared-kernel` + 4 上下文 × 6）。课程要求的四层之外增加 `*-api`，用于 ADR-007 契约优先下的跨上下文契约。
2. **依赖单向**：`interfaces → application → domain`，`infrastructure` 实现 `domain` 定义的仓储接口；`domain` 不依赖 Spring / JPA 等框架。
3. **跨上下文**：只允许依赖 `*-api` 或经集成事件通信（ADR-002 事务性发件箱），不共享事务。
4. **聚合保护不变式**：Setter 私有，状态变更经工厂方法或聚合方法；JPA Entity 与领域对象分离（ADR-008）。
5. **构建期强制**：`ArchitectureTest` 的 FF-1~FF-6 六条规则在 `mvn test` 与 CI 的「架构约束检查」步骤中执行，违反分层直接构建失败。

## 三、被否决的替代方案

按技术层全局分包、单模块 + 包命名隔离、直接拆微服务、领域对象直接加 JPA 注解、跨上下文直接依赖对方 `domain`。逐条否决理由见正文第四节。

## 四、Lab 4 提交物对照

| PDF 提交物 | 状态 | 位置 |
| --- | --- | --- |
| ① 可运行的多模块 Maven 工程 | 已完成（单测基线 91 用例 / 0 失败；集成测试 `CampusOverflowFlowIT` 因依赖 S3 能力 `@Disabled`） | `campus-overflow-student/backend/` |
| ② `application.yml` 启动并连库 | 配置齐全 | `co-bootstrap/src/main/resources/application.yml` |
| ③ ADR 记录分模块决策 | **本文件 + 009 正文已补齐** | 本目录 / `campus-overflow-student/docs/adr/` |
| ④ Postman Collection 含 `POST /api/v1/questions` 成功用例 | 集合已有，运行证据待实跑归档 | `campus-overflow-student/docs/campus-overflow.postman_collection.json` |

---

**登记日期**：2026-11-19　|　**维护人**：PO（唐志烽 / luck970）
