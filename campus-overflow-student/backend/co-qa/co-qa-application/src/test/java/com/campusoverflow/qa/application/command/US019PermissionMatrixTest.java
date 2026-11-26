package com.campusoverflow.qa.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerEndorsedEvent;
import com.campusoverflow.qa.application.fake.QaTestFixtures.CountingCache;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryAnswers;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryQuestions;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingAudit;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingEvents;
import com.campusoverflow.qa.application.fake.QaTestFixtures.StubIdentity;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.DomainException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * US-019 权限矩阵验收：采纳、教师认证、删除三类动作在“提问者 / 回答者 / 普通学生 / 课程管理者”
 * 四种身份下的放行与拒绝边界，以及动作成功后的缓存失效与审计留痕。
 */
class US019PermissionMatrixTest {

    private static final Instant NOW = Instant.parse("2026-11-20T04:00:00Z");
    private static final long COURSE_ID = 100L;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final InMemoryQuestions questions = new InMemoryQuestions();
    private final InMemoryAnswers answers = new InMemoryAnswers();
    private final RecordingEvents events = new RecordingEvents();
    private final CountingCache listCache = new CountingCache();
    private final RecordingAudit audit = new RecordingAudit();
    private final StubIdentity identity = new StubIdentity();

    private final Actor asker = new Actor(1, "小柯", Role.STUDENT);
    private final Actor answerer = new Actor(2, "小周", Role.STUDENT);
    private final Actor stranger = new Actor(3, "小吴", Role.STUDENT);
    private final Actor teacher = new Actor(9, "李老师", Role.TEACHER);
    private final Actor assistant = new Actor(10, "助教小王", Role.TA);

    private AnswerCommandService service;
    private long questionId;
    private long answerId;

    @BeforeEach
    void setUp() {
        identity.withUser(1L, "小柯", Role.STUDENT)
                .withUser(2L, "小周", Role.STUDENT)
                .withUser(3L, "小吴", Role.STUDENT)
                .withUser(9L, "李老师", Role.TEACHER)
                .withUser(10L, "助教小王", Role.TA)
                .withCourseManager(9L)
                .withCourseManager(10L);
        service = new AnswerCommandService(questions, answers, identity, events, listCache, audit, clock);

        Question question = questions.save(Question.post(1L, COURSE_ID, new Title("动态规划的状态转移怎么推？"),
                MarkdownBody.ofQuestion("背包问题的一维写法为什么需要倒序枚举容量？"),
                Tags.of(List.of("算法")), NOW));
        questionId = question.id();
        Answer answer = answers.save(Answer.submit(question, 2L,
                MarkdownBody.ofAnswer("倒序是为了保证每件物品只被放入一次。"), NOW));
        answerId = answer.id();
    }

    @Test
    @DisplayName("权限矩阵 R-2：提问者可以采纳自己问题下的回答")
    void askerCanAccept() {
        service.accept(asker, questionId, answerId);

        Question stored = questions.findById(questionId).orElseThrow();
        assertThat(stored.acceptedAnswerId()).isEqualTo(answerId);
    }

    @Test
    @DisplayName("权限矩阵 R-2：既非提问者也非课程管理者的学生采纳被拒绝")
    void strangerCannotAccept() {
        assertThatThrownBy(() -> service.accept(stranger, questionId, answerId))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).code()).isEqualTo("QA-1008"));
    }

    @Test
    @DisplayName("权限矩阵 R-2：课程管理者可以代提问者采纳")
    void courseManagerCanAccept() {
        service.accept(teacher, questionId, answerId);

        assertThat(questions.findById(questionId).orElseThrow().acceptedAnswerId()).isEqualTo(answerId);
    }

    @Test
    @DisplayName("权限矩阵 T-3：教师与助教都可以为回答加认证")
    void teacherAndAssistantCanEndorse() {
        service.endorse(teacher, answerId);
        assertThat(answers.findById(answerId).orElseThrow().endorsedBy()).isEqualTo(9L);

        long secondAnswerId = service.submit(asker, questionId, "还可以用滚动数组再省一维。");
        service.endorse(assistant, secondAnswerId);
        assertThat(answers.findById(secondAnswerId).orElseThrow().endorsedBy()).isEqualTo(10L);
    }

    @Test
    @DisplayName("权限矩阵 T-3：普通学生认证回答被拒绝")
    void studentCannotEndorse() {
        assertThatThrownBy(() -> service.endorse(stranger, answerId))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).code()).isEqualTo("QA-2003"));
    }

    @Test
    @DisplayName("权限矩阵 T-3：已被认证的回答不能重复认证")
    void endorseIsNotRepeatable() {
        service.endorse(teacher, answerId);

        assertThatThrownBy(() -> service.endorse(assistant, answerId))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).code()).isEqualTo("QA-2003"));
    }

    @Test
    @DisplayName("权限矩阵 D-4：被采纳的回答不能由作者本人删除")
    void authorCannotDeleteAcceptedAnswer() {
        service.accept(asker, questionId, answerId);

        assertThatThrownBy(() -> service.delete(answerer, answerId))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).code()).isEqualTo("QA-2004"));
        assertThat(answers.findById(answerId).orElseThrow().isDeleted()).isFalse();
    }

    @Test
    @DisplayName("权限矩阵 D-4：课程管理者可以删除已被采纳的回答")
    void courseManagerCanDeleteAcceptedAnswer() {
        service.accept(asker, questionId, answerId);

        service.delete(teacher, answerId);

        assertThat(answers.findById(answerId).orElseThrow().isDeleted()).isTrue();
    }

    @Test
    @DisplayName("缓存一致性：采纳与认证都会让问题列表缓存整体失效")
    void stateChangingActionsInvalidateListCache() {
        service.accept(asker, questionId, answerId);
        service.endorse(teacher, answerId);

        assertThat(listCache.invalidations).isEqualTo(2);
    }

    @Test
    @DisplayName("审计契约：采纳与认证分别留下 ANSWER_ACCEPTED、ANSWER_ENDORSED 记录")
    void actionsAreAudited() {
        service.accept(asker, questionId, answerId);
        service.endorse(teacher, answerId);

        assertThat(audit.actions).containsExactly("ANSWER_ACCEPTED", "ANSWER_ENDORSED");
    }

    @Test
    @DisplayName("事件契约：采纳与认证发布的事件携带回答作者与问题归属")
    void actionsPublishEvents() {
        service.accept(asker, questionId, answerId);
        service.endorse(teacher, answerId);

        AnswerAcceptedEvent accepted = events.ofType(AnswerAcceptedEvent.class).get(0);
        assertThat(accepted.answerId()).isEqualTo(answerId);
        assertThat(accepted.questionId()).isEqualTo(questionId);
        assertThat(accepted.answerAuthorId()).isEqualTo(2L);
        assertThat(accepted.acceptedById()).isEqualTo(1L);

        AnswerEndorsedEvent endorsed = events.ofType(AnswerEndorsedEvent.class).get(0);
        assertThat(endorsed.answerId()).isEqualTo(answerId);
        assertThat(endorsed.endorserId()).isEqualTo(9L);
    }
}
