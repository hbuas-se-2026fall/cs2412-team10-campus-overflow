package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.identity.api.event.UserRegisteredEvent;
import com.campusoverflow.qa.api.QaQueryApi;
import com.campusoverflow.reputation.application.ReputationQueryServiceTest.FakeIdentity;
import com.campusoverflow.reputation.application.ReputationQueryServiceTest.InMemoryAccounts;
import com.campusoverflow.reputation.application.ReputationQueryServiceTest.InMemoryBadges;
import com.campusoverflow.reputation.application.ReputationQueryServiceTest.InMemoryLedger;
import com.campusoverflow.reputation.api.event.ReputationChangedEvent;
import com.campusoverflow.reputation.domain.BadgeType;
import com.campusoverflow.reputation.domain.Bounty;
import com.campusoverflow.reputation.domain.BountyRepository;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.event.IntegrationEvent;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 声誉链路验收：从上游集成事件（注册 / 采纳 / 投票）出发，验证「记账 → 余额 → 徽章 → 对账」整条链路
 * 的幂等性、每日上限与一致性不变量（余额恒等于流水之和）。
 */
class ReputationChainAcceptanceTest {

    private static final Instant NOW = Instant.parse("2026-11-21T02:00:00Z");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final InMemoryAccounts accounts = new InMemoryAccounts();
    private final InMemoryLedger ledger = new InMemoryLedger();
    private final InMemoryBadges badges = new InMemoryBadges();
    private final EmptyBounties bounties = new EmptyBounties();
    private final RecordingPublisher events = new RecordingPublisher();
    private final NoopAudit audit = new NoopAudit();
    private final FakeIdentity identity = new FakeIdentity()
            .withUser(1L, "小柯", Role.STUDENT)
            .withUser(2L, "小周", Role.STUDENT)
            .withUser(9L, "李老师", Role.TEACHER);

    private ReputationEventHandlers handlers;
    private ReconciliationService reconciliation;

    @BeforeEach
    void setUp() {
        BadgeService badgeService = new BadgeService(badges, events, clock);
        ReputationLedgerService ledgerService = new ReputationLedgerService(accounts, ledger, badgeService, events, clock);
        BountyService bountyService = new BountyService(bounties, accounts, ledgerService, new NoQuestionApi(),
                identity, events, audit, clock);
        handlers = new ReputationEventHandlers(accounts, ledger, ledgerService, badgeService, bountyService, clock);
        reconciliation = new ReconciliationService(accounts, ledger);
    }

    @Test
    @DisplayName("注册开账户：学生 0 分、教师 1000 悬赏积分，重复注册幂等")
    void registrationOpensAccountOnce() {
        handlers.on(new UserRegisteredEvent("evt-reg-1", NOW, 1L, "小柯", Role.STUDENT));
        handlers.on(new UserRegisteredEvent("evt-reg-2", NOW, 9L, "李老师", Role.TEACHER));
        handlers.on(new UserRegisteredEvent("evt-reg-3", NOW, 1L, "小柯", Role.STUDENT));

        assertThat(account(1L).reputation()).isZero();
        assertThat(account(1L).availablePoints()).isZero();
        assertThat(account(9L).availablePoints()).isEqualTo(1000);
    }

    @Test
    @DisplayName("采纳链路：回答者 +15、写入流水、发布 ReputationChanged 并授予首个采纳徽章")
    void acceptanceCreditsAuthorAndGrantsBadge() {
        handlers.on(accepted("evt-acc-1", 2L, 1L));

        assertThat(account(2L).reputation()).isEqualTo(15);
        assertThat(ledger.countByUserAndReason(2L, ReputationReason.ANSWER_ACCEPTED)).isEqualTo(1);
        assertThat(badges.has(2L, BadgeType.FIRST_ACCEPTED)).isTrue();
        assertThat(events.ofType(ReputationChangedEvent.class)).hasSize(1);
        assertThat(events.ofType(ReputationChangedEvent.class).get(0).delta()).isEqualTo(15);
    }

    @Test
    @DisplayName("采纳链路：自问自答被采纳不加声誉，也不写流水")
    void selfAcceptanceCreditsNothing() {
        handlers.on(accepted("evt-acc-self", 1L, 1L));

        assertThat(ledger.entries).isEmpty();
        assertThat(accounts.findByUserId(1L)).isEmpty();
    }

    @Test
    @DisplayName("幂等：同一采纳事件重复投递只记一次账")
    void repeatedEventIsIdempotent() {
        handlers.on(accepted("evt-acc-dup", 2L, 1L));
        handlers.on(accepted("evt-acc-dup", 2L, 1L));

        assertThat(account(2L).reputation()).isEqualTo(15);
        assertThat(ledger.entries).hasSize(1);
    }

    @Test
    @DisplayName("投票链路：点赞 +10、改投与撤销按新旧效果差计算增量")
    void voteChainUsesDeltaSemantics() {
        handlers.on(vote("evt-vote-1", 0, 1));
        handlers.on(vote("evt-vote-2", 1, -1));
        handlers.on(vote("evt-vote-3", -1, 0));

        assertThat(account(2L).reputation()).isZero();
        assertThat(ledger.entries).hasSize(3);
        assertThat(ledger.entries.get(0).delta()).isEqualTo(10);
        assertThat(ledger.entries.get(1).delta()).isEqualTo(-12);
        assertThat(ledger.entries.get(2).delta()).isEqualTo(2);
    }

    @Test
    @DisplayName("反作弊：每人每日因被投票获得的正向声誉最多 200，超出部分不再入账")
    void dailyVoteCapApplies() {
        for (int i = 0; i < 21; i++) {
            handlers.on(vote("evt-cap-" + i, 0, 1));
        }

        assertThat(account(2L).reputation()).isEqualTo(200);
        assertThat(ledger.entries).hasSize(20);
        assertThat(badges.has(2L, BadgeType.REPUTATION_100)).isTrue();
    }

    @Test
    @DisplayName("对账不变量：正常情况下余额与流水之和一致，无人为篡改时无差异")
    void reconciliationReportsNoMismatch() {
        handlers.on(accepted("evt-acc-1", 2L, 1L));
        handlers.on(vote("evt-vote-1", 0, 1));

        assertThat(reconciliation.reconcile()).isEmpty();
        assertThat(account(2L).reputation()).isEqualTo(25);
    }

    @Test
    @DisplayName("对账不变量：账户被绕过账本地直接改分时能被检出")
    void reconciliationDetectsTampering() {
        handlers.on(accepted("evt-acc-1", 2L, 1L));

        ReputationAccount tampered = account(2L);
        tampered.applyReputation(100);
        accounts.save(tampered);

        List<ReconciliationService.Mismatch> mismatches = reconciliation.reconcile();
        assertThat(mismatches).hasSize(1);
        assertThat(mismatches.get(0).userId()).isEqualTo(2L);
        assertThat(mismatches.get(0).accountReputation()).isEqualTo(115);
        assertThat(mismatches.get(0).ledgerSum()).isEqualTo(15L);
    }

    private ReputationAccount account(long userId) {
        return accounts.findByUserId(userId).orElseThrow();
    }

    private static com.campusoverflow.qa.api.event.AnswerAcceptedEvent accepted(String eventId, long answerAuthorId,
                                                                               long questionAuthorId) {
        return new com.campusoverflow.qa.api.event.AnswerAcceptedEvent(eventId, NOW, 501L, 301L,
                answerAuthorId, questionAuthorId, questionAuthorId, "进程与线程的区别？");
    }

    private static com.campusoverflow.qa.api.event.VoteCastEvent vote(String eventId, int oldValue, int newValue) {
        return new com.campusoverflow.qa.api.event.VoteCastEvent(eventId, NOW, "ANSWER", 501L, 2L, 1L, 301L,
                oldValue, newValue);
    }

    // ---- 链路外围的轻量替身 ----

    static class RecordingPublisher implements IntegrationEventPublisher {
        final List<IntegrationEvent> published = new ArrayList<>();

        @Override
        public void publish(IntegrationEvent event) {
            published.add(event);
        }

        <T extends IntegrationEvent> List<T> ofType(Class<T> type) {
            List<T> result = new ArrayList<>();
            published.forEach(e -> {
                if (type.isInstance(e)) {
                    result.add(type.cast(e));
                }
            });
            return result;
        }
    }

    static class NoopAudit implements AuditTrail {
        @Override
        public void record(String action, Long actorId, String targetType, String targetId, String detail) {
        }
    }

    static class NoQuestionApi implements QaQueryApi {
        @Override
        public Optional<com.campusoverflow.qa.api.QuestionRef> findQuestion(long questionId) {
            return Optional.empty();
        }
    }

    /** 本组用例不涉及悬赏，仓储恒为空，使采纳链路只走声誉与徽章分支。 */
    static class EmptyBounties implements BountyRepository {
        @Override
        public Optional<Bounty> findOpenByQuestion(long questionId) {
            return Optional.empty();
        }

        @Override
        public Optional<Bounty> findLatestByQuestion(long questionId) {
            return Optional.empty();
        }

        @Override
        public List<Bounty> findDue(Instant now, int limit) {
            return List.of();
        }

        @Override
        public Bounty save(Bounty bounty) {
            return bounty;
        }
    }
}
