package targeter.aim.domain.challenge.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.repository.ChallengeFilterType;
import targeter.aim.domain.challenge.repository.ChallengeQueryRepository;
import targeter.aim.domain.challenge.repository.ChallengeSortType;
import targeter.aim.domain.user.entity.User;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ChallengeServiceTest {

    @InjectMocks
    private ChallengeService challengeService;

    @Mock
    private ChallengeQueryRepository challengeQueryRepository;

    private User user;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .loginId("test_login")
                .nickname("tester")
                .build();

        userDetails = new UserDetails(user);
    }

    @Test
    void MY_탭은_비로그인_사용자가_접근하면_예외가_발생한다() {
        // given
        var condition = ChallengeDto.ListSearchCondition.builder()
                .filterType("MY")
                .sort("LATEST")
                .build();

        // when & then
        assertThatThrownBy(() ->
                challengeService.getVsChallenges(
                        condition,
                        null,
                        PageRequest.of(0, 16)
                )
        ).isInstanceOf(RestException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_LOGIN_REQUIRED);

    }

    @Test
    void MY_탭은_로그인_사용자면_정상_호출된다() {
        // given
        var condition = ChallengeDto.ListSearchCondition.builder()
                .filterType("MY")
                .sort("LATEST")
                .build();

        when(challengeQueryRepository.paginateByType(
                eq(userDetails),
                any(Pageable.class),
                eq(ChallengeFilterType.MY),
                eq(ChallengeSortType.CREATED_AT)
        )).thenReturn(
                new PageImpl<>(List.of(), PageRequest.of(0, 16), 0)
        );

        // when
        challengeService.getVsChallenges(
                condition,
                userDetails,
                PageRequest.of(0, 10)
        );
    }

    @Test
    void ALL_탭은_비로그인_사용자도_조회할_수_있다() {
        // given
        var condition = ChallengeDto.ListSearchCondition.builder()
                .filterType("ALL")
                .sort("LATEST")
                .build();

        when(challengeQueryRepository.paginateByType(
                isNull(),
                any(Pageable.class),
                eq(ChallengeFilterType.ALL),
                eq(ChallengeSortType.CREATED_AT)
        )).thenReturn(
                new PageImpl<>(List.of(), PageRequest.of(0, 16), 0)
        );

        // when
        challengeService.getVsChallenges(
                condition,
                null,
                PageRequest.of(0, 16)
        );
    }

}
