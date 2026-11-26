package com.campusoverflow.qa.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.campusoverflow.qa.api.event.AnswerEndorsedEvent;
import com.campusoverflow.qa.api.event.CommentPostedEvent;
import com.campusoverflow.qa.api.event.ContentDeletedEvent;
import com.campusoverflow.qa.api.event.QuestionEditedEvent;
import com.campusoverflow.qa.api.event.QuestionPostedEvent;
import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.qa.application.fake.QaTestFixtures.CountingCache;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryAnswers;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryComments;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryQuestions;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryVotes;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingAudit;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingEvents;
import com.campusoverflow.qa.application.fake.QaTestFixtures.StubIdentity;
import com.campusoverflow.qa.application.fake.QaTestFixtures.StubRenderer;
import com.campusoverflow.qa.application.fake.QaTestFixtures.StubTags;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.TargetType;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.DomainException;
import com.campusoverflow.shared.domain.ForbiddenException;
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

/**
 * 应用服务补测：覆盖问题 / 回答 / 评论命令服务此前未触达的成功路径与异常分支，
 * 仅验证编排（仓储加载、权限判定入口、事件、缓存、审计），领域规则由 domain 单测覆盖。
 */
class CommandServiceCoverageTest {

    private static final Instant NOW = Instant.parse("2026-10-26T06:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final InMemoryQuestions questions = new InMemoryQuestions();
    private final InMemoryAnswers answers = new InMemoryAnswers();
    private final InMemoryComments comments = new InMemoryComments();
    private final InMemoryVotes votes = new InMemoryVotes();
    private final StubTags tags = new StubTags();
    private final RecordingEvents events = new RecordingEvents();
    private final CountingCache cache = new CountingCache();
    private final RecordingAudit audit = new RecordingAudit();
    private final StubRenderer renderer = new StubRenderer();
    private final StubIdentity identity = new StubIdentity()
            .withUser(1, "小明", Role.STUDENT).withUser(2, "小红", Role.STUDENT)
            .withUser(9, "王老师", Role.TEACHER).withCourseManager(9);

    private final Actor ming = new Actor(1, "小明", Role.STUDENT);
    private final Actor hong = new Actor(2, "小红", Role.STUDENT);
    private final Actor teacher = new Actor(9, "王老师", Role.TEACHER);

    private QuestionCommandService questionService;
    private AnswerCommandService answerService;
    private CommentService commentService;
    private VoteService voteService;
    private long questionId;

    @BeforeEach
    void setUp() {
        questionService = new QuestionCommandService(questions, tags, identity, events, renderer, cache, audit, clock);
        answerService = new AnswerCommandService(questions, answers, identity, events, cache, audit, clock);
        commentService = new CommentService(comments, questions, answers, events, clock);
        voteService = new VoteService(votes, questions, answers, events, clock);

        Question q = questions.save(Question.post(ming.userId(), 100L,
                new Title("红黑树插入后为什么必须进行旋转？"),
                MarkdownBody.ofQuestion("变色为什么不够？希望有一个直观的解释。"),
                Tags.of(List.of("数据结构", "红黑树")), NOW));
        questionId = q.id();
    }

    // ---------- QuestionCommandService ----------

    @Test
    @DisplayName("发布问题：落库、标签计数、事件、缓存失效与审计全链路")
    void postQuestionPersistsAndPublishes() {
        long id = questionService.post(ming, new PostQuestionCommand(100L,
                "新增问题标题用于覆盖发布链路场景", "这是问题正文内容，用于覆盖问题发布的完整编排流程。",
                List.of("java", "集合")));

        assertThat(id).isPositive();
        assertThat(tags.usage).containsEntry("java", 1).containsEntry("集合", 1);
        assertThat(events.ofType(QuestionPostedEvent.class)).hasSize(1);
        assertThat(cache.invalidations).isEqualTo(1);
        assertThat(audit.actions).contains("QUESTION_POSTED");
    }

    @Test
    @DisplayName("作者编辑自己的问题：发布编辑事件并记录审计")
    void editQuestionByAuthor() {
        questionService.edit(ming, questionId, new EditQuestionCommand(
                "更新后的问题标题内容较长一些", "更新后的问题正文内容，同样需要满足长度要求。", List.of("树")));

        Question stored = questions.findById(questionId).orElseThrow();
        assertThat(stored.title().value()).isEqualTo("更新后的问题标题内容较长一些");
        assertThat(events.ofType(QuestionEditedEvent.class)).hasSize(1);
        assertThat(audit.actions).contains("QUESTION_EDITED");
    }

    @Test
    @DisplayName("编辑不存在的问题：抛出问题不存在异常")
    void editMissingQuestionThrowsNotFound() {
        DomainException ex = catchThrowableOfType(() -> questionService.edit(ming, 999L,
                new EditQuestionCommand("标题内容较长一些用于校验", "正文内容较长一些用于通过校验。", List.of("树"))),
                NotFoundException.class);
        assertThat(ex.code()).isEqualTo("QA-1000");
    }

    @Test
    @DisplayName("陌生人编辑他人问题：权限拒绝")
    void editQuestionByStrangerForbidden() {
        assertThatThrownBy(() -> questionService.edit(hong, questionId, new EditQuestionCommand(
                "篡改后的问题标题内容较长一些", "篡改后的问题正文内容，长度同样满足要求。", List.of("树"))))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("作者删除自己无回答的问题：发布删除事件并失效缓存")
    void deleteQuestionByAuthor() {
        questionService.delete(ming, questionId);

        Question stored = questions.findById(questionId).orElseThrow();
        assertThat(stored.isDeleted()).isTrue();
        assertThat(events.ofType(ContentDeletedEvent.class)).isNotEmpty();
        assertThat(cache.invalidations).isEqualTo(1);
        assertThat(audit.actions).contains("QUESTION_DELETED");
    }

    @Test
    @DisplayName("删除不存在的问题：抛出问题不存在异常")
    void deleteMissingQuestionThrowsNotFound() {
        DomainException ex = catchThrowableOfType(() -> questionService.delete(ming, 999L), NotFoundException.class);
        assertThat(ex.code()).isEqualTo("QA-1000");
    }

    @Test
    @DisplayName("提问者本人不能删除已有回答的问题")
    void deleteQuestionWithAnswersByAuthorBlocked() {
        Question answered = Question.reconstitute(50L, ming.userId(), 100L,
                new Title("已有回答的问题标题内容较长一些"), MarkdownBody.ofQuestion("已有一条回答的问题正文内容用于校验。"),
                Tags.of(List.of("树")), null, 0, 1, 0, NOW, NOW, null, 0L);
        questions.save(answered);

        DomainException ex = catchThrowableOfType(() -> questionService.delete(ming, answered.id()),
                BusinessRuleException.class);
        assertThat(ex.code()).isEqualTo("QA-1009");
    }

    @Test
    @DisplayName("课程管理者可以删除已有回答的问题")
    void deleteQuestionWithAnswersByManagerAllowed() {
        Question answered = Question.reconstitute(51L, ming.userId(), 100L,
                new Title("管理者可删除的问题标题内容较长一些"), MarkdownBody.ofQuestion("已有一条回答的问题正文内容用于校验。"),
                Tags.of(List.of("树")), null, 0, 1, 0, NOW, NOW, null, 0L);
        questions.save(answered);

        questionService.delete(teacher, answered.id());
        assertThat(questions.findById(answered.id()).orElseThrow().isDeleted()).isTrue();
    }

    // ---------- AnswerCommandService ----------

    @Test
    @DisplayName("作者编辑自己的回答：保存并记录审计")
    void editAnswerByAuthor() {
        long answerId = answerService.submit(hong, questionId, "关键在于黑高这个不变量，仅靠变色无法调整。");

        answerService.edit(hong, answerId, "补充：叔叔节点为黑色时才需要进行旋转处理。");

        assertThat(answers.findById(answerId).orElseThrow().body().value()).contains("叔叔节点");
        assertThat(audit.actions).contains("ANSWER_EDITED");
    }

    @Test
    @DisplayName("编辑不存在的回答：抛出回答不存在异常")
    void editMissingAnswerThrowsNotFound() {
        DomainException ex = catchThrowableOfType(() -> answerService.edit(hong, 999L, "回答正文内容足够长。"),
                NotFoundException.class);
        assertThat(ex.code()).isEqualTo("QA-2000");
    }

    @Test
    @DisplayName("陌生人编辑他人回答：权限拒绝")
    void editAnswerByStrangerForbidden() {
        long answerId = answerService.submit(hong, questionId, "关键在于黑高这个不变量，仅靠变色无法调整。");

        assertThatThrownBy(() -> answerService.edit(ming, answerId, "篡改后的回答正文内容足够长。"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("作者删除自己的回答：回答计数回退并发布删除事件")
    void deleteAnswerByAuthor() {
        long answerId = answerService.submit(hong, questionId, "关键在于黑高这个不变量，仅靠变色无法调整。");

        answerService.delete(hong, answerId);

        assertThat(questions.answerCountDeltas).containsEntry(questionId, 0); // +1 后 -1
        assertThat(events.ofType(ContentDeletedEvent.class)).isNotEmpty();
        assertThat(audit.actions).contains("ANSWER_DELETED");
    }

    @Test
    @DisplayName("删除不存在的回答：抛出回答不存在异常")
    void deleteMissingAnswerThrowsNotFound() {
        DomainException ex = catchThrowableOfType(() -> answerService.delete(hong, 999L), NotFoundException.class);
        assertThat(ex.code()).isEqualTo("QA-2000");
    }

    @Test
    @DisplayName("已被采纳的回答不能由作者自行删除")
    void deleteAcceptedAnswerByAuthorBlocked() {
        long answerId = answerService.submit(hong, questionId, "关键在于黑高这个不变量，仅靠变色无法调整。");
        answerService.accept(ming, questionId, answerId);

        DomainException ex = catchThrowableOfType(() -> answerService.delete(hong, answerId),
                BusinessRuleException.class);
        assertThat(ex.code()).isEqualTo("QA-2004");
    }

    @Test
    @DisplayName("管理者认证回答：发布认证事件并记录审计")
    void endorseAnswerByManager() {
        long answerId = answerService.submit(hong, questionId, "关键在于黑高这个不变量，仅靠变色无法调整。");

        answerService.endorse(teacher, answerId);

        assertThat(events.ofType(AnswerEndorsedEvent.class)).hasSize(1);
        assertThat(audit.actions).contains("ANSWER_ENDORSED");
    }

    @Test
    @DisplayName("非管理者不能认证回答：权限拒绝")
    void endorseAnswerByNonManagerForbidden() {
        long answerId = answerService.submit(hong, questionId, "关键在于黑高这个不变量，仅靠变色无法调整。");

        assertThatThrownBy(() -> answerService.endorse(hong, answerId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("同一回答不能被重复认证：冲突异常")
    void endorseAnswerTwiceConflict() {
        long answerId = answerService.submit(hong, questionId, "关键在于黑高这个不变量，仅靠变色无法调整。");
        answerService.endorse(teacher, answerId);

        DomainException ex = catchThrowableOfType(() -> answerService.endorse(teacher, answerId),
                ConflictException.class);
        assertThat(ex.code()).isEqualTo("QA-2003");
    }

    // ---------- CommentService ----------

    @Test
    @DisplayName("在问题下发表评论：关联到问题并发布评论事件")
    void postCommentOnQuestion() {
        long id = commentService.post(ming, new PostCommentCommand(TargetType.QUESTION, questionId, null, "赞同这个思路。"));

        var comment = comments.findById(id).orElseThrow();
        assertThat(comment.questionId()).isEqualTo(questionId);
        assertThat(events.ofType(CommentPostedEvent.class)).hasSize(1);
    }

    @Test
    @DisplayName("在回答下发表评论：归属到回答所在问题")
    void postCommentOnAnswer() {
        long answerId = answerService.submit(hong, questionId, "关键在于黑高这个不变量，仅靠变色无法调整。");

        long id = commentService.post(ming, new PostCommentCommand(TargetType.ANSWER, answerId, null, "追问第二步为什么要变色。"));

        var comment = comments.findById(id).orElseThrow();
        assertThat(comment.questionId()).isEqualTo(questionId);
        assertThat(comment.targetType()).isEqualTo(TargetType.ANSWER);
    }

    @Test
    @DisplayName("回复不存在的父评论：抛出父评论不存在异常")
    void postCommentWithMissingParentThrowsNotFound() {
        DomainException ex = catchThrowableOfType(() -> commentService.post(ming,
                new PostCommentCommand(TargetType.QUESTION, questionId, 999L, "回复一条不存在的评论内容。")),
                NotFoundException.class);
        assertThat(ex.code()).isEqualTo("QA-1007");
    }

    @Test
    @DisplayName("评论挂到不存在的问题：抛出问题不存在异常")
    void postCommentOnMissingQuestionThrowsNotFound() {
        DomainException ex = catchThrowableOfType(() -> commentService.post(ming,
                new PostCommentCommand(TargetType.QUESTION, 999L, null, "挂在不存在问题下的评论内容。")),
                NotFoundException.class);
        assertThat(ex.code()).isEqualTo("QA-1000");
    }

    @Test
    @DisplayName("评论挂到不存在的回答：抛出回答不存在异常")
    void postCommentOnMissingAnswerThrowsNotFound() {
        DomainException ex = catchThrowableOfType(() -> commentService.post(ming,
                new PostCommentCommand(TargetType.ANSWER, 999L, null, "挂在不存在回答下的评论内容。")),
                NotFoundException.class);
        assertThat(ex.code()).isEqualTo("QA-2000");
    }

    // ---------- VoteService 补充分支 ----------

    @Test
    @DisplayName("给不存在的问题投票：问题不存在异常")
    void voteMissingQuestionThrowsNotFound() {
        DomainException ex = catchThrowableOfType(() -> voteService.cast(ming, TargetType.QUESTION, 999L, 1),
                NotFoundException.class);
        assertThat(ex.code()).isEqualTo("QA-1000");
    }

    @Test
    @DisplayName("给不存在的回答投票：回答不存在异常")
    void voteMissingAnswerThrowsNotFound() {
        DomainException ex = catchThrowableOfType(() -> voteService.cast(ming, TargetType.ANSWER, 999L, 1),
                NotFoundException.class);
        assertThat(ex.code()).isEqualTo("QA-2000");
    }

    @Test
    @DisplayName("投票值超出 -1/0/1：业务规则异常")
    void voteValueOutOfRangeRejected() {
        DomainException ex = catchThrowableOfType(() -> voteService.cast(hong, TargetType.QUESTION, questionId, 2),
                BusinessRuleException.class);
        assertThat(ex.code()).isEqualTo("QA-1006");
    }

    @Test
    @DisplayName("撤销投票（value=0）：删除原记录并按旧值回退分数")
    void revokeVoteRemovesRecord() {
        voteService.cast(hong, TargetType.QUESTION, questionId, 1);
        VoteService.VoteResult revoked = voteService.cast(hong, TargetType.QUESTION, questionId, 0);

        assertThat(revoked.myVote()).isZero();
        assertThat(revoked.scoreDelta()).isEqualTo(-1);
        assertThat(votes.size()).isZero();
        assertThat(events.ofType(VoteCastEvent.class)).hasSize(2);
    }
}
