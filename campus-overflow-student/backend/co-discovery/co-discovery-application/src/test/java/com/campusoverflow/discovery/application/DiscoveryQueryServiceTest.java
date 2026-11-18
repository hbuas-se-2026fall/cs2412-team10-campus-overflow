package com.campusoverflow.discovery.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.discovery.domain.search.SearchDocument;
import com.campusoverflow.discovery.domain.search.SearchHit;
import com.campusoverflow.discovery.domain.search.SearchIndex;
import com.campusoverflow.discovery.domain.search.SearchIndex.RelatedCandidate;
import com.campusoverflow.discovery.domain.search.SearchPort;
import com.campusoverflow.discovery.domain.search.SearchQuery;
import com.campusoverflow.identity.api.CourseSummary;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 查询用例：搜索结果组装与作者补全；相关问题按 Jaccard 排序，空结果返回空列表。 */
class DiscoveryQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-18T08:00:00Z");

    private FakeSearchPort searchPort;
    private FakeSearchIndex index;
    private FakeIdentity identity;
    private DiscoveryQueryService service;

    @BeforeEach
    void setUp() {
        searchPort = new FakeSearchPort();
        index = new FakeSearchIndex();
        identity = new FakeIdentity();
        service = new DiscoveryQueryService(searchPort, index, identity);
    }

    @Test
    @DisplayName("搜索：分页与命中字段透传，作者批量补全，缺失用户兜底为已注销")
    void searchAssemblesHitsAndFillsAuthor() {
        identity.withUser(7L, "小明", Role.STUDENT);
        searchPort.hits = List.of(
                new SearchHit(101L, "红黑树旋转", "变色为什么不够", List.of("数据结构"), 100L, 7L, 3, 2, true, false, NOW),
                new SearchHit(102L, "二叉树遍历", "前中后序", List.of("算法"), null, 9L, 0, 0, false, true, NOW));

        PageResult<SearchResultView> result = service.search(new SearchQuery("红黑树", null, null, null, false, false,
                new PageRequest(1, 20)));

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(20);

        SearchResultView first = result.items().get(0);
        assertThat(first.id()).isEqualTo(101L);
        assertThat(first.excerpt()).isEqualTo("变色为什么不够");
        assertThat(first.tags()).containsExactly("数据结构");
        assertThat(first.courseId()).isEqualTo(100L);
        assertThat(first.author().displayName()).isEqualTo("小明");
        assertThat(first.author().role()).isEqualTo("STUDENT");
        assertThat(first.author().verified()).isTrue();
        assertThat(first.score()).isEqualTo(3);
        assertThat(first.answerCount()).isEqualTo(2);
        assertThat(first.accepted()).isTrue();
        assertThat(first.bountyOpen()).isFalse();

        SearchResultView second = result.items().get(1);
        assertThat(second.author().displayName()).isEqualTo("已注销用户");
        assertThat(second.author().role()).isEqualTo("STUDENT");
        assertThat(second.author().verified()).isFalse();
        assertThat(second.bountyOpen()).isTrue();
    }

    @Test
    @DisplayName("相关推荐：共享标签越多相似度越高，按 Jaccard 降序返回")
    void relatedRanksByJaccard() {
        index.tagsByQuestion.put(101L, List.of("数据结构", "红黑树"));
        index.candidates = List.of(
                new RelatedCandidate(201L, "候选A", 1, 3, 10, 2, false),
                new RelatedCandidate(202L, "候选B", 2, 2, 0, 0, true));

        List<RelatedQuestionView> related = service.related(101L, 5);

        assertThat(related).hasSize(2);
        assertThat(related).extracting(RelatedQuestionView::id).containsExactly(202L, 201L);
        RelatedQuestionView top = related.get(0);
        assertThat(top.similarity()).isEqualTo(1.0);
        assertThat(top.accepted()).isTrue();
        assertThat(top.answerCount()).isZero();
        assertThat(related.get(1).similarity()).isEqualTo(0.25);
    }

    @Test
    @DisplayName("相关推荐：问题无标签时返回空列表，不抛异常")
    void relatedWithoutTagsReturnsEmpty() {
        assertThat(service.related(404L, 5)).isEmpty();
    }

    @Test
    @DisplayName("相关推荐：无共享标签候选时返回空列表")
    void relatedWithoutCandidatesReturnsEmpty() {
        index.tagsByQuestion.put(101L, List.of("数据结构"));
        assertThat(service.related(101L, 5)).isEmpty();
    }

    // ---- fakes ----

    static class FakeSearchPort implements SearchPort {
        List<SearchHit> hits = List.of();

        @Override
        public PageResult<SearchHit> search(SearchQuery query) {
            return new PageResult<>(hits, hits.size(), query.page().page(), query.page().size());
        }
    }

    static class FakeSearchIndex implements SearchIndex {
        final Map<Long, List<String>> tagsByQuestion = new HashMap<>();
        List<RelatedCandidate> candidates = List.of();

        @Override
        public void insert(SearchDocument document) {
        }

        @Override
        public void updateText(long questionId, String title, String bodyText, List<String> tags, Instant at) {
        }

        @Override
        public void adjustAnswerCount(long questionId, int delta, Instant at) {
        }

        @Override
        public void adjustScore(long questionId, int delta) {
        }

        @Override
        public void markAccepted(long questionId, Instant at) {
        }

        @Override
        public void setBountyOpen(long questionId, boolean open) {
        }

        @Override
        public void markDeleted(long questionId) {
        }

        @Override
        public List<String> findTags(long questionId) {
            return tagsByQuestion.getOrDefault(questionId, List.of());
        }

        @Override
        public List<RelatedCandidate> findCandidatesSharingTags(long questionId, List<String> tags, int limit) {
            return candidates;
        }
    }

    static class FakeIdentity implements IdentityApi {
        private final Map<Long, UserSummary> users = new HashMap<>();

        FakeIdentity withUser(long id, String name, Role role) {
            users.put(id, new UserSummary(id, name, role, true, "计算机工程学院"));
            return this;
        }

        @Override
        public Optional<UserSummary> findUser(long userId) {
            return Optional.ofNullable(users.get(userId));
        }

        @Override
        public Map<Long, UserSummary> findUsers(Collection<Long> userIds) {
            Map<Long, UserSummary> result = new HashMap<>();
            userIds.forEach(id -> findUser(id).ifPresent(u -> result.put(id, u)));
            return result;
        }

        @Override
        public Optional<UserSummary> findByDisplayName(String displayName) {
            return users.values().stream().filter(u -> u.displayName().equals(displayName)).findFirst();
        }

        @Override
        public Optional<CourseSummary> findCourse(long courseId) {
            return Optional.empty();
        }

        @Override
        public boolean canManageCourse(long userId, Long courseId) {
            return false;
        }
    }
}
