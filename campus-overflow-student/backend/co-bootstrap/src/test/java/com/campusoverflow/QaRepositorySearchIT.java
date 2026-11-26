package com.campusoverflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.discovery.domain.search.SearchDocument;
import com.campusoverflow.discovery.domain.search.SearchHit;
import com.campusoverflow.discovery.domain.search.SearchIndex;
import com.campusoverflow.discovery.domain.search.SearchPort;
import com.campusoverflow.discovery.domain.search.SearchQuery;
import com.campusoverflow.discovery.domain.search.SearchSort;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * T-079 仓储层集成测试：在真实 MySQL 8.4 容器中验证 Q&A 聚合的 JPA 映射存取往返，
 * 以及 ngram 全文检索对中文关键词的命中。
 *
 * <p>使用 Testcontainers 拉起 {@code mysql:8.4}，Flyway 自动建表；本机无 Docker 时
 * {@code disabledWithoutDocker = true} 会跳过本类，不会让构建失败。
 *
 * <pre>
 * mvn verify -Dit.test=QaRepositorySearchIT
 * </pre>
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.session.SessionAutoConfiguration",
        "spring.flyway.clean-disabled=false"
})
class QaRepositorySearchIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("campus_overflow_it")
            .withUsername("root")
            .withPassword("");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private QuestionRepository questions;

    @Autowired
    private AnswerRepository answers;

    @Autowired
    private SearchIndex searchIndex;

    @Autowired
    private SearchPort searchPort;

    private static final Instant NOW = Instant.parse("2026-10-26T06:00:00Z");
    private final Actor ming = new Actor(1, "it-ming", Role.STUDENT);
    private final Actor hong = new Actor(2, "it-hong", Role.STUDENT);

    @Test
    void questionAndAnswerPersistThroughJpaAndAreSearchableByNgram() {
        // 1. 问题聚合落库后按 JPA 映射往返读取，校验标题 / 正文 / 标签 / 作者 / 课程。
        Question question = Question.post(ming.userId(), 100L,
                new Title("红黑树插入后为什么必须进行旋转？"),
                MarkdownBody.ofQuestion("变色为什么不够？关键在于黑高这个不变量需要通过旋转来恢复。"),
                Tags.of(List.of("数据结构", "红黑树")), NOW);
        Question savedQuestion = questions.save(question);

        Question reloaded = questions.findById(savedQuestion.id()).orElseThrow();
        assertThat(reloaded.title().value()).isEqualTo("红黑树插入后为什么必须进行旋转？");
        assertThat(reloaded.body().value()).contains("黑高");
        assertThat(reloaded.tags().names()).containsExactlyInAnyOrder("数据结构", "红黑树");
        assertThat(reloaded.authorId()).isEqualTo(ming.userId());
        assertThat(reloaded.courseId()).isEqualTo(100L);

        // 2. 回答聚合落库后往返读取，校验与问题的关联及作者映射。
        Answer answer = Answer.submit(reloaded, hong.userId(),
                MarkdownBody.ofAnswer("并发场景优先跳表，单线程内存场景红黑树更省内存。"), NOW);
        Answer savedAnswer = answers.save(answer);

        Answer reloadedAnswer = answers.findById(savedAnswer.id()).orElseThrow();
        assertThat(reloadedAnswer.questionId()).isEqualTo(reloaded.id());
        assertThat(reloadedAnswer.authorId()).isEqualTo(hong.userId());
        assertThat(reloadedAnswer.body().value()).contains("跳表");

        // 3. 写入搜索投影，再用中文关键词经 ngram 全文检索命中该问题。
        searchIndex.insert(new SearchDocument(reloaded.id(), 100L, ming.userId(),
                reloaded.title().value(), reloaded.body().value(), reloaded.tags().names(), NOW));

        PageResult<SearchHit> result = searchPort.search(new SearchQuery(
                "红黑树", 100L, null, SearchSort.RELEVANCE, false, false, new PageRequest(1, 20)));

        assertThat(result.items()).extracting(SearchHit::questionId).contains(reloaded.id());
    }
}
