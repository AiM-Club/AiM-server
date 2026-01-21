package targeter.aim.domain.challenge.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.entity.*;
import targeter.aim.domain.challenge.repository.ChallengeMemberRepository;
import targeter.aim.domain.challenge.repository.ChallengeRepository;
import targeter.aim.domain.challenge.repository.WeeklyProgressQueryRepository;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.UserRepository;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceOverviewTest {

    @Mock ChallengeRepository challengeRepository;
    @Mock ChallengeMemberRepository challengeMemberRepository;
    @Mock UserRepository userRepository;
    @Mock WeeklyProgressQueryRepository weeklyProgressQueryRepository;

    @InjectMocks ChallengeService challengeService;

    User me;
    User opponent;
    Challenge challenge;

    @BeforeEach
    void setUp() {
        me = new User();
        opponent = new User();
        challenge = new Challenge();

        // ✅ id setter 막혀있어도 reflection으로 주입
        setEntityId(me, 1L);
        setEntityId(opponent, 2L);
        setEntityId(challenge, 10L);

        me.setNickname("me");
        opponent.setNickname("oppo");

        challenge.setMode(ChallengeMode.VS);
        challenge.setVisibility(ChallengeVisibility.PUBLIC);
        challenge.setName("vs challenge");
        challenge.setStartedAt(LocalDate.now().minusDays(10));
        challenge.setDurationWeek(4);
        challenge.setJob("DEV");
    }

    @Test
    void overview_success_public_vs() {
        // given
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUser()).thenReturn(me);

        when(challengeRepository.findById(10L)).thenReturn(Optional.of(challenge));
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));

        // ✅ ChallengeMemberId setter 없으니 static factory로 생성
        ChallengeMember cm1 = new ChallengeMember();
        cm1.setId(ChallengeMemberId.of(challenge, me));

        ChallengeMember cm2 = new ChallengeMember();
        cm2.setId(ChallengeMemberId.of(challenge, opponent));

        when(challengeMemberRepository.findAllById_Challenge(challenge))
                .thenReturn(List.of(cm1, cm2));

        // completedCountByUsers: endWeek가 뭐가 오더라도 동일 맵 반환
        when(weeklyProgressQueryRepository.completedCountByUsers(eq(10L), anyList(), anyInt()))
                .thenReturn(Map.of(1L, 2L, 2L, 1L));

        // when
        ChallengeDto.VsChallengeOverviewResponse res =
                challengeService.getVsChallengeOverview(10L, ud);

        // then
        assertThat(res).isNotNull();

        assertThat(res.getChallengeInfo()).isNotNull();
        assertThat(res.getChallengeInfo().getName()).isEqualTo("vs challenge");

        assertThat(res.getParticipants()).isNotNull();
        assertThat(res.getParticipants().getMe()).isNotNull();
        assertThat(res.getParticipants().getMe().getId()).isEqualTo(1L);

        // 멤버 2명이므로 상대 있어야 함
        assertThat(res.getParticipants().getOpponent()).isNotNull();
        assertThat(res.getParticipants().getOpponent().getId()).isEqualTo(2L);

        assertThat(res.getDominance()).isNotNull();
        assertThat(res.getDominance().getMyPercent() + res.getDominance().getOpponentPercent())
                .isEqualTo(100);

        verify(challengeRepository).findById(10L);
        verify(challengeMemberRepository).findAllById_Challenge(challenge);
        verify(weeklyProgressQueryRepository, atLeastOnce())
                .completedCountByUsers(eq(10L), anyList(), anyInt());
    }

    @Test
    void overview_success_when_opponent_missing() {
        // given
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUser()).thenReturn(me);

        when(challengeRepository.findById(10L)).thenReturn(Optional.of(challenge));
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));

        // 멤버가 나만 있는 케이스
        ChallengeMember cm1 = new ChallengeMember();
        cm1.setId(ChallengeMemberId.of(challenge, me));

        when(challengeMemberRepository.findAllById_Challenge(challenge))
                .thenReturn(List.of(cm1));

        when(weeklyProgressQueryRepository.completedCountByUsers(eq(10L), anyList(), anyInt()))
                .thenReturn(Map.of(1L, 1L));

        // when
        ChallengeDto.VsChallengeOverviewResponse res =
                challengeService.getVsChallengeOverview(10L, ud);

        // then
        assertThat(res).isNotNull();
        assertThat(res.getParticipants().getOpponent()).isNull();

        // 상대 없으면 정책에 따라 100:0(서비스 구현 기준)
        assertThat(res.getDominance().getMyPercent()).isEqualTo(100);
        assertThat(res.getDominance().getOpponentPercent()).isEqualTo(0);
    }

    @Test
    void overview_fail_when_not_logged_in() {
        assertThatThrownBy(() -> challengeService.getVsChallengeOverview(10L, null))
                .isInstanceOf(RestException.class);
    }

    @Test
    void overview_fail_when_not_vs_mode() {
        // given
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUser()).thenReturn(me);

        challenge.setMode(ChallengeMode.SOLO);
        when(challengeRepository.findById(10L)).thenReturn(Optional.of(challenge));

        // when & then
        assertThatThrownBy(() -> challengeService.getVsChallengeOverview(10L, ud))
                .isInstanceOf(RestException.class);
    }

    /**
     * ✅ JPA 엔티티의 @Id setter를 막아둔 경우 테스트에서 흔히 쓰는 패턴
     * - 부모 클래스(TimeStampedEntity 등)에 id가 있을 수도 있으니 상위로 타고 올라가며 탐색
     */
    private static void setEntityId(Object entity, Long idValue) {
        Class<?> clazz = entity.getClass();
        while (clazz != null) {
            try {
                Field idField = clazz.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(entity, idValue);
                return;
            } catch (NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("id 필드 주입 실패: " + entity.getClass().getName(), e);
            }
        }
        throw new IllegalStateException("id 필드를 찾지 못함: " + entity.getClass().getName());
    }
}
