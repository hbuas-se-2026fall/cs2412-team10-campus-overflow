package com.campusoverflow.discovery.application;

import com.campusoverflow.discovery.domain.search.RelatedQuestionPolicy;
import com.campusoverflow.discovery.domain.search.SearchHit;
import com.campusoverflow.discovery.domain.search.SearchIndex;
import com.campusoverflow.discovery.domain.search.SearchIndex.RelatedCandidate;
import com.campusoverflow.discovery.domain.search.SearchPort;
import com.campusoverflow.discovery.domain.search.SearchQuery;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.shared.page.PageResult;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用例：搜索与筛选、相关问题推荐。 */
@Service
@Transactional(readOnly = true)
public class DiscoveryQueryService {

    /** 相关问题推荐返回条数上限，防止入参过大。 */
    private static final int MAX_RELATED = 20;

    private final SearchPort search;
    private final SearchIndex index;
    private final IdentityApi identity;

    public DiscoveryQueryService(SearchPort search, SearchIndex index, IdentityApi identity) {
        this.search = search;
        this.index = index;
        this.identity = identity;
    }

    public PageResult<SearchResultView> search(SearchQuery query) {
        PageResult<SearchHit> hits = search.search(query);
        Set<Long> authorIds = hits.items().stream().map(SearchHit::authorId).collect(Collectors.toSet());
        Map<Long, UserSummary> users = identity.findUsers(authorIds);
        List<SearchResultView> items = hits.items().stream()
                .map(hit -> new SearchResultView(
                        hit.questionId(),
                        hit.title(),
                        hit.snippet(),
                        hit.tags(),
                        hit.courseId(),
                        author(hit.authorId(), users.get(hit.authorId())),
                        hit.score(),
                        hit.answerCount(),
                        hit.accepted(),
                        hit.bountyOpen(),
                        hit.createdAt()))
                .toList();
        return new PageResult<>(items, hits.total(), hits.page(), hits.size());
    }

    public List<RelatedQuestionView> related(long questionId, int limit) {
        List<String> tags = index.findTags(questionId);
        if (tags.isEmpty() || limit <= 0) {
            return List.of();
        }
        int effectiveLimit = Math.min(limit, MAX_RELATED);
        List<RelatedCandidate> candidates = index.findCandidatesSharingTags(questionId, tags, effectiveLimit);
        List<RelatedCandidate> ranked = RelatedQuestionPolicy.rank(candidates, tags.size(), effectiveLimit);
        return ranked.stream()
                .map(c -> new RelatedQuestionView(c.questionId(), c.title(), c.answerCount(), c.accepted(),
                        RelatedQuestionPolicy.jaccard(c.sharedTags(), tags.size(), c.tagCount())))
                .toList();
    }

    private SearchResultView.Author author(long userId, UserSummary summary) {
        if (summary == null) {
            return new SearchResultView.Author(userId, "已注销用户", "STUDENT", false);
        }
        return new SearchResultView.Author(userId, summary.displayName(), summary.role().name(), summary.verified());
    }
}
