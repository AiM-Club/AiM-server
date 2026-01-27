package targeter.aim.domain.challenge.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.entity.*;
import targeter.aim.domain.challenge.repository.*;
import targeter.aim.domain.challenge.service.ChallengeCleanupService;
import targeter.aim.domain.challenge.service.ChallengeRouteGenerationService;
import targeter.aim.domain.challenge.service.ChallengeRoutePersistService;
import targeter.aim.domain.challenge.service.ChallengeService;
import targeter.aim.domain.file.handler.FileHandler;
import targeter.aim.domain.label.repository.FieldRepository;
import targeter.aim.domain.label.repository.TagRepository;
import targeter.aim.domain.label.service.FieldService;
import targeter.aim.domain.label.service.TagService;
import targeter.aim.domain.user.entity.Tier;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.UserRepository;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceVsResultTest {

    @Mock private ChallengeRepository challengeRepository;
    @Mock private ChallengeMemberRepository challengeMemberRepository;
    @Mock private WeeklyProgressRepository weeklyProgressRepository;
    @Mock private ChallengeLikedRepository challengeLikedRepository;
    @Mock private UserRepository userRepository;
    @Mock private TagRepository tagRepository;
    @Mock private FieldRepository fieldRepository;
    @Mock private ChallengeQueryRepository challengeQueryRepository;
    @Mock private WeeklyProgressQueryRepository weeklyProgressQueryRepository;
    @Mock private ChallengeRoutePersistService persistService;
    @Mock private ChallengeRouteGenerationService generationService;
    @Mock private ChallengeCleanupService cleanupService;
    @Mock private TagService tagService;
    @Mock private FieldService fieldService;
    @Mock private FileHandler fileHandler;

    @InjectMocks
    private ChallengeService challengeService;

    @Test
    @DisplayName("VS 결과: 성공 주차 수가 더 많은 HOST가 승리한다")
    void getVsChallengeResult_hostWinsBySuccessWeeks() {
        // given
        Challenge challenge = Challenge.builder()
                .id(1L)
                .mode(ChallengeMode.VS)
                .durationWeek(8)
                .startedAt(LocalDate.now())
                .build();

        User host = userWithTier(10L, "host");
        User member = userWithTier(20L, "member");

        when(challengeRepository.findById(1L)).thenReturn(Optional.of(challenge));

        ChallengeMember hostCm = mockChallengeMember(MemberRole.HOST, host);
        ChallengeMember memCm = mockChallengeMember(MemberRole.MEMBER, member);
        when(challengeMemberRepository.findAllById_Challenge(challenge)).thenReturn(List.of(hostCm, memCm));

        // host SUCCESS 3, member SUCCESS 1
        when(weeklyProgressRepository.findAllByChallengeAndUser(challenge, host)).thenReturn(List.of(
                wp(WeeklyStatus.SUCCESS, 100),
                wp(WeeklyStatus.SUCCESS, 100),
                wp(WeeklyStatus.SUCCESS, 100),
                wp(WeeklyStatus.FAIL, 10)
        ));
        when(weeklyProgressRepository.findAllByChallengeAndUser(challenge, member)).thenReturn(List.of(
                wp(WeeklyStatus.SUCCESS, 999),
                wp(WeeklyStatus.FAIL, 999),
                wp(WeeklyStatus.FAIL, 999)
        ));

        // when
        ChallengeDto.VsResultResponse res = challengeService.getVsChallengeResult(1L);

        // then
        assertThat(res.getChallengeId()).isEqualTo(1L);
        assertThat(res.getDurationWeeks()).isEqualTo(8);
        assertThat(res.getWinnerInfo()).isNotNull();

        Long winnerInfoUserId = extractUserId(res.getWinnerInfo());
        assertThat(winnerInfoUserId).isEqualTo(host.getId());
    }

    @Test
    @DisplayName("VS 결과: 성공 주차 수가 같으면 총 elapsed가 큰 MEMBER가 승리한다")
    void getVsChallengeResult_memberWinsByElapsedWhenSuccessTied() {
        // given
        Challenge challenge = Challenge.builder()
                .id(2L)
                .mode(ChallengeMode.VS)
                .durationWeek(4)
                .startedAt(LocalDate.now())
                .build();

        User host = userWithTier(11L, "host");
        User member = userWithTier(22L, "member");

        when(challengeRepository.findById(2L)).thenReturn(Optional.of(challenge));

        ChallengeMember hostCm = mockChallengeMember(MemberRole.HOST, host);
        ChallengeMember memCm = mockChallengeMember(MemberRole.MEMBER, member);
        when(challengeMemberRepository.findAllById_Challenge(challenge)).thenReturn(List.of(hostCm, memCm));

        // 둘 다 SUCCESS 2
        // host elapsed total = 200
        // member elapsed total = 300 -> member 승
        when(weeklyProgressRepository.findAllByChallengeAndUser(challenge, host)).thenReturn(List.of(
                wp(WeeklyStatus.SUCCESS, 100),
                wp(WeeklyStatus.SUCCESS, 100),
                wp(WeeklyStatus.FAIL, 0)
        ));
        when(weeklyProgressRepository.findAllByChallengeAndUser(challenge, member)).thenReturn(List.of(
                wp(WeeklyStatus.SUCCESS, 150),
                wp(WeeklyStatus.SUCCESS, 150),
                wp(WeeklyStatus.FAIL, 0)
        ));

        // when
        ChallengeDto.VsResultResponse res = challengeService.getVsChallengeResult(2L);

        // then
        assertThat(res.getChallengeId()).isEqualTo(2L);
        assertThat(res.getDurationWeeks()).isEqualTo(4);
        assertThat(res.getWinnerInfo()).isNotNull();

        Long winnerInfoUserId = extractUserId(res.getWinnerInfo());
        assertThat(winnerInfoUserId).isEqualTo(member.getId());
    }

    @Test
    @DisplayName("VS 결과: 성공 주차/elapsed 모두 같으면 현재 정책대로 HOST가 승리한다")
    void getVsChallengeResult_hostWinsWhenFullyTied() {
        // given
        Challenge challenge = Challenge.builder()
                .id(3L)
                .mode(ChallengeMode.VS)
                .durationWeek(4)
                .startedAt(LocalDate.now())
                .build();

        User host = userWithTier(100L, "host");
        User member = userWithTier(200L, "member");

        when(challengeRepository.findById(3L)).thenReturn(Optional.of(challenge));

        ChallengeMember hostCm = mockChallengeMember(MemberRole.HOST, host);
        ChallengeMember memCm = mockChallengeMember(MemberRole.MEMBER, member);
        when(challengeMemberRepository.findAllById_Challenge(challenge)).thenReturn(List.of(hostCm, memCm));

        // 완전 동점
        when(weeklyProgressRepository.findAllByChallengeAndUser(challenge, host)).thenReturn(List.of(
                wp(WeeklyStatus.SUCCESS, 100),
                wp(WeeklyStatus.SUCCESS, 100)
        ));
        when(weeklyProgressRepository.findAllByChallengeAndUser(challenge, member)).thenReturn(List.of(
                wp(WeeklyStatus.SUCCESS, 100),
                wp(WeeklyStatus.SUCCESS, 100)
        ));

        // when
        ChallengeDto.VsResultResponse res = challengeService.getVsChallengeResult(3L);

        // then
        Long winnerInfoUserId = extractUserId(res.getWinnerInfo());
        assertThat(winnerInfoUserId).isEqualTo(host.getId());
    }

    // ===== helpers =====

    private WeeklyProgress wp(WeeklyStatus status, Integer elapsedSeconds) {
        return WeeklyProgress.builder()
                .weeklyStatus(status)
                .elapsedTimeSeconds(elapsedSeconds)
                .build();
    }

    private ChallengeMember mockChallengeMember(MemberRole role, User user) {
        ChallengeMember cm = mock(ChallengeMember.class);
        ChallengeMemberId id = mock(ChallengeMemberId.class);

        when(cm.getRole()).thenReturn(role);
        when(cm.getId()).thenReturn(id);
        when(id.getUser()).thenReturn(user);

        return cm;
    }

    /**
     * ✅ 핵심: UserDto.UserResponse.from(user)에서 tier.getName()을 호출하므로
     * 테스트용 User에 tier를 반드시 넣어준다.
     */
    private User userWithTier(Long id, String nickname) {
        User u = User.builder()
                .id(id)
                .nickname(nickname)
                .build();

        Tier tier = mock(Tier.class);
        lenient().when(tier.getName()).thenReturn("BRONZE");

        // user.setTier(...)가 있으면 그걸 쓰고, 없으면 reflection으로 주입
        boolean injected = tryInvokeSetter(u, "setTier", Tier.class, tier);
        if (!injected) {
            setField(u, "tier", tier); // User 엔티티 필드명이 tier라고 가정(로그가 그렇게 말해줌)
        }

        return u;
    }

    private boolean tryInvokeSetter(Object target, String methodName, Class<?> paramType, Object value) {
        try {
            target.getClass().getMethod(methodName, paramType).invoke(target, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            fail("테스트용 주입 실패: " + target.getClass().getSimpleName() + "." + fieldName + " 필드를 찾거나 주입할 수 없습니다. (" + e.getMessage() + ")");
        }
    }

    /**
     * winnerInfo 타입이 UserDto.UserResponse라서 프로젝트마다 필드명이 다를 수 있음.
     * - getUserId() 또는 getId() 중 하나를 찾아서 반환.
     */
    private Long extractUserId(Object winnerInfo) {
        if (winnerInfo == null) return null;

        try {
            var m = winnerInfo.getClass().getMethod("getUserId");
            return (Long) m.invoke(winnerInfo);
        } catch (Exception ignored) {}

        try {
            var m = winnerInfo.getClass().getMethod("getId");
            return (Long) m.invoke(winnerInfo);
        } catch (Exception e) {
            fail("winnerInfo에서 userId(id)를 추출할 수 없습니다. getUserId() 또는 getId()를 제공해야 합니다.");
            return null;
        }
    }
}
