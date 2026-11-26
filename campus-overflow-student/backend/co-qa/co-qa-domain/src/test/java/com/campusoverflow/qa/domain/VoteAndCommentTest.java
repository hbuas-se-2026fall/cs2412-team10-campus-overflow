package com.campusoverflow.qa.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.shared.domain.BusinessRuleException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class VoteAndCommentTest {

    private static final Instant NOW = Instant.parse("2026-10-01T08:00:00Z");

    @Test
    void cannotVoteOnOwnContent() {
        assertThatThrownBy(() -> Vote.cast(3, 3, TargetType.ANSWER, 50, 1, NOW))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("自己");
    }

    @Test
    void voteValueMustBePlusOrMinusOne() {
        assertThatThrownBy(() -> Vote.cast(1, 3, TargetType.ANSWER, 50, 2, NOW))
                .isInstanceOf(BusinessRuleException.class);
        Vote v = Vote.cast(1, 3, TargetType.ANSWER, 50, 1, NOW);
        assertThat(v.change(-1, NOW)).isEqualTo(1);
        assertThat(v.value()).isEqualTo(-1);
    }

    @Test
    void commentBodyMustBeBetweenOneAndOneThousandChars() {
        assertThatThrownBy(() -> Comment.post(TargetType.ANSWER, 50, 10, 1, null, "", NOW))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("评论内容");
        assertThatThrownBy(() -> Comment.post(TargetType.ANSWER, 50, 10, 1, null, "字".repeat(1001), NOW))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("评论内容");
        assertThat(Comment.post(TargetType.ANSWER, 50, 10, 1, null, "赞", NOW).body()).isEqualTo("赞");
        assertThat(Comment.post(TargetType.ANSWER, 50, 10, 1, null, "字".repeat(1000), NOW).body()).hasSize(1000);
    }

    @Test
    void repliesAreLimitedToOneLevelOnSameTarget() {
        Comment top = Comment.post(TargetType.ANSWER, 50, 10, 1, null, "请问第二步为什么要变色？", NOW);
        top.assignId(1);
        Comment reply = Comment.post(TargetType.ANSWER, 50, 10, 3, top, "因为要保持黑高一致。", NOW);
        reply.assignId(2);
        assertThat(reply.parentId()).isEqualTo(1L);

        assertThatThrownBy(() -> Comment.post(TargetType.ANSWER, 50, 10, 1, reply, "再回复一层", NOW))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> Comment.post(TargetType.QUESTION, 10, 10, 1, top, "挂错位置的回复", NOW))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void reconstitutedVoteExposesFieldsAndRejectsDuplicateId() {
        Vote v = Vote.reconstitute(7L, 1, TargetType.ANSWER, 50, -1, NOW.plusSeconds(15));

        assertThat(v.id()).isEqualTo(7L);
        assertThat(v.voterId()).isEqualTo(1);
        assertThat(v.targetType()).isEqualTo(TargetType.ANSWER);
        assertThat(v.targetId()).isEqualTo(50);
        assertThat(v.updatedAt()).isEqualTo(NOW.plusSeconds(15));
        v.assignId(8L);
        assertThat(v.id()).isEqualTo(8L);
    }

    @Test
    void reconstitutedCommentExposesFieldsAndRejectsDuplicateId() {
        Comment c = Comment.reconstitute(4L, TargetType.QUESTION, 10, 10, 3, null, "还原回来的评论内容",
                NOW.plusSeconds(25));

        assertThat(c.questionId()).isEqualTo(10);
        assertThat(c.authorId()).isEqualTo(3);
        assertThat(c.createdAt()).isEqualTo(NOW.plusSeconds(25));
        assertThatThrownBy(() -> c.assignId(5L)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ID 已分配");
    }
}
