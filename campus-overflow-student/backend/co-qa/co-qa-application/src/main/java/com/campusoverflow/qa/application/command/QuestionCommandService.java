package com.campusoverflow.qa.application.command;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.qa.api.event.ContentDeletedEvent;
import com.campusoverflow.qa.api.event.QuestionEditedEvent;
import com.campusoverflow.qa.api.event.QuestionPostedEvent;
import com.campusoverflow.qa.application.port.ContentRenderer;
import com.campusoverflow.qa.application.port.QuestionListCache;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.TagRepository;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用例：发布 / 编辑 / 删除问题。 */
@Service
public class QuestionCommandService {

    static final int PLAIN_TEXT_FOR_INDEX = 2000;

    private final QuestionRepository questions;
    private final TagRepository tags;
    private final IdentityApi identity;
    private final IntegrationEventPublisher events;
    private final ContentRenderer renderer;
    private final QuestionListCache listCache;
    private final AuditTrail audit;
    private final Clock clock;

    public QuestionCommandService(QuestionRepository questions, TagRepository tags, IdentityApi identity,
                                  IntegrationEventPublisher events, ContentRenderer renderer,
                                  QuestionListCache listCache, AuditTrail audit, Clock clock) {
        this.questions = questions;
        this.tags = tags;
        this.identity = identity;
        this.events = events;
        this.renderer = renderer;
        this.listCache = listCache;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public long post(Actor actor, PostQuestionCommand cmd) {
        Instant now = Instant.now(clock);
        Title title = new Title(cmd.title());
        MarkdownBody body = MarkdownBody.ofQuestion(cmd.body());
        Tags tagList = Tags.of(cmd.tags());
        Question question = Question.post(actor.userId(), cmd.courseId(), title, body, tagList, now);
        Question saved = questions.save(question);
        tags.adjustUsage(tagList.names(), 1);
        String plainText = renderer.toPlainText(body.value(), PLAIN_TEXT_FOR_INDEX);
        events.publish(new QuestionPostedEvent(EventIds.next(), now, saved.id(), actor.userId(),
                cmd.courseId(), title.value(), plainText, tagList.names()));
        listCache.invalidateAll();
        audit.record("QUESTION_POSTED", actor.userId(), "QUESTION", String.valueOf(saved.id()), title.value());
        return saved.id();
    }

    @Transactional
    public void edit(Actor actor, long questionId, EditQuestionCommand cmd) {
        Question q = load(questionId);
        Title title = new Title(cmd.title());
        MarkdownBody body = MarkdownBody.ofQuestion(cmd.body());
        Tags tagList = Tags.of(cmd.tags());
        Instant now = Instant.now(clock);
        boolean manager = identity.canManageCourse(actor.userId(), q.courseId());
        Set<String> tagsBefore = new HashSet<>(q.tags().names());
        q.edit(actor, manager, title, body, tagList, now);
        questions.save(q);
        adjustTagUsage(tagsBefore, tagList.names());
        String plainText = renderer.toPlainText(body.value(), PLAIN_TEXT_FOR_INDEX);
        events.publish(new QuestionEditedEvent(EventIds.next(), now, q.id(), title.value(), plainText, tagList.names()));
        listCache.invalidateAll();
        audit.record("QUESTION_EDITED", actor.userId(), "QUESTION", String.valueOf(q.id()), title.value());
    }

    @Transactional
    public void delete(Actor actor, long questionId) {
        Question q = load(questionId);
        boolean manager = identity.canManageCourse(actor.userId(), q.courseId());
        Instant now = Instant.now(clock);
        Set<String> tagsBefore = new HashSet<>(q.tags().names());
        q.softDelete(actor, manager, now);
        questions.save(q);
        tags.adjustUsage(tagsBefore, -1);
        events.publish(new ContentDeletedEvent(EventIds.next(), now, "QUESTION", q.id(), q.id()));
        listCache.invalidateAll();
        audit.record("QUESTION_DELETED", actor.userId(), "QUESTION", String.valueOf(q.id()), null);
    }

    /**
     * 按差集维护标签使用计数：只有新挂上的标签 +1、被摘掉的标签 -1。
     * 编辑时若整体按新集合加一次，未变化的标签会被反复累加，标签热度会持续虚高。
     */
    private void adjustTagUsage(Set<String> before, List<String> after) {
        Set<String> added = new HashSet<>(after);
        added.removeAll(before);
        Set<String> removed = new HashSet<>(before);
        removed.removeAll(after);
        tags.adjustUsage(added, 1);
        tags.adjustUsage(removed, -1);
    }

    private Question load(long questionId) {
        return questions.findById(questionId)
                .orElseThrow(() -> new NotFoundException("QA-1000", "问题不存在: " + questionId));
    }
}
