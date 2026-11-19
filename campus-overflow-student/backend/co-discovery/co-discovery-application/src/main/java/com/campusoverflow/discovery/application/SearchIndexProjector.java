package com.campusoverflow.discovery.application;

import com.campusoverflow.discovery.domain.search.SearchDocument;
import com.campusoverflow.discovery.domain.search.SearchIndex;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.ContentDeletedEvent;
import com.campusoverflow.qa.api.event.QuestionEditedEvent;
import com.campusoverflow.qa.api.event.QuestionPostedEvent;
import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.reputation.api.event.BountyAwardedEvent;
import com.campusoverflow.reputation.api.event.BountyClosedEvent;
import com.campusoverflow.reputation.api.event.BountyOpenedEvent;
import com.campusoverflow.shared.event.IntegrationEvent;
import com.campusoverflow.shared.event.ProcessedEventStore;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 事件投影器：把上游事件折叠为搜索投影（dis_search_doc）。
 * 计数器类更新不是天然幂等的，因此每个事件先经 ProcessedEventStore 去重（与投影更新同一事务）。
 */
@Component
public class SearchIndexProjector {

    static final String CONSUMER = "discovery.search-index";

    /** 事件中内容类型标识：问题（与 co-qa-domain 的 TargetType.QUESTION 名称一致，本模块只依赖 qa-api，故用字面量）。 */
    private static final String TYPE_QUESTION = "QUESTION";

    private final SearchIndex index;
    private final ProcessedEventStore processed;

    public SearchIndexProjector(SearchIndex index, ProcessedEventStore processed) {
        this.index = index;
        this.processed = processed;
    }

    /** 首次处理返回 true；重复投递返回 false，处理器应直接跳过。 */
    private boolean firstTime(IntegrationEvent e) {
        return processed.markProcessed(e.eventId(), CONSUMER);
    }

    @EventListener
    @Transactional
    public void on(QuestionPostedEvent e) {
        if (!firstTime(e)) {
            return;
        }
        index.insert(new SearchDocument(e.questionId(), e.courseId(), e.authorId(), e.title(), e.plainText(),
                e.tags(), e.occurredAt()));
    }

    @EventListener
    @Transactional
    public void on(QuestionEditedEvent e) {
        if (!firstTime(e)) {
            return;
        }
        index.updateText(e.questionId(), e.title(), e.plainText(), e.tags(), e.occurredAt());
    }

    @EventListener
    @Transactional
    public void on(AnswerSubmittedEvent e) {
        if (!firstTime(e)) {
            return;
        }
        index.adjustAnswerCount(e.questionId(), 1, e.occurredAt());
    }

    @EventListener
    @Transactional
    public void on(ContentDeletedEvent e) {
        if (!firstTime(e)) {
            return;
        }
        if (TYPE_QUESTION.equals(e.targetType())) {
            index.markDeleted(e.questionId());
        } else {
            // 删除的是回答：问题投影的回答数减一，问题本身保留。
            index.adjustAnswerCount(e.questionId(), -1, e.occurredAt());
        }
    }

    @EventListener
    @Transactional
    public void on(AnswerAcceptedEvent e) {
        if (!firstTime(e)) {
            return;
        }
        index.markAccepted(e.questionId(), e.occurredAt());
    }

    @EventListener
    @Transactional
    public void on(VoteCastEvent e) {
        if (!firstTime(e)) {
            return;
        }
        // 只有问题投票影响问题投影的得分；回答投票对应答案自身的评分，不在问题投影内。
        if (TYPE_QUESTION.equals(e.targetType())) {
            index.adjustScore(e.questionId(), e.newValue() - e.oldValue());
        }
    }

    @EventListener
    @Transactional
    public void on(BountyOpenedEvent e) {
        if (!firstTime(e)) {
            return;
        }
        index.setBountyOpen(e.questionId(), true);
    }

    @EventListener
    @Transactional
    public void on(BountyAwardedEvent e) {
        if (!firstTime(e)) {
            return;
        }
        index.setBountyOpen(e.questionId(), false);
    }

    @EventListener
    @Transactional
    public void on(BountyClosedEvent e) {
        if (!firstTime(e)) {
            return;
        }
        index.setBountyOpen(e.questionId(), false);
    }
}
