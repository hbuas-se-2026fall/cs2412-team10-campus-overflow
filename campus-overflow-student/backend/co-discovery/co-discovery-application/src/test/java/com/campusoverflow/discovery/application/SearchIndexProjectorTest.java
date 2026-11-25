package com.campusoverflow.discovery.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.discovery.domain.search.SearchDocument;
import com.campusoverflow.discovery.domain.search.SearchIndex;
import com.campusoverflow.discovery.domain.search.SearchIndex.RelatedCandidate;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.ContentDeletedEvent;
import com.campusoverflow.qa.api.event.QuestionEditedEvent;
import com.campusoverflow.qa.api.event.QuestionPostedEvent;
import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.reputation.api.event.BountyAwardedEvent;
import com.campusoverflow.reputation.api.event.BountyClosedEvent;
import com.campusoverflow.reputation.api.event.BountyOpenedEvent;
import com.campusoverflow.shared.event.ProcessedEventStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 搜索投影器：验证各类事件折叠到索引，且重复事件被幂等守卫拦截。 */
class SearchIndexProjectorTest {

    private static final Instant NOW = Instant.parse("2026-11-18T08:00:00Z");

    private FakeIndex index;
    private FakeProcessed processed;
    private SearchIndexProjector projector;

    @BeforeEach
    void setUp() {
        index = new FakeIndex();
        processed = new FakeProcessed();
        projector = new SearchIndexProjector(index, processed);
    }

    @Test
    @DisplayName("发帖事件写入索引文档；重复投递不重复插入")
    void postInsertsDocumentAndDeduplicates() {
        QuestionPostedEvent e = new QuestionPostedEvent("evt-1", NOW, 101, 7L, 100L, "红黑树旋转", "变色为什么不够",
                List.of("数据结构", "红黑树"));

        projector.on(e);
        projector.on(e);

        assertThat(index.inserted).hasSize(1);
        SearchDocument doc = index.inserted.get(0);
        assertThat(doc.questionId()).isEqualTo(101);
        assertThat(doc.authorId()).isEqualTo(7L);
        assertThat(doc.courseId()).isEqualTo(100L);
        assertThat(doc.title()).isEqualTo("红黑树旋转");
        assertThat(doc.bodyText()).isEqualTo("变色为什么不够");
        assertThat(doc.tags()).containsExactly("数据结构", "红黑树");
        assertThat(doc.createdAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("编辑事件更新标题、正文与标签")
    void editUpdatesText() {
        projector.on(new QuestionEditedEvent("evt-2", NOW, 101, "新标题", "新正文", List.of("算法")));
        assertThat(index.updateTexts).containsExactly("101:新标题");
    }

    @Test
    @DisplayName("回答提交使问题回答数 +1")
    void answerSubmittedIncrementsAnswerCount() {
        projector.on(new AnswerSubmittedEvent("evt-3", NOW, 2001, 101, 8L, 7L, "红黑树旋转"));
        assertThat(index.answerDeltas).containsExactly(new long[] {101, 1});
    }

    @Test
    @DisplayName("删除问题：索引标记删除；删除回答：回答数 -1")
    void deletedEventRoutesByTargetType() {
        projector.on(new ContentDeletedEvent("evt-4", NOW, "QUESTION", 101, 101));
        projector.on(new ContentDeletedEvent("evt-5", NOW, "ANSWER", 2001, 101));

        assertThat(index.deleted).containsExactly(101L);
        assertThat(index.answerDeltas).containsExactly(new long[] {101, -1});
    }

    @Test
    @DisplayName("采纳事件标记问题已采纳")
    void acceptedMarksAccepted() {
        projector.on(new AnswerAcceptedEvent("evt-6", NOW, 2001, 101, 8L, 7L, 7L, "红黑树旋转"));
        assertThat(index.accepted).containsExactly(101L);
    }

    @Test
    @DisplayName("问题投票按新旧差值调整得分；回答投票不触碰问题投影")
    void voteQuestionAdjustsScoreButAnswerDoesNot() {
        projector.on(new VoteCastEvent("evt-7", NOW, "QUESTION", 101, 7L, 9L, 101, 0, 1));
        projector.on(new VoteCastEvent("evt-8", NOW, "ANSWER", 2001, 8L, 9L, 101, -1, 0));

        assertThat(index.scoreDeltas).containsExactly(new long[] {101, 1});
    }

    @Test
    @DisplayName("悬赏：开启为 true，授予与关闭均回到 false")
    void bountyLifecycleUpdatesOpenFlag() {
        projector.on(new BountyOpenedEvent("evt-9", NOW, 3001, 101, 7L, 50, NOW.plusSeconds(3600)));
        assertThat(index.bounty).containsEntry(101L, true);

        projector.on(new BountyAwardedEvent("evt-10", NOW, 3001, 101, 7L, 8L, 50));
        projector.on(new BountyClosedEvent("evt-11", NOW, 3001, 101, 7L, 50));
        assertThat(index.bounty).containsEntry(101L, false);
    }

    // ---- fakes ----

    static class FakeIndex implements SearchIndex {
        final List<SearchDocument> inserted = new ArrayList<>();
        final List<String> updateTexts = new ArrayList<>();
        final List<long[]> answerDeltas = new ArrayList<>();
        final List<long[]> scoreDeltas = new ArrayList<>();
        final List<Long> accepted = new ArrayList<>();
        final Map<Long, Boolean> bounty = new HashMap<>();
        final List<Long> deleted = new ArrayList<>();

        @Override
        public void insert(SearchDocument document) {
            inserted.add(document);
        }

        @Override
        public void updateText(long questionId, String title, String bodyText, List<String> tags, Instant at) {
            updateTexts.add(questionId + ":" + title);
        }

        @Override
        public void adjustAnswerCount(long questionId, int delta, Instant at) {
            answerDeltas.add(new long[] {questionId, delta});
        }

        @Override
        public void adjustScore(long questionId, int delta) {
            scoreDeltas.add(new long[] {questionId, delta});
        }

        @Override
        public void markAccepted(long questionId, Instant at) {
            accepted.add(questionId);
        }

        @Override
        public void setBountyOpen(long questionId, boolean open) {
            bounty.put(questionId, open);
        }

        @Override
        public void markDeleted(long questionId) {
            deleted.add(questionId);
        }

        @Override
        public List<String> findTags(long questionId) {
            return List.of();
        }

        @Override
        public List<RelatedCandidate> findCandidatesSharingTags(long questionId, List<String> tags, int limit) {
            return List.of();
        }
    }

    static class FakeProcessed implements ProcessedEventStore {
        private final Set<String> seen = new HashSet<>();

        @Override
        public boolean markProcessed(String eventId, String consumer) {
            return seen.add(eventId + "|" + consumer);
        }
    }
}
