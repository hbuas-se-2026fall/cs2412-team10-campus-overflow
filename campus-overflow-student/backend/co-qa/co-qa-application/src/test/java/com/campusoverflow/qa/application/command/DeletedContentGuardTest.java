package com.campusoverflow.qa.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.qa.application.fake.QaTestFixtures.CountingCache;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryAnswers;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryComments;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryQuestions;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingAudit;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingEvents;
import com.campusoverflow.qa.application.fake.QaTestFixtures.StubIdentity;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.Comment;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.TargetType;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.DomainException;
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
 * 软删除守卫：内容被软删除后，其上游动作（回答、评论、采纳、编辑、删除）必须被一致地拒绝，
 * 避免「已下线的帖子仍能继续产生互动与声誉」这类跨聚合不一致。
 */
class DeletedContentGuardTest {

    private static final Instant NOW = Instant.parse("2026-11-21T02:00:00Z");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final InMemoryQuestions questions = new InMemoryQuestions();
    private final InMemoryAnswers answers = new InMemoryAnswers();
    private final InMemoryComments comments = new InMemoryComments();
    private final RecordingEvents events = new RecordingEvents();
    private final CountingCache listCache = new CountingCache();
    private final RecordingAudit audit = new RecordingAudit();
    private final StubIdentity identity = new StubIdentity()
            .withUser(1L, "小柯", Role.STUDENT)
            .withUser(2L, "小周", Role.STUDENT)
            .withUser(9L, "李老师", Role.TEACHER)
            .withCourseManager(9L);

    private final Actor asker = new Actor(1, "小柯", Role.STUDENT);
    private final Actor answerer = new Actor(2, "小周", Role.STUDENT);
    private final Actor teacher = new Actor(9, "李老师", Role.TEACHER);

    private AnswerCommandService answerService;
    private CommentService commentService;
    private long questionId;
    private long answerId;

    @BeforeEach
    void setUp() {
        answerService = new AnswerCommandService(questions, answers, identity, events, listCache, audit, clock);
        commentService = new CommentService(comments, questions, answers, events, clock);
        Question question = questions.save(Question.post(asker.userId(), 100L, new Title("进程与线程在调度上的区别到底是什么？"),
                MarkdownBody.ofQuestion("操作系统课上讲的调度单位到底指谁？"),
                Tags.of(List.of("操作系统")), NOW));
        questionId = question.id();
        Answer answer = answers.save(Answer.submit(question, answerer.userId(),
                MarkdownBody.ofAnswer("进程是资源分配单位，线程是调度单位。"), NOW));
        answerId = answer.id();
    }

    @Test
    @DisplayName("已软删除的问题：不再接受新的回答")
    void deletedQuestionRejectsNewAnswer() {
        question().softDelete(asker, false, NOW);

        assertThatThrownBy(() -> answerService.submit(answerer, questionId, "补充：线程共享地址空间。"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).code()).isEqualTo("QA-1009"));
    }

    @Test
    @DisplayName("已软删除的问题：不再接受新的评论")
    void deletedQuestionRejectsNewComment() {
        question().softDelete(asker, false, NOW);

        assertThatThrownBy(() -> commentService.post(teacher,
                new PostCommentCommand(TargetType.QUESTION, questionId, null, "课程组已知悉该问题。")))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).code()).isEqualTo("QA-1009"));
        assertThat(comments.size()).isZero();
    }

    @Test
    @DisplayName("已软删除的问题：不能再被采纳答案")
    void deletedQuestionRejectsAccept() {
        question().softDelete(asker, false, NOW);

        assertThatThrownBy(() -> answerService.accept(asker, questionId, answerId))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).code()).isEqualTo("QA-1009"));
        assertThat(question().acceptedAnswerId()).isNull();
    }

    @Test
    @DisplayName("已软删除的回答：不能再被编辑")
    void deletedAnswerRejectsEdit() {
        answer().softDelete(answerer, false, false, NOW);

        assertThatThrownBy(() -> answerService.edit(answerer, answerId, "改写后的内容。"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).code()).isEqualTo("QA-2004"));
    }

    @Test
    @DisplayName("已软删除的回答：不能再被采纳")
    void deletedAnswerCannotBeAccepted() {
        answer().softDelete(answerer, false, false, NOW);

        assertThatThrownBy(() -> answerService.accept(asker, questionId, answerId))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).code()).isEqualTo("QA-1008"));
    }

    @Test
    @DisplayName("问题已有回答时：提问者不能删除，课程管理者可以删除")
    void onlyManagerCanDeleteQuestionWithAnswers() {
        Question withAnswers = Question.reconstitute(questionId, asker.userId(), 100L,
                new Title("进程与线程在调度上的区别到底是什么？"),
                MarkdownBody.ofQuestion("操作系统课上讲的调度单位到底指谁？"),
                Tags.of(List.of("操作系统")), null, 0, 1, 0, NOW, NOW, null, 0);

        assertThatThrownBy(() -> withAnswers.softDelete(asker, false, NOW))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).code()).isEqualTo("QA-1009"));
        assertThat(withAnswers.isDeleted()).isFalse();

        withAnswers.softDelete(teacher, true, NOW);

        assertThat(withAnswers.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("删除回答：计数器与列表缓存同步变化，并留下审计与事件")
    void deletingAnswerKeepsCountersAndCacheInSync() {
        answerService.delete(answerer, answerId);

        assertThat(answers.findById(answerId).orElseThrow().isDeleted()).isTrue();
        assertThat(questions.answerCountDeltas.get(questionId)).isEqualTo(-1);
        assertThat(listCache.invalidations).isEqualTo(1);
        assertThat(audit.actions).containsExactly("ANSWER_DELETED");
        assertThat(events.published).hasSize(1);
    }

    @Test
    @DisplayName("已软删除的回答：二级回复仍可指向其父评论，但主评论不可指向已删除问题")
    void commentOnDeletedQuestionLeavesNoResidue() {
        long parentId = commentService.post(teacher,
                new PostCommentCommand(TargetType.QUESTION, questionId, null, "请补充教材版本。"));

        question().softDelete(teacher, true, NOW);

        assertThat(comments.findById(parentId)).isPresent();
        assertThatThrownBy(() -> commentService.post(answerer,
                new PostCommentCommand(TargetType.QUESTION, questionId, parentId, "第 3 版。")))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(comments.size()).isEqualTo(1);
    }

    private Question question() {
        return questions.findById(questionId).orElseThrow();
    }

    private Answer answer() {
        return answers.findById(answerId).orElseThrow();
    }
}
