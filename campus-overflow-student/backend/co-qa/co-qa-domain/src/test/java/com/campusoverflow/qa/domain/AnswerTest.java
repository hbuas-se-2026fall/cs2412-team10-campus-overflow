package com.campusoverflow.qa.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import org.junit.jupiter.api.Test;

class AnswerTest {

    private final Actor answerer = new Actor(3, "回答者", Role.STUDENT);
    private final Actor teacher = new Actor(9, "王老师", Role.TEACHER);

    @Test
    void onlyCourseManagerCanEndorseAndOnlyOnce() {
        Question q = QuestionTest.persistedQuestion();
        Answer a = QuestionTest.persistedAnswer(q, answerer.userId(), 50);
        assertThatThrownBy(() -> a.endorse(answerer, false)).isInstanceOf(ForbiddenException.class);
        a.endorse(teacher, true);
        assertThat(a.endorsedBy()).isEqualTo(9L);
        assertThatThrownBy(() -> a.endorse(teacher, true)).isInstanceOf(ConflictException.class);
    }

    @Test
    void acceptedAnswerCannotBeDeletedByItsAuthor() {
        Question q = QuestionTest.persistedQuestion();
        Answer a = QuestionTest.persistedAnswer(q, answerer.userId(), 50);
        assertThatThrownBy(() -> a.softDelete(answerer, false, true, QuestionTest.NOW))
                .isInstanceOf(BusinessRuleException.class);
        a.softDelete(teacher, true, true, QuestionTest.NOW);
        assertThat(a.isDeleted()).isTrue();
    }

    @Test
    void strangersCannotEditOrDelete() {
        Question q = QuestionTest.persistedQuestion();
        Answer a = QuestionTest.persistedAnswer(q, answerer.userId(), 50);
        Actor stranger = new Actor(77, "路人", Role.STUDENT);
        assertThatThrownBy(() -> a.edit(stranger, false, MarkdownBody.ofAnswer("这是被路人篡改后的回答内容"), QuestionTest.NOW))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> a.softDelete(stranger, false, false, QuestionTest.NOW))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void authorCanEditOwnAnswer() {
        Question q = QuestionTest.persistedQuestion();
        Answer a = QuestionTest.persistedAnswer(q, answerer.userId(), 50);

        a.edit(answerer, false, MarkdownBody.ofAnswer("补充：叔叔节点为黑色时才需要旋转。"), QuestionTest.NOW.plusSeconds(60));

        assertThat(a.body().value()).contains("叔叔节点");
        assertThat(a.updatedAt()).isAfter(a.createdAt());
    }

    @Test
    void deletedAnswerCannotBeEdited() {
        Question q = QuestionTest.persistedQuestion();
        Answer a = QuestionTest.persistedAnswer(q, answerer.userId(), 50);
        a.softDelete(answerer, false, false, QuestionTest.NOW);

        assertThatThrownBy(() -> a.edit(answerer, false, MarkdownBody.ofAnswer("已删除后又尝试修改的内容"), QuestionTest.NOW))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("已被删除");
    }

    @Test
    void idCanBeAssignedOnlyOnce() {
        Question q = QuestionTest.persistedQuestion();
        Answer a = QuestionTest.persistedAnswer(q, answerer.userId(), 50);
        assertThatThrownBy(() -> a.assignId(51)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ID 已分配");
    }

    @Test
    void reconstitutedAnswerExposesPersistedFields() {
        Answer a = Answer.reconstitute(50L, 10L, answerer.userId(),
                MarkdownBody.ofAnswer("这是从持久层还原回来的回答正文内容。"), 3, 9L,
                QuestionTest.NOW, QuestionTest.NOW.plusSeconds(20), QuestionTest.NOW.plusSeconds(40), 2L);

        assertThat(a.authorId()).isEqualTo(answerer.userId());
        assertThat(a.body().value()).contains("还原");
        assertThat(a.score()).isEqualTo(3);
        assertThat(a.createdAt()).isEqualTo(QuestionTest.NOW);
        assertThat(a.updatedAt()).isEqualTo(QuestionTest.NOW.plusSeconds(20));
        assertThat(a.deletedAt()).isEqualTo(QuestionTest.NOW.plusSeconds(40));
        assertThat(a.version()).isEqualTo(2L);
    }
}
