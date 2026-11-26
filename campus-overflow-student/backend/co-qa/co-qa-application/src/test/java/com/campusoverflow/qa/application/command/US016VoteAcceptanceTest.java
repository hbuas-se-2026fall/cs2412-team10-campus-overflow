package com.campusoverflow.qa.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryAnswers;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryQuestions;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryVotes;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingEvents;
import com.campusoverflow.qa.domain.Answer;
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

/** US-016 投票验收：首次投票、重复投票、改投、撤销、自赞、非法值、目标不存在。 */
class US016VoteAcceptanceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T02:00:00Z");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final InMemoryQuestions questions = new InMemoryQuestions();
    private final InMemoryAnswers answers = new InMemoryAnswers();
    private final InMemoryVotes votes = new InMemoryVotes();
    private final RecordingEvents events = new RecordingEvents();

    private final Actor author = new Actor(2, "小红", Role.STUDENT);
    private final Actor voter = new Actor(3, "小雷", Role.STUDENT);

    private VoteService service;
    private long questionId;
    private long answerId;

    @BeforeEach
    void setUp() {
        service = new VoteService(votes, questions, answers, events, clock);
        Question question = questions.save(Question.post(1L, 100L, new Title("哈希表的冲突怎么处理？"),
                MarkdownBody.ofQuestion("链地址法和开放寻址各自适合什么场景？"),
                Tags.of(List.of("数据结构")), NOW));
        questionId = question.id();
        Answer answer = answers.save(Answer.submit(question, 2L,
                MarkdownBody.ofAnswer("链地址法适合装载因子偏高、删除频繁的场景。"), NOW));
        answerId = answer.id();
    }

    @Test
    @DisplayName("首次点赞：分数 +1，事件携带旧值 0 与新值 1")
    void firstUpvoteAddsScoreAndPublishesEvent() {
        VoteService.VoteResult result = service.cast(voter, TargetType.ANSWER, answerId, 1);

        assertThat(result.myVote()).isEqualTo(1);
        assertThat(result.scoreDelta()).isEqualTo(1);
        assertThat(votes.size()).isEqualTo(1);
        assertThat(answers.scoreDeltas.get(answerId)).isEqualTo(1);
        List<VoteCastEvent> cast = events.ofType(VoteCastEvent.class);
        assertThat(cast).hasSize(1);
        assertThat(cast.get(0).targetType()).isEqualTo("ANSWER");
        assertThat(cast.get(0).targetId()).isEqualTo(answerId);
        assertThat(cast.get(0).targetAuthorId()).isEqualTo(2L);
        assertThat(cast.get(0).voterId()).isEqualTo(3L);
        assertThat(cast.get(0).questionId()).isEqualTo(questionId);
        assertThat(cast.get(0).oldValue()).isZero();
        assertThat(cast.get(0).newValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("重复投相同票：分数不变，事件不重复发布")
    void repeatedSameVoteIsIdempotent() {
        service.cast(voter, TargetType.ANSWER, answerId, 1);
        VoteService.VoteResult result = service.cast(voter, TargetType.ANSWER, answerId, 1);

        assertThat(result.scoreDelta()).isZero();
        assertThat(votes.size()).isEqualTo(1);
        assertThat(answers.scoreDeltas.get(answerId)).isEqualTo(1);
        assertThat(events.ofType(VoteCastEvent.class)).hasSize(1);
    }

    @Test
    @DisplayName("改投：赞同改反对的分数增量是 -2，事件带旧值 1 与新值 -1")
    void switchFromUpvoteToDownvoteAppliesDeltaOfTwo() {
        service.cast(voter, TargetType.ANSWER, answerId, 1);
        VoteService.VoteResult result = service.cast(voter, TargetType.ANSWER, answerId, -1);

        assertThat(result.myVote()).isEqualTo(-1);
        assertThat(result.scoreDelta()).isEqualTo(-2);
        assertThat(votes.size()).isEqualTo(1);
        assertThat(answers.scoreDeltas.get(answerId)).isEqualTo(-1);
        VoteCastEvent last = events.ofType(VoteCastEvent.class).get(1);
        assertThat(last.oldValue()).isEqualTo(1);
        assertThat(last.newValue()).isEqualTo(-1);
    }

    @Test
    @DisplayName("撤销投票：票据被删除，分数回到原值")
    void withdrawVoteDropsRowAndRestoresScore() {
        service.cast(voter, TargetType.ANSWER, answerId, -1);
        VoteService.VoteResult result = service.cast(voter, TargetType.ANSWER, answerId, 0);

        assertThat(result.myVote()).isZero();
        assertThat(result.scoreDelta()).isEqualTo(1);
        assertThat(votes.size()).isZero();
        assertThat(answers.scoreDeltas.get(answerId)).isZero();
        assertThat(events.ofType(VoteCastEvent.class)).hasSize(2);
    }

    @Test
    @DisplayName("未投票时直接撤销：不写票、不发事件")
    void withdrawWithoutExistingVoteIsNoop() {
        VoteService.VoteResult result = service.cast(voter, TargetType.ANSWER, answerId, 0);

        assertThat(result.scoreDelta()).isZero();
        assertThat(votes.size()).isZero();
        assertThat(events.ofType(VoteCastEvent.class)).isEmpty();
    }

    @Test
    @DisplayName("不能给自己的内容投票")
    void selfVoteRejected() {
        assertThatThrownBy(() -> service.cast(author, TargetType.ANSWER, answerId, 1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("自己");
        assertThat(votes.size()).isZero();
    }

    @Test
    @DisplayName("投票值越界：只接受 -1、0、1")
    void outOfRangeVoteValueRejected() {
        for (int value : new int[] {-2, 2, 5}) {
            assertThatThrownBy(() -> service.cast(voter, TargetType.ANSWER, answerId, value))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("投票值");
        }
        assertThat(votes.size()).isZero();
    }

    @Test
    @DisplayName("目标不存在：问题与回答都抛出 NotFoundException")
    void unknownTargetRejected() {
        assertThatThrownBy(() -> service.cast(voter, TargetType.ANSWER, 999L, 1))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.cast(voter, TargetType.QUESTION, 999L, 1))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("给问题投票：分数记到问题而不是回答")
    void questionVoteAdjustsQuestionScore() {
        VoteService.VoteResult result = service.cast(voter, TargetType.QUESTION, questionId, 1);

        assertThat(result.scoreDelta()).isEqualTo(1);
        assertThat(questions.scoreDeltas.get(questionId)).isEqualTo(1);
        VoteCastEvent event = events.ofType(VoteCastEvent.class).get(0);
        assertThat(event.targetType()).isEqualTo("QUESTION");
        assertThat(event.targetId()).isEqualTo(questionId);
        assertThat(event.questionId()).isEqualTo(questionId);
    }
}
