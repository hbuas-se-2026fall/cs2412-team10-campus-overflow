# Logback JSON 日志验证记录

> 对应任务：Lab 4 · T-033（日志规范：JSON 输出 + traceId 贯穿）
> 验证人：柯博（Ke-1226）｜ 验证时间：2026-11-10
> 涉及文件：`campus-overflow-student/backend/co-bootstrap/src/main/resources/logback-spring.xml`、`campus-overflow-student/backend/co-bootstrap/src/main/java/com/campusoverflow/bootstrap/web/TraceIdFilter.java`

---

## 一、验证目标

1. 应用日志以**单行 JSON** 输出，字段可被日志平台（ELK / Loki）直接解析，无需正则。
2. 每条日志带 `app` / `env` 标识，便于多环境混流后区分。
3. 请求入口注入 `traceId`，同一次请求的所有日志共享同一值，并回写到响应头。
4. 请求结束（含异常）后清理 MDC，避免线程复用造成串号。

## 二、验证方法

构建产物 `co-bootstrap/target/campus-overflow.jar` 打包完成后，以 `local` profile 启动，读取 `logs/campus-overflow.json`：

```bash
mvn clean install -DskipITs          # BUILD SUCCESS，18 个测试类 / 91 个用例 / 0 失败
java -jar co-bootstrap/target/campus-overflow.jar --spring.profiles.active=local --server.port=18080
```

启动后 `logs/` 目录自动生成，滚动策略为 `SizeAndTimeBasedRollingPolicy`（单文件 100MB、保留 7 天、总量上限 1GB、归档为 `.json.gz`）。

## 三、实测输出（原文摘录）

```json
{"@timestamp":"2026-11-10T21:59:42.084+08:00","message":"Starting CampusOverflowApplication v0.1.0-SNAPSHOT using Java 21.0.10 with PID 79072 (D:\\co-framework\\campus-overflow-teacher\\campus-overflow-student\\backend\\co-bootstrap\\target\\campus-overflow.jar started by k2090 in D:\\co-framework\\campus-overflow-teacher\\campus-overflow-student\\backend)","logger":"com.campusoverflow.CampusOverflowApplication","thread":"main","level":"INFO","app":"campus-overflow","env":"local"}
{"@timestamp":"2026-11-10T21:59:42.085+08:00","message":"The following 1 profile is active: \"local\"","logger":"com.campusoverflow.CampusOverflowApplication","thread":"main","level":"INFO","app":"campus-overflow","env":"local"}
{"@timestamp":"2026-11-10T21:59:47.695+08:00","message":"Application run failed","logger":"org.springframework.boot.SpringApplication","thread":"main","level":"ERROR","stack_trace":"org.springframework.beans.factory.BeanCreationException: ...","app":"campus-overflow","env":"local"}
```

字段对照：

| 字段 | 来源 | 用途 |
| --- | --- | --- |
| `@timestamp` | LogstashEncoder 默认 | 带时区偏移的 ISO-8601 时间，跨机器排序不用再猜时区 |
| `level` / `logger` / `thread` | LogstashEncoder 默认 | 定位问题所属模块与线程 |
| `message` | 日志正文 | 业务可读信息 |
| `stack_trace` | LogstashEncoder 默认 | 异常堆栈折叠为单个字符串字段，保持「一行一条日志」 |
| `app` / `env` | `customFields` | 多应用、多环境日志汇聚后仍可拆分 |
| `traceId` / `userId` | `includeMdcKeyName` | 同一请求的日志可串成一条链路 |

## 四、traceId 链路

`TraceIdFilter` 以 `Ordered.HIGHEST_PRECEDENCE` 注册，位于所有业务过滤器之前：

1. 读取请求头 `X-Request-Id`，命中 `[A-Za-z0-9_-]{1,64}` 则原样透传（上游网关已生成时保持链路连续）；否则生成 32 位无横杠 UUID。
2. 写入 `MDC("traceId")`，`logback-spring.xml` 通过 `includeMdcKeyName` 输出到 JSON 的 `traceId` 字段。
3. 把最终 traceId 回写到响应头 `X-Request-Id`，前端报错截图即可直接定位后端日志。
4. `finally` 中 `MDC.remove`，线程池复用不会串号。

安全约束：拒绝含空白、`<`、`>`、换行等字符的入参，避免外部可控字符串污染日志（日志注入 / 伪造链路）。

单元测试 `co-bootstrap/src/test/java/com/campusoverflow/bootstrap/web/TraceIdFilterTest.java` 覆盖 4 个场景：请求头缺失时生成、合法值透传、非法值丢弃重生成、下游抛异常后 MDC 仍被清理。

## 五、口径说明

本次验证只针对**日志序列化格式与 traceId 注入**。应用在本机启动到数据源初始化阶段即退出，原因是本机没有 Docker 环境、MySQL 与 Redis 未就绪（与日志配置无关）；应用启动成功的验收属「提交物 ② 通过 `application.yml` 即可启动」，见 `提交物与验收口径.md` 第二节与第四节②。

## 六、可复现步骤

```bash
# 1) 构建并产出 jar
cd campus-overflow-student/backend && mvn clean install -DskipITs

# 2) 启动（无数据库时也能产出日志文件，用于校验格式）
java -jar co-bootstrap/target/campus-overflow.jar --spring.profiles.active=local

# 3) 查看 JSON 日志
cat logs/campus-overflow.json | head -n 3
```
