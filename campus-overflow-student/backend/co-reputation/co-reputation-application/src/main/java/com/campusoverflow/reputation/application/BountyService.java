package com.campusoverflow.reputation.application;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.qa.api.QaQueryApi;
import com.campusoverflow.qa.api.QuestionRef;
import com.campusoverflow.reputation.api.event.BountyAwardedEvent;
import com.campusoverflow.reputation.api.event.BountyClosedEvent;
import com.campusoverflow.reputation.api.event.BountyOpenedEvent;
import com.campusoverflow.reputation.domain.Bounty;
import com.campusoverflow.reputation.domain.BountyRepository;
import com.campusoverflow.reputation.domain.BountyStatus;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 用例：悬赏的发起、结算、到期退款。积分冻结与悬赏状态在同一事务中变更。 */
@Service
public class BountyService {

    private final BountyRepository bounties;
    private final ReputationAccountRepository accounts;
    private final ReputationLedgerService ledgerService;
    private final QaQueryApi qa;
    private final IdentityApi identity;
    private final IntegrationEventPublisher events;
    private final AuditTrail audit;
    private final Clock clock;

    public BountyService(BountyRepository bounties, ReputationAccountRepository accounts,
                         ReputationLedgerService ledgerService, QaQueryApi qa, IdentityApi identity,
                         IntegrationEventPublisher events, AuditTrail audit, Clock clock) {
        this.bounties = bounties;
        this.accounts = accounts;
        this.ledgerService = ledgerService;
        this.qa = qa;
        this.identity = identity;
        this.events = events;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public long open(Actor actor, long questionId, int points, int days) {
        QuestionRef question = qa.findQuestion(questionId)
                .orElseThrow(() -> new NotFoundException("QA-404", "问题不存在: " + questionId));
        boolean managesCourse = identity.canManageCourse(actor.userId(), question.courseId());
        boolean questionOpen = !question.deleted() && question.acceptedAnswerId() == null;
        Bounty bounty = Bounty.open(questionId, questionOpen, actor, managesCourse, points, days, clock.instant());

        ReputationAccount sponsor = accounts.findByUserId(actor.userId())
                .orElseGet(() -> ReputationAccount.open(actor.userId(), 0));
        sponsor.freezePoints(points);
        accounts.save(sponsor);

        Bounty saved = bounties.save(bounty);
        audit.record("BOUNTY_OPEN", actor.userId(), "bounty", String.valueOf(saved.id()),
                points + " pts, question " + questionId);
        events.publish(new BountyOpenedEvent(EventIds.next(), clock.instant(), saved.id(), questionId,
                actor.userId(), points, saved.expiresAt()));
        return saved.id();
    }

    /** 由 AnswerAccepted 事件触发（在处理器事务内）。幂等：悬赏已结束则直接返回。 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void settleOnAccept(long questionId, long answerId, long winnerId, String sourceEventId) {
        Optional<Bounty> open = bounties.findOpenByQuestion(questionId);
        if (open.isEmpty()) {
            return; // 无悬赏或已结束（幂等）
        }
        Bounty bounty = open.get();
        ReputationAccount sponsor = accounts.findByUserId(bounty.sponsorId())
                .orElseGet(() -> ReputationAccount.open(bounty.sponsorId(), 0));
        try {
            bounty.award(answerId, winnerId, clock.instant());
        } catch (BusinessRuleException ex) {
            // 被采纳的恰好是发起人自己的回答：关闭并退款（状态机 EXPIRED）
            closeAndRefund(bounty, sponsor, clock.instant());
            return;
        }
        bounties.save(bounty);

        sponsor.spendFrozen(bounty.points());
        accounts.save(sponsor);

        ledgerService.record(winnerId, sourceEventId, ReputationReason.BOUNTY_AWARDED, bounty.points(),
                "question", questionId);
        events.publish(new BountyAwardedEvent(EventIds.next(), clock.instant(), bounty.id(), questionId,
                bounty.sponsorId(), winnerId, bounty.points()));
        audit.record("BOUNTY_AWARD", null, "bounty", String.valueOf(bounty.id()),
                "winner " + winnerId + ", " + bounty.points() + " pts");
    }

    /** 到期未采纳的悬赏：关闭并退回冻结积分。由定时任务调用。 */
    @Transactional
    public int expireDue() {
        List<Bounty> due = bounties.findDue(clock.instant(), 200);
        int closed = 0;
        for (Bounty bounty : due) {
            if (bounty.status() != BountyStatus.OPEN) {
                continue;
            }
            ReputationAccount sponsor = accounts.findByUserId(bounty.sponsorId())
                    .orElseGet(() -> ReputationAccount.open(bounty.sponsorId(), 0));
            closeAndRefund(bounty, sponsor, clock.instant());
            closed++;
        }
        return closed;
    }

    @Transactional(readOnly = true)
    public Optional<BountyView> latest(long questionId) {
        Optional<Bounty> found = bounties.findLatestByQuestion(questionId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Bounty b = found.get();
        String sponsorName = identity.findUser(b.sponsorId()).map(UserSummary::displayName).orElse(null);
        return Optional.of(new BountyView(b.id(), b.questionId(), b.sponsorId(), sponsorName, b.points(),
                b.status().name(), b.createdAt(), b.expiresAt(), b.winnerId(), b.answerId()));
    }

    private void closeAndRefund(Bounty bounty, ReputationAccount sponsor, Instant now) {
        bounty.close(now);
        bounties.save(bounty);
        sponsor.refundFrozen(bounty.points());
        accounts.save(sponsor);
        events.publish(new BountyClosedEvent(EventIds.next(), now, bounty.id(), bounty.questionId(),
                bounty.sponsorId(), bounty.points()));
        audit.record("BOUNTY_CLOSE", null, "bounty", String.valueOf(bounty.id()),
                "refund " + bounty.points() + " pts");
    }
}
