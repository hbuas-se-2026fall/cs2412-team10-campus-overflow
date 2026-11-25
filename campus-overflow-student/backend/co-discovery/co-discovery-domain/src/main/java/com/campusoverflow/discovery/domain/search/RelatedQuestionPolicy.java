package com.campusoverflow.discovery.domain.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 相关问题推荐（规则引擎，非 AI）：按标签集合的 Jaccard 相似度排序，
 * J(A,B) = |A∩B| / |A∪B|；相似度相同则得分高、已采纳者优先。
 */
public final class RelatedQuestionPolicy {

    private RelatedQuestionPolicy() {
    }

    /** Jaccard 相似度：交集 / 并集；并集为空时返回 0。 */
    public static double jaccard(int shared, int sizeA, int sizeB) {
        int union = sizeA + sizeB - shared;
        if (union <= 0) {
            return 0.0;
        }
        return (double) shared / union;
    }

    /**
     * 按相似度降序排序；相似度相同则得分高优先，再相同则已采纳优先，最后按问题 ID 升序保证结果稳定。
     * 返回截断到 {@code limit} 条的新列表，不修改入参。
     */
    public static List<SearchIndex.RelatedCandidate> rank(List<SearchIndex.RelatedCandidate> candidates, int ownTagCount,
                                                          int limit) {
        if (candidates == null || candidates.isEmpty() || limit <= 0) {
            return List.of();
        }
        List<SearchIndex.RelatedCandidate> ordered = new ArrayList<>(candidates);
        Comparator<SearchIndex.RelatedCandidate> bySimilarity =
                Comparator.comparingDouble((SearchIndex.RelatedCandidate c) -> jaccard(c.sharedTags(), ownTagCount,
                        c.tagCount())).reversed();
        Comparator<SearchIndex.RelatedCandidate> byScore =
                Comparator.comparingInt(SearchIndex.RelatedCandidate::score).reversed();
        Comparator<SearchIndex.RelatedCandidate> byAccepted =
                Comparator.comparing((SearchIndex.RelatedCandidate c) -> c.accepted()).reversed();
        ordered.sort(bySimilarity.thenComparing(byScore).thenComparing(byAccepted)
                .thenComparingLong(SearchIndex.RelatedCandidate::questionId));
        return ordered.stream().limit(limit).toList();
    }
}
