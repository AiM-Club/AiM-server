package targeter.aim.domain.challenge.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import targeter.aim.domain.ai.llm.dto.RoutePayload;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.challenge.repository.ChallengeMemberRepository;
import targeter.aim.domain.challenge.repository.ChallengeRepository;
import targeter.aim.domain.challenge.repository.WeeklyProgressRepository;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChallengeRoutePersistServiceTest {

    @InjectMocks
    private ChallengeRoutePersistService persistService;

    @Mock private ChallengeRepository challengeRepository;
    @Mock private ChallengeMemberRepository challengeMemberRepository;
    @Mock private WeeklyProgressRepository weeklyProgressRepository;
    @Mock private UserRepository userRepository;

    @Test
    @DisplayName("중복 방지: 이미 존재하는 챌린지가 있으면 저장 없이 ID만 반환한다 (멱등성)")
    void persistAtomic_Skip_If_Exists() {
        // given
        Long userId = 1L;
        ChallengeDto.ChallengeCreateRequest request = ChallengeDto.ChallengeCreateRequest.builder()
                .name("Existing Challenge")
                .startedAt(LocalDate.now())
                .build();

        User mockUser = mock(User.class);
        Challenge existingChallenge = mock(Challenge.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(existingChallenge.getId()).willReturn(999L);

        // DB에 이미 있다고 가정
        given(challengeRepository.findByHostAndNameAndStartedAt(mockUser, request.getName(), request.getStartedAt()))
                .willReturn(Optional.of(existingChallenge));

        // when
        Long resultId = persistService.persistAtomic(userId, request, new RoutePayload());

        // then
        assertThat(resultId).isEqualTo(999L);
        // 저장이 호출되지 않아야 함
        verify(challengeRepository, never()).save(any(Challenge.class));
        verify(weeklyProgressRepository, never()).save(any());
    }

    @Test
    @DisplayName("정상 저장: 새로운 챌린지라면 엔티티들이 모두 저장된다")
    void persistAtomic_Success() {
        // given
        Long userId = 1L;
        ChallengeDto.ChallengeCreateRequest request = ChallengeDto.ChallengeCreateRequest.builder()
                .name("New Challenge")
                .startedAt(LocalDate.now())
                .jobs(List.of("Dev"))
                .duration(1)
                .build();

        RoutePayload payload = new RoutePayload();
        RoutePayload.Week week = new RoutePayload.Week();
        week.setWeekNumber(1);
        payload.setWeeks(List.of(week));

        User mockUser = mock(User.class);
        Challenge savedChallenge = mock(Challenge.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(challengeRepository.findByHostAndNameAndStartedAt(any(), any(), any()))
                .willReturn(Optional.empty()); // 중복 없음
        given(challengeRepository.save(any(Challenge.class))).willReturn(savedChallenge);
        given(savedChallenge.getId()).willReturn(100L);

        // when
        Long resultId = persistService.persistAtomic(userId, request, payload);

        // then
        assertThat(resultId).isEqualTo(100L);
        verify(challengeRepository, times(1)).save(any(Challenge.class));
        verify(challengeMemberRepository, times(1)).save(any());
        verify(weeklyProgressRepository, times(1)).save(any());
    }
}