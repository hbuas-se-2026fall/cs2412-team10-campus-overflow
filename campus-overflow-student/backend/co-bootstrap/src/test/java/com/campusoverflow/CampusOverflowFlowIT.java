package com.campusoverflow;

import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 端到端集成测试（质量场景 R-2 的验收）：注册 → 登录 → 提问 → 回答 → 采纳 → 声誉结算 → 搜索命中 → 收到通知。
 * 使用真实的 MySQL 8 与 Redis 7（Testcontainers），因此也验证了 Flyway 迁移、ngram 全文索引与发件箱中继。
 *
 * <p><b>当前状态：仍保持禁用，但阻塞点已定位到通知投递（T-052 ~ T-057），不再是「S3 范围未开工」。</b>
 * Sprint 3 内 SM 已把本类改造为可在无 Docker 的本机实跑（见下「外部服务模式」），并在 2026-11-19 实跑取证：
 * 3 个用例中 1 个通过、2 个失败，根因是 {@code NotificationProjector} 的 5 个 {@code on(...)} 仍为
 * {@code TODO(S3)} 并抛 {@link UnsupportedOperationException}，导致
 * {@code AnswerSubmittedEvent} / {@code AnswerAcceptedEvent} / {@code BadgeAwardedEvent}
 * 在发件箱中反复重试后仍未投递（{@code outbox_event.last_error = "TODO(S3)：NotificationProjector.on 尚未实现"}），
 * 搜索投影因此拿不到 {@code answer_count} 与 {@code accepted}。证据见
 * {@code docs/lab6/evidence/flow-it-run-2026-11-19.md}。
 *
 * <p><b>两种运行方式：</b>
 * <ol>
 *   <li><b>默认（CI 与有 Docker 的本机）</b>：由 Testcontainers 拉起 mysql:8.0.39（utf8mb4 + ngram=2）与
 *       redis:7-alpine，无需任何外部依赖，命令为 {@code mvn verify -Dit.test=CampusOverflowFlowIT}。</li>
 *   <li><b>外部服务模式（无 Docker 的本机）</b>：加 {@code -Dco.it.external=true}，并给出
 *       {@code -Dco.it.jdbc-url=}、{@code -Dco.it.db-user=}、{@code -Dco.it.db-password=}、
 *       {@code -Dco.it.redis-host=}、{@code -Dco.it.redis-port=}，直接连接已就绪的 MySQL 8（要求
 *       ngram_token_size=2、库为空）与 Redis 7，跳过容器启动。该模式用于无法安装 Docker 的开发机，
 *       判定标准与默认模式完全一致。</li>
 * </ol>
 *
 * <p><b>恢复条件（须同时满足）：</b>
 * <ol>
 *   <li>T-052 ~ T-057 完成通知投递实现，{@code NotificationProjector} 不再抛 {@code TODO(S3)}；</li>
 *   <li>删除本类的 {@code @Disabled}，{@code mvn verify -Dit.test=CampusOverflowFlowIT} 三个用例全绿；</li>
 *   <li>构建口径由 {@code mvn clean install -DskipITs} 改回含集成测试的完整构建，并同步更新
 *       {@code campus-overflow-student/README.md} 与 CI 配置。</li>
 * </ol>
 */
@Disabled("等待 T-052 ~ T-057：NotificationProjector 仍为 TODO(S3) 并抛 UnsupportedOperationException，"
        + "AnswerSubmitted/AnswerAccepted/BadgeAwarded 事件无法投递，搜索投影与通知断言必然失败；"
        + "实证见 docs/lab6/evidence/flow-it-run-2026-11-19.md（T-063）")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.session.SessionAutoConfiguration",
        "co.outbox.poll-interval-ms=200",
        "spring.flyway.clean-disabled=false"
})
class CampusOverflowFlowIT {

    /** 外部服务模式：为 true 时不启动 Testcontainers，改用 -Dco.it.* 指定的 MySQL 与 Redis。 */
    private static final boolean EXTERNAL_MODE = Boolean.getBoolean("co.it.external");

    private static MySQLContainer<?> mysql;

    private static GenericContainer<?> redis;

    static {
        if (!EXTERNAL_MODE) {
            mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.39"))
                    .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_0900_ai_ci",
                            "--ngram-token-size=2");
            mysql.start();
            redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
            redis.start();
        }
    }

    @DynamicPropertySource
    static void datasourceAndRedisProperties(DynamicPropertyRegistry registry) {
        if (EXTERNAL_MODE) {
            registry.add("spring.datasource.url", () -> required("co.it.jdbc-url"));
            registry.add("spring.datasource.username", () -> System.getProperty("co.it.db-user", "root"));
            registry.add("spring.datasource.password", () -> System.getProperty("co.it.db-password", ""));
            registry.add("spring.data.redis.host", () -> System.getProperty("co.it.redis-host", "localhost"));
            registry.add("spring.data.redis.port", () -> System.getProperty("co.it.redis-port", "6379"));
        } else {
            registry.add("spring.datasource.url", mysql::getJdbcUrl);
            registry.add("spring.datasource.username", mysql::getUsername);
            registry.add("spring.datasource.password", mysql::getPassword);
            registry.add("spring.data.redis.host", redis::getHost);
            registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        }
    }

    @AfterAll
    static void stopContainers() {
        if (!EXTERNAL_MODE) {
            if (redis != null) {
                redis.stop();
            }
            if (mysql != null) {
                mysql.stop();
            }
        }
    }

    private static String required(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("缺少系统属性 -D" + key + "（外部服务模式 co.it.external=true 时必须提供）");
        }
        return value;
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Test
    void 提问到采纳的完整链路会异步结算声誉并更新搜索与通知() throws Exception {
        register("2026001", "测试小明", "it-ming@campus.edu.cn");
        long hongId = register("2026002", "测试小红", "it-hong@campus.edu.cn");
        MockHttpSession ming = login("2026001");
        MockHttpSession hong = login("2026002");

        long questionId = idOf(mvc.perform(post("/api/v1/questions").session(ming).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"跳表和红黑树在工程上如何取舍？",
                                 "body":"课程项目里需要一个有序结构，二者的实现复杂度和并发友好程度差别很大，应该怎么选？",
                                 "tags":["数据结构","跳表"]}
                                """))
                .andExpect(status().isCreated()).andReturn());

        long answerId = idOf(mvc.perform(post("/api/v1/questions/" + questionId + "/answers").session(hong).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"并发场景优先跳表：它的插入删除只需局部指针操作，容易做成无锁结构；单线程内存场景红黑树更省内存。"}
                                """))
                .andExpect(status().isCreated()).andReturn());

        mvc.perform(put("/api/v1/questions/" + questionId + "/accepted-answer").session(ming).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answerId\":" + answerId + "}"))
                .andExpect(status().isNoContent());

        // 采纳后：回答者 +15 声誉（经发件箱异步结算）
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                mvc.perform(get("/api/v1/users/" + hongId + "/reputation"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.reputation").value(15))
                        .andExpect(jsonPath("$.badges[?(@.code=='FIRST_ACCEPTED')]").exists()));

        // 搜索投影：中文关键词可命中，且标记为已采纳
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                mvc.perform(get("/api/v1/search/questions").param("q", "跳表"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.items[0].id").value(questionId))
                        .andExpect(jsonPath("$.items[0].accepted").value(true)));

        // 通知中心：回答者收到“被采纳”通知
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                mvc.perform(get("/api/v1/notifications").session(hong))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.items[?(@.type=='ANSWER_ACCEPTED')]").exists()));
    }

    @Test
    void 不能给自己的内容投票() throws Exception {
        register("2026003", "测试小刚", "it-gang@campus.edu.cn");
        MockHttpSession gang = login("2026003");
        long questionId = idOf(mvc.perform(post("/api/v1/questions").session(gang).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"为什么哈希表的负载因子一般取 0.75？",
                                 "body":"JDK HashMap 默认负载因子是 0.75，这个取值是怎么权衡空间与冲突概率得到的？",
                                 "tags":["数据结构"]}
                                """))
                .andExpect(status().isCreated()).andReturn());

        mvc.perform(put("/api/v1/votes").session(gang).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"QUESTION\",\"targetId\":" + questionId + ",\"value\":1}"))
                .andExpect(status().isUnprocessableEntity())
                // 不变式 I6-2「不能给自己的内容投票」的正式错误码是 QA-1006（docs/ddd/aggregates.md 第 154-155 行、
                // docs/lab4/不变式-测试覆盖对照表.md 第 109 行），本处原写 QA-1015 与规范不符，2026-11-19 实跑后按规范修正。
                .andExpect(jsonPath("$.code").value("QA-1006"));
    }

    @Test
    void 未登录不能提问且缺少CSRF令牌会被拒绝() throws Exception {
        mvc.perform(post("/api/v1/questions").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"未登录的提问测试标题\",\"body\":\"这是一段足够长的正文用于通过参数校验。\",\"tags\":[\"测试\"]}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"2026001\",\"password\":\"Campus2026\"}"))
                .andExpect(status().isForbidden());
    }

    private long register(String username, String displayName, String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RegisterPayload(username, displayName, email,
                                "Campus2026", "计算机工程学院", "计科2301"))))
                .andExpect(status().isCreated()).andReturn();
        return idOf(result);
    }

    private MockHttpSession login(String username) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"Campus2026\"}"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private long idOf(MvcResult result) throws Exception {
        JsonNode node = json.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private record RegisterPayload(String username, String displayName, String email, String password,
                                   String college, String className) {
    }
}
