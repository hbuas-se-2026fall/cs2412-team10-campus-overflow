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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 端到端集成测试（质量场景 R-2 的验收）：注册 → 登录 → 提问 → 回答 → 采纳 → 声誉结算 → 搜索命中 → 收到通知。
 * 使用真实的 MySQL 8 与 Redis，因此也验证了 Flyway 迁移、ngram 全文索引与发件箱中继。
 *
 * <p><b>Sprint 3（T-063）已解除 Sprint 2 的临时禁用（原 D1 / T-038 / A-08）。</b>
 * 容器装配由 Testcontainers 改为连接本机原生 MySQL / Redis：本机无 Docker 时用例同样可执行，
 * 且被测代码与验收断言未做任何放宽。连接参数可用 {@code -D} 覆盖，默认值见常量：
 * <pre>
 * mvn verify -Dit.test=CampusOverflowFlowIT
 *     -Dco.it.jdbc-url=jdbc:mysql://127.0.0.1:3307/campus_overflow_it
 *     -Dco.it.db-user=root -Dco.it.db-password=
 *     -Dco.it.redis-host=127.0.0.1 -Dco.it.redis-port=6379
 * </pre>
 * 目标库需为专用空库：每次运行先 {@code flyway.clean()} 再 {@code migrate()}（见内部配置类），
 * 以免上一轮遗留的固定测试账号（2026001 等）导致唯一键冲突。
 *
 * <p><b>遗留待办：</b>构建口径由 {@code mvn clean install -DskipITs} 改回含集成测试的完整构建，
 * 并同步更新 {@code campus-overflow-student/README.md}。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.session.SessionAutoConfiguration",
        "co.outbox.poll-interval-ms=200",
        "spring.flyway.clean-disabled=false"
})
class CampusOverflowFlowIT {

    /** 集成测试专用库（本机原生 MySQL，非 Docker 容器）。 */
    private static final String JDBC_URL = System.getProperty("co.it.jdbc-url",
            "jdbc:mysql://127.0.0.1:3307/campus_overflow_it"
                    + "?useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_0900_ai_ci"
                    + "&connectionTimeZone=UTC"
                    + "&forceConnectionTimeZoneToSession=true&rewriteBatchedStatements=true");

    @DynamicPropertySource
    static void externalServices(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> JDBC_URL);
        registry.add("spring.datasource.username", () -> System.getProperty("co.it.db-user", "root"));
        registry.add("spring.datasource.password", () -> System.getProperty("co.it.db-password", ""));
        registry.add("spring.data.redis.host", () -> System.getProperty("co.it.redis-host", "127.0.0.1"));
        registry.add("spring.data.redis.port", () -> System.getProperty("co.it.redis-port", "6379"));
    }

    /** 每轮启动前清空专用库再迁移，保证固定账号可重复注册。 */
    @TestConfiguration
    static class CleanDatabaseConfig {

        @Bean
        FlywayMigrationStrategy cleanMigrateStrategy() {
            return flyway -> {
                flyway.clean();
                flyway.migrate();
            };
        }
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
