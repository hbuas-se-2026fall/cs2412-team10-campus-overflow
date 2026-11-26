package com.campusoverflow.qa.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.identity.api.CourseSummary;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.qa.application.port.ContentRenderer;
import com.campusoverflow.qa.application.port.QuestionListCache;
import com.campusoverflow.qa.application.query.QuestionSummaryView;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.TagRepository;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.DomainException;
import com.campusoverflow.shared.event.IntegrationEvent;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * T-058 缺陷回归：两处跨路径不一致。
 * 缺陷一：标签使用计数只在发布时累加，编辑时整体按新集合再加一次、删除时完全不回收，
 *         导致 qa_tag.question_count 随编辑次数持续虚高，热门标签排序失去参考价值。
 * 缺陷二：教师认证只校验课程管理者身份，未校验回答本身是否已被软删除，
 *         已下线的回答仍可被认证并产生声誉与索引事件。
 */
class TagUsageAndEndorseRegressionTest {

    private static final Instant NOW = Instant.parse("2026-11-25T02:00:00Z");
    private static final long COURSE_ID = 100L;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final InMemoryQuestions questions = new InMemoryQuestions();
    private final InMemoryAnswers answers = new InMemoryAnswers();
    private final RecordingTags tags = new RecordingTags();
    private final StubIdentity identity = new StubIdentity();
    private final StubRenderer renderer = new StubRenderer();
    private final NoopCache listCache = new NoopCache();
    private final RecordingAudit audit = new RecordingAudit();
    private final RecordingEvents events = new RecordingEvents();

    private final Actor asker = new Actor(1, "小柯", Role.STUDENT);
    private final Actor answerer = new Actor(2, "小周", Role.STUDENT);
    private final Actor teacher = new Actor(9, "李老师", Role.TEACHER);

    private QuestionCommandService questionService;
    private AnswerCommandService answerService;
    private long questionId;
    private long answerId;

    @BeforeEach
    void setUp() {
        identity.withCourseManager(9L);
        questionService = new QuestionCommandService(questions, tags, identity, events, renderer, listCache, audit, clock);
        answerService = new AnswerCommandService(questions, answers, identity, events, listCache, audit, clock);
        questionId = questionService.post(asker, new PostQuestionCommand(COURSE_ID,
                "动态规划的状态转移怎么推？", "背包问题的一维写法为什么需要倒序枚举容量？", List.of("算法", "图论")));
        answerId = answerService.submit(answerer, questionId, "倒序是为了保证每件物品只被放入一次。");
        tags.clear();
    }

    @Test
    @DisplayName("缺陷一：编辑只新增一个标签时，只为新标签 +1，未变化的标签不再二次累加")
    void editOnlyCountsNewlyAddedTags() {
        questionService.edit(asker, questionId, new EditQuestionCommand("动态规划的状态转移怎么推？",
                "背包问题的一维写法为什么需要倒序枚举容量？", List.of("算法", "图论", "动态规划")));

        assertThat(tags.pairs()).containsExactly("动态规划:+1");
    }

    @Test
    @DisplayName("缺陷一：编辑摘掉标签时，被摘掉的标签计 -1")
    void editReclaimsDroppedTags() {
        questionService.edit(asker, questionId, new EditQuestionCommand("动态规划的状态转移怎么推？",
                "背包问题的一维写法为什么需要倒序枚举容量？", List.of("算法")));

        assertThat(tags.pairs()).containsExactly("图论:-1");
    }

    @Test
    @DisplayName("缺陷一：编辑未改动标签时，不产生任何计数变更")
    void editWithSameTagsTouchesNoCounter() {
        questionService.edit(asker, questionId, new EditQuestionCommand("动态规划的状态转移怎么推？补充说明",
                "背包问题的一维写法为什么需要倒序枚举容量？", List.of("算法", "图论")));

        assertThat(tags.pairs()).isEmpty();
    }

    @Test
    @DisplayName("缺陷一：删除问题后，其占用的标签计数被回收")
    void deletingQuestionReclaimsTagUsage() {
        questionService.delete(teacher, questionId);

        assertThat(tags.pairs()).containsExactlyInAnyOrder("算法:-1", "图论:-1");
    }

    @Test
    @DisplayName("缺陷二：已被软删除的回答不能被教师认证")
    void deletedAnswerCannotBeEndorsed() {
        softDeleteAnswer();

        assertThatThrownBy(() -> answerService.endorse(teacher, answerId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).code()).isEqualTo("QA-2004"));
        assertThat(answers.findById(answerId).orElseThrow().endorsedBy()).isNull();
    }

    @Test
    @DisplayName("缺陷二：未删除的回答仍可正常认证")
    void activeAnswerCanStillBeEndorsed() {
        answerService.endorse(teacher, answerId);

        assertThat(answers.findById(answerId).orElseThrow().endorsedBy()).isEqualTo(9L);
    }

    private void softDeleteAnswer() {
        Answer answer = answers.findById(answerId).orElseThrow();
        answer.softDelete(answerer, false, false, NOW);
        answers.save(answer);
    }

    /** 记录标签计数变更，空集合调用与真实实现一样直接忽略。 */
    static class RecordingTags implements TagRepository {
        private final List<String[]> calls = new ArrayList<>();

        @Override
        public void adjustUsage(Collection<String> tagNames, int delta) {
            if (tagNames == null || tagNames.isEmpty() || delta == 0) {
                return;
            }
            tagNames.forEach(name -> calls.add(new String[] {name, (delta > 0 ? "+" : "") + delta}));
        }

        List<String> pairs() {
            List<String> result = new ArrayList<>();
            calls.forEach(call -> result.add(call[0] + ":" + call[1]));
            return result;
        }

        void clear() {
            calls.clear();
        }
    }

    static class InMemoryQuestions implements QuestionRepository {
        private final Map<Long, Question> store = new LinkedHashMap<>();
        private final AtomicLong sequence = new AtomicLong();

        @Override
        public Optional<Question> findById(long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Question save(Question question) {
            if (question.id() == null) {
                question.assignId(sequence.incrementAndGet());
            }
            store.put(question.id(), question);
            return question;
        }

        @Override
        public void adjustAnswerCount(long questionId, int delta) {
        }

        @Override
        public void adjustScore(long questionId, int delta) {
        }
    }

    static class InMemoryAnswers implements AnswerRepository {
        private final Map<Long, Answer> store = new LinkedHashMap<>();
        private final AtomicLong sequence = new AtomicLong();

        @Override
        public Optional<Answer> findById(long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Answer save(Answer answer) {
            if (answer.id() == null) {
                answer.assignId(sequence.incrementAndGet());
            }
            store.put(answer.id(), answer);
            return answer;
        }

        @Override
        public void adjustScore(long answerId, int delta) {
        }
    }

    static class StubIdentity implements IdentityApi {
        private final Map<Long, UserSummary> users = new HashMap<>();
        private final Map<Long, Boolean> courseManagers = new HashMap<>();

        StubIdentity withUser(long id, String name, Role role) {
            users.put(id, new UserSummary(id, name, role, true, "计算机工程学院"));
            return this;
        }

        StubIdentity withCourseManager(long userId) {
            courseManagers.put(userId, true);
            return this;
        }

        @Override
        public Optional<UserSummary> findUser(long userId) {
            return Optional.ofNullable(users.get(userId));
        }

        @Override
        public Map<Long, UserSummary> findUsers(Collection<Long> userIds) {
            Map<Long, UserSummary> result = new HashMap<>();
            userIds.forEach(id -> findUser(id).ifPresent(user -> result.put(id, user)));
            return result;
        }

        @Override
        public Optional<UserSummary> findByDisplayName(String displayName) {
            return users.values().stream().filter(user -> user.displayName().equals(displayName)).findFirst();
        }

        @Override
        public Optional<CourseSummary> findCourse(long courseId) {
            return Optional.of(new CourseSummary(courseId, "CS2001", "数据结构", "2026-秋", 100L));
        }

        @Override
        public boolean canManageCourse(long userId, Long courseId) {
            return courseManagers.getOrDefault(userId, false);
        }
    }

    static class StubRenderer implements ContentRenderer {
        @Override
        public String toSafeHtml(String markdown) {
            return markdown;
        }

        @Override
        public String toPlainText(String markdown, int maxLength) {
            return markdown.length() <= maxLength ? markdown : markdown.substring(0, maxLength);
        }
    }

    static class NoopCache implements QuestionListCache {
        @Override
        public Optional<PageResult<QuestionSummaryView>> get(Long courseId, PageRequest page) {
            return Optional.empty();
        }

        @Override
        public void put(Long courseId, PageRequest page, PageResult<QuestionSummaryView> value) {
        }

        @Override
        public void invalidateAll() {
        }
    }

    static class RecordingAudit implements AuditTrail {
        private final List<String> actions = new ArrayList<>();

        @Override
        public void record(String action, Long actorId, String targetType, String targetId, String detail) {
            actions.add(action);
        }
    }

    static class RecordingEvents implements IntegrationEventPublisher {
        private final List<IntegrationEvent> published = new ArrayList<>();

        @Override
        public void publish(IntegrationEvent event) {
            published.add(event);
        }
    }
}
