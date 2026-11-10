package com.campusoverflow.reputation.application;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.reputation.api.event.ReputationChangedEvent;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import com.campusoverflow.shared.event.ProcessedEventStore;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 排行榜：周/月/学期榜来自 Redis ZSET，总榜直接读账户表。 */
@Service
public class LeaderboardService {

    private final LeaderboardStore store;
    private final ReputationAccountRepository accounts;
    private final IdentityApi identity;
    private final ProcessedEventStore processed;
    private final Clock clock;

    public LeaderboardService(LeaderboardStore store, ReputationAccountRepository accounts, IdentityApi identity,
                              ProcessedEventStore processed, Clock clock) {
        this.store = store;
        this.accounts = accounts;
        this.identity = identity;
        this.processed = processed;
        this.clock = clock;
    }

    @EventListener
    @Transactional
    public void on(ReputationChangedEvent e) {
        if (!processed.markProcessed(e.eventId(), "leaderboard")) {
            return; // 重复投递：已处理
        }
        store.increment(e.userId(), e.delta(), e.occurredAt());
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryView> top(LeaderboardPeriod period, int limit) {
        Instant now = clock.instant();
        if (period == LeaderboardPeriod.ALL) {
            List<ReputationAccount> topAccounts = accounts.topByReputation(Math.min(limit, 100));
            Map<Long, UserSummary> users = identity.findUsers(
                    topAccounts.stream().map(ReputationAccount::userId).toList());
            List<LeaderboardEntryView> result = new ArrayList<>();
            int rank = 1;
            for (ReputationAccount account : topAccounts) {
                UserSummary user = users.get(account.userId());
                result.add(new LeaderboardEntryView(rank++, account.userId(),
                        user != null ? user.displayName() : "用户" + account.userId(),
                        user != null && user.verified(), account.reputation()));
            }
            return result;
        }
        List<LeaderboardStore.Entry> entries = store.top(period, now, Math.min(limit, 100));
        Map<Long, UserSummary> users = identity.findUsers(
                entries.stream().map(LeaderboardStore.Entry::userId).toList());
        List<LeaderboardEntryView> result = new ArrayList<>();
        int rank = 1;
        for (LeaderboardStore.Entry entry : entries) {
            UserSummary user = users.get(entry.userId());
            result.add(new LeaderboardEntryView(rank++, entry.userId(),
                    user != null ? user.displayName() : "用户" + entry.userId(),
                    user != null && user.verified(), entry.score()));
        }
        return result;
    }
}
