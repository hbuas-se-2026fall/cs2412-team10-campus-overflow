package com.campusoverflow.qa.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.qa.api.event.CommentPostedEvent;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryAnswers;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryComments;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryQuestions;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingEvents;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.Comment;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.TargetType;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** US-017 评论验收：问题/回答评论、二级回复边界、长度校验、目标校验。 */
class US017CommentAcceptanceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T02:30:00Z");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final InMemoryComments comments = new InMemoryComments();
    private final InMemoryQuestions questions = new InMemoryQuestions();
    private final InMemoryAnswers answers = new InMemoryAnswers();
    private final RecordingEvents events = new RecordingEvents();

    private final Actor asker = new Actor(1, "柯博", Role.STUDENT);
    private final Actor author = new Actor(2, "小红", Role.STUDENT);
    private final Actor reader = new Actor(3, "小雷", Role.STUDENT);

    private CommentService service;
    private long questionId;
    private long answerId;

    @BeforeEach
    void setUp() {
        service = new CommentService(comments, questions, answers, events, clock);
        Question question = questions.save(Question.post(1L, 100L, new Title("索引失效的常见原因？"),
                MarkdownBody.ofQuestion("同样的查询在测试库很快，线上却很慢。"),
                Tags.of(List.of("数据库")), NOW));
        questionId = question.id();
        answerId = answers.save(Answer.submit(question, 2L,
                MarkdownBody.ofAnswer("先看执行计划有没有走全表扫描。"), NOW)).id();
    }

    @Test
    @DisplayName("在问题下评论：评论落库且事件携带问题编号")
    void commentOnQuestion() {
        long id = service.post(reader, new PostCommentCommand(TargetType.QUESTION, questionId, null, "补充一下索引统计信息"));

        Comment saved = comments.findById(id).orElseThrow();
        assertThat(saved.targetType()).isEqualTo(TargetType.QUESTION);
        assertThat(saved.targetId()).isEqualTo(questionId);
        assertThat(saved.questionId()).isEqualTo(questionId);
        assertThat(saved.authorId()).isEqualTo(3L);
        assertThat(saved.parentId()).isNull();
        List<CommentPostedEvent> posted = events.ofType(CommentPostedEvent.class);
        assertThat(posted).hasSize(1);
        assertThat(posted.get(0).questionId()).isEqualTo(questionId);
    }

    @Test
    @DisplayName("在回答下评论：问题编号取回答所属的问题")
    void commentOnAnswerInheritsQuestionId() {
        long id = service.post(reader, new PostCommentCommand(TargetType.ANSWER, answerId, null, "执行计划确实全表扫描了"));

        Comment saved = comments.findById(id).orElseThrow();
        assertThat(saved.targetType()).isEqualTo(TargetType.ANSWER);
        assertThat(saved.questionId()).isEqualTo(questionId);
        assertThat(events.ofType(CommentPostedEvent.class).get(0).questionId()).isEqualTo(questionId);
    }

    @Test
    @DisplayName("回复评论：记录父评论编号")
    void replyToCommentRecordsParent() {
        long parent = service.post(reader, new PostCommentCommand(TargetType.QUESTION, questionId, null, "统计信息过期了"));
        long child = service.post(author, new PostCommentCommand(TargetType.QUESTION, questionId, parent, "是的，先 ANALYZE"));

        assertThat(comments.findById(child).orElseThrow().parentId()).isEqualTo(parent);
    }

    @Test
    @DisplayName("只允许一层回复：回复二级评论被拒绝")
    void thirdLevelReplyRejected() {
        long parent = service.post(reader, new PostCommentCommand(TargetType.QUESTION, questionId, null, "统计信息过期了"));
        long child = service.post(author, new PostCommentCommand(TargetType.QUESTION, questionId, parent, "先 ANALYZE"));

        assertThatThrownBy(() -> service.post(reader,
                new PostCommentCommand(TargetType.QUESTION, questionId, child, "再补一句")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("一层");
    }

    @Test
    @DisplayName("回复目标必须与父评论一致")
    void replyToDifferentTargetRejected() {
        long parent = service.post(reader, new PostCommentCommand(TargetType.ANSWER, answerId, null, "同问"));

        assertThatThrownBy(() -> service.post(reader,
                new PostCommentCommand(TargetType.QUESTION, questionId, parent, "跑错地方了")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("父评论");
    }

    @Test
    @DisplayName("内容为空或全空白被拒绝")
    void blankCommentRejected() {
        assertThatThrownBy(() -> service.post(reader, new PostCommentCommand(TargetType.QUESTION, questionId, null, "   ")))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(comments.size()).isZero();
    }

    @Test
    @DisplayName("内容超过 1000 字符被拒绝")
    void tooLongCommentRejected() {
        String tooLong = "字".repeat(1001);

        assertThatThrownBy(() -> service.post(reader, new PostCommentCommand(TargetType.QUESTION, questionId, null, tooLong)))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(comments.size()).isZero();
    }

    @Test
    @DisplayName("评论内容恰为 1000 字符时通过")
    void upperBoundCommentAccepted() {
        String body = "字".repeat(1000);

        long id = service.post(reader, new PostCommentCommand(TargetType.QUESTION, questionId, null, body));

        assertThat(comments.findById(id).orElseThrow().body()).hasSize(1000);
    }

    @Test
    @DisplayName("父评论不存在时抛出 NotFoundException")
    void missingParentRejected() {
        assertThatThrownBy(() -> service.post(reader,
                new PostCommentCommand(TargetType.QUESTION, questionId, 999L, "回复幽灵评论")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("目标不存在时抛出 NotFoundException")
    void missingTargetRejected() {
        assertThatThrownBy(() -> service.post(reader, new PostCommentCommand(TargetType.QUESTION, 999L, null, "无主评论")))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.post(reader, new PostCommentCommand(TargetType.ANSWER, 999L, null, "无主评论")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("已删除的问题不允许继续评论")
    void commentOnDeletedQuestionRejected() {
        Question question = questions.findById(questionId).orElseThrow();
        question.softDelete(asker, false, NOW);
        questions.save(question);

        assertThatThrownBy(() -> service.post(reader, new PostCommentCommand(TargetType.QUESTION, questionId, null, "迟到的评论")))
                .isInstanceOf(BusinessRuleException.class);
    }
}
