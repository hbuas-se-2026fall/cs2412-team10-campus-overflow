package com.campusoverflow.reputation.application;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.reputation.domain.BadgeRepository;
import com.campusoverflow.reputation.domain.LedgerRepository;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReputationQueryService {

    private final ReputationAccountRepository accounts;
    private final LedgerRepository ledger;
    private final BadgeRepository badges;
    private final IdentityApi identity;

    public ReputationQueryService(ReputationAccountRepository accounts, LedgerRepository ledger,
                                  BadgeRepository badges, IdentityApi identity) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.badges = badges;
        this.identity = identity;
    }

    public ReputationProfileView profile(long userId, Actor viewerOrNull) {
        ReputationAccount account = accounts.findByUserId(userId)
                .orElseGet(() -> ReputationAccount.open(userId, 0));
        UserSummary user = identity.findUser(userId).orElse(null);
        String displayName = user != null ? user.displayName() : "用户" + userId;
        boolean selfOrAdmin = viewerOrNull != null && (viewerOrNull.is(userId) || viewerOrNull.isAdmin());
        Integer available = selfOrAdmin ? account.availablePoints() : null;
        Integer frozen = selfOrAdmin ? account.frozenPoints() : null;
        List<ReputationProfileView.BadgeView> badgeViews = badges.findByUser(userId).stream()
                .map(a -> new ReputationProfileView.BadgeView(a.type().name(), a.type().displayName(),
                        a.type().description(), a.awardedAt()))
                .toList();
        return new ReputationProfileView(userId, displayName, account.reputation(), available, frozen, badgeViews);
    }

    /** 声誉流水属于个人数据：仅本人与管理员可查看。 */
    public PageResult<LedgerEntryView> ledger(long userId, Actor viewer, PageRequest page) {
        if (viewer == null || (!viewer.is(userId) && !viewer.isAdmin())) {
            throw new ForbiddenException("REP-3002", "仅本人与管理员可查看声誉流水");
        }
        return ledger.findByUser(userId, page)
                .map(e -> new LedgerEntryView(e.id(), e.reason().name(), e.reason().label(), e.delta(),
                        e.refType(), e.refId(), e.createdAt()));
    }
}
