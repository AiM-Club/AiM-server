package targeter.aim.domain.challenge.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.entity.*;
import targeter.aim.domain.user.entity.Gender;
import targeter.aim.domain.user.entity.Tier;
import targeter.aim.domain.user.entity.User;
import targeter.aim.system.configuration.QueryDSLConfig;
import targeter.aim.system.security.model.UserDetails;

import java.time.LocalDate;

import static com.jayway.jsonpath.internal.path.PathCompiler.fail;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DataJpaTest
@Import({
        QueryDSLConfig.class,
        ChallengeQueryRepository.class
})
class ChallengeQueryRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ChallengeQueryRepository challengeQueryRepository;

    private Tier tier;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {

        tier = Tier.builder()
                .name("BRONZE")
                .build();

        em.persist(tier);

        // User 생성
        user1 = createUser("user1");
        user2 = createUser("user2");

        em.persist(user1);
        em.persist(user2);

        createChallenges();

        em.flush();
        em.clear();
    }

    private User createUser(String nickname) {
        return User.builder()
                .loginId(nickname + "_login")
                .password("password")
                .nickname(nickname)
                .birthday(LocalDate.of(2000, 1, 1))
                .gender(Gender.FEMALE)
                .tier(tier)
                .build();
    }

    private void createChallenges() {

        // 1~10: user1 주최 + 진행중 + user1 좋아요
        for (int i = 1; i <= 10; i++) {
            Challenge challenge = createChallenge(
                    String.format("챌린지%02d", i),
                    user1,
                    ChallengeStatus.IN_PROGRESS,
                    i   // i일 전
            );

            em.persist(
                    ChallengeMember.builder()
                            .id(ChallengeMemberId.of(challenge, user1))
                            .role(MemberRole.HOST)
                            .build()
            );

            em.persist(new ChallengeLiked(user1, challenge));
        }


        // 11~15: user2 주최 + 완료 + user1 참여
        for (int i = 11; i <= 15; i++) {
            Challenge challenge = createChallenge(
                    String.format("챌린지%02d", i),
                    user2,
                    ChallengeStatus.COMPLETED,
                    i
            );

            em.persist(
                    ChallengeMember.builder()
                            .id(ChallengeMemberId.of(challenge, user1))
                            .role(MemberRole.MEMBER)
                            .build()
            );
        }

        // 16~20: user2 주최 + 진행중 + user1 참여 + 좋아요
        for (int i = 16; i <= 20; i++) {
            Challenge challenge = createChallenge(
                    String.format("챌린지%02d", i),
                    user2,
                    ChallengeStatus.IN_PROGRESS,
                    i
            );

            em.persist(
                    ChallengeMember.builder()
                            .id(ChallengeMemberId.of(challenge, user1))
                            .role(MemberRole.MEMBER)
                            .build()
            );

            em.persist(new ChallengeLiked(user1, challenge));

        }

        // 21~25: user2 주최 + SOLO + 진행중 (VS 조회 시 제외돼야 함)
        for (int i = 21; i <= 25; i++) {
            Challenge challenge = Challenge.builder()
                    .name(String.format("솔로챌린지%02d", i))
                    .host(user2)
                    .job("백엔드")
                    .startedAt(LocalDate.now().minusDays(i))
                    .durationWeek(2)
                    .mode(ChallengeMode.SOLO) // ⭐ 핵심
                    .status(ChallengeStatus.IN_PROGRESS)
                    .visibility(ChallengeVisibility.PUBLIC)
                    .build();

            em.persist(challenge);

            em.persist(
                    ChallengeMember.builder()
                            .id(ChallengeMemberId.of(challenge, user2))
                            .role(MemberRole.HOST)
                            .build()
            );
        }
    }

    private Challenge createChallenge(
            String name,
            User host,
            ChallengeStatus status,
            int daysAgo
    ) {
        Challenge challenge = Challenge.builder()
                .name(name)
                .host(host)
                .job("백엔드")
                .startedAt(LocalDate.now().minusDays(daysAgo))
                .durationWeek(2)
                .mode(ChallengeMode.VS)
                .status(status)
                .visibility(ChallengeVisibility.PUBLIC)
                .build();

        em.persist(challenge);
        return challenge;
    }

    @Test
    void ALL_탭_기본조회_페이지네이션_확인() {
        // given
        ChallengeDto.ListSearchCondition condition =
                ChallengeDto.ListSearchCondition.builder()
                        .filterType("ALL")
                        .sort("LATEST")
                        .build();

        Pageable pageable = PageRequest.of(0, 16);

        // when
        Page<ChallengeDto.ChallengeListResponse> page =
                challengeQueryRepository.paginateByType(
                        null, // 비로그인
                        pageable,
                        ChallengeFilterType.ALL,
                        ChallengeSortType.LATEST
                );

        // then
        assertThat(page.getContent()).hasSize(16);
        assertThat(page.getTotalElements()).isEqualTo(20);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void MY_탭_조회시_내가_참여한_챌린지만_조회된다() {
        // given
        Pageable pageable = PageRequest.of(0, 30);

        Page<ChallengeDto.ChallengeListResponse> page =
                challengeQueryRepository.paginateByType(
                        new UserDetails(user1),
                        pageable,
                        ChallengeFilterType.MY,
                        ChallengeSortType.LATEST
                );

        // then
        // user1이 참여하지 않은 챌린지(21~25)는 제외
        assertThat(page.getTotalElements()).isEqualTo(20);
        assertThat(page.getContent())
                .noneMatch(dto ->
                        dto.getName().equals("챌린지21")
                                || dto.getName().equals("챌린지22")
                                || dto.getName().equals("챌린지23")
                                || dto.getName().equals("챌린지24")
                                || dto.getName().equals("챌린지25")
                );

    }


    @Test
    void LATEST_정렬은_최신순으로_조회된다() {
        // given
        Pageable pageable = PageRequest.of(0, 20);

        // when
        Page<ChallengeDto.ChallengeListResponse> page =
                challengeQueryRepository.paginateByType(
                        null,
                        pageable,
                        ChallengeFilterType.ALL,
                        ChallengeSortType.LATEST
                );

        // then
        var content = page.getContent();

        for (int i = 0; i < content.size() - 1; i++) {
            assertThat(content.get(i).getCreatedAt())
                    .isAfterOrEqualTo(content.get(i + 1).getCreatedAt());
        }
    }

    @Test
    void OLDEST_정렬은_오래된순으로_조회된다() {
        Pageable pageable = PageRequest.of(0, 20);

        Page<ChallengeDto.ChallengeListResponse> page =
                challengeQueryRepository.paginateByType(
                        null,
                        pageable,
                        ChallengeFilterType.ALL,
                        ChallengeSortType.OLDEST
                );

        var content = page.getContent();

        for (int i = 0; i < content.size() - 1; i++) {
            assertThat(content.get(i).getCreatedAt())
                    .isBeforeOrEqualTo(content.get(i + 1).getCreatedAt());
        }
    }

    @Test
    void TITLE_정렬은_이름_오름차순으로_조회된다() {
        Pageable pageable = PageRequest.of(0, 20);

        Page<ChallengeDto.ChallengeListResponse> page =
                challengeQueryRepository.paginateByType(
                        null,
                        pageable,
                        ChallengeFilterType.ALL,
                        ChallengeSortType.TITLE
                );

        var content = page.getContent();

        for (int i = 0; i < content.size() - 1; i++) {
            assertThat(content.get(i).getName()
                    .compareTo(content.get(i + 1).getName()))
                    .isLessThanOrEqualTo(0);
        }
    }

    @Test
    void ONGOING_정렬은_진행중이_먼저_오고_종료일_빠른순이다() {
        Pageable pageable = PageRequest.of(0, 20);

        Page<ChallengeDto.ChallengeListResponse> page =
                challengeQueryRepository.paginateByType(
                        null,
                        pageable,
                        ChallengeFilterType.ALL,
                        ChallengeSortType.ONGOING
                );

        var content = page.getContent();

        // IN_PROGRESS 이후에 COMPLETED만 와야 함
        boolean metCompleted = false;

        for (var dto : content) {
            if (dto.getStatus() == ChallengeStatus.COMPLETED) {
                metCompleted = true;
            }

            if (metCompleted && dto.getStatus() == ChallengeStatus.IN_PROGRESS) {
                fail("COMPLETED 이후에 IN_PROGRESS가 나왔습니다.");
            }
        }

        // IN_PROGRESS 구간 종료일 오름차순
        for (int i = 0; i < content.size() - 1; i++) {
            var c1 = content.get(i);
            var c2 = content.get(i + 1);

            if (c1.getStatus() == ChallengeStatus.IN_PROGRESS
                    && c2.getStatus() == ChallengeStatus.IN_PROGRESS) {

                LocalDate end1 = c1.getStartDate()
                        .plusWeeks(Long.parseLong(c1.getDuration().replace("주", "")));
                LocalDate end2 = c2.getStartDate()
                        .plusWeeks(Long.parseLong(c2.getDuration().replace("주", "")));

                assertThat(end1).isBeforeOrEqualTo(end2);
            }
        }
    }


    @Test
    void FINISHED_정렬은_완료된것이_먼저_오고_종료일_최근순이다() {
        Pageable pageable = PageRequest.of(0, 20);

        Page<ChallengeDto.ChallengeListResponse> page =
                challengeQueryRepository.paginateByType(
                        null,
                        pageable,
                        ChallengeFilterType.ALL,
                        ChallengeSortType.FINISHED
                );

        var content = page.getContent();

        // COMPLETED 이후에 IN_PROGRESS만 나와야 함
        boolean metInProgress = false;

        for (var dto : content) {
            if (dto.getStatus() == ChallengeStatus.IN_PROGRESS) {
                metInProgress = true;
            }

            if (metInProgress && dto.getStatus() == ChallengeStatus.COMPLETED) {
                fail("IN_PROGRESS 이후에 COMPLETED가 나왔습니다.");
            }
        }

        // COMPLETED 구간 종료일 내림차순 (최신순)
        for (int i = 0; i < content.size() - 1; i++) {
            var c1 = content.get(i);
            var c2 = content.get(i + 1);

            if (c1.getStatus() == ChallengeStatus.COMPLETED
                    && c2.getStatus() == ChallengeStatus.COMPLETED) {

                LocalDate end1 = c1.getStartDate()
                        .plusWeeks(Long.parseLong(c1.getDuration().replace("주", "")));
                LocalDate end2 = c2.getStartDate()
                        .plusWeeks(Long.parseLong(c2.getDuration().replace("주", "")));

                assertThat(end1).isAfterOrEqualTo(end2);
            }
        }
    }

    @Test
    void VS_챌린지만_조회된다() {
        // given
        Pageable pageable = PageRequest.of(0, 30);

        // when
        Page<ChallengeDto.ChallengeListResponse> page =
                challengeQueryRepository.paginateByType(
                        null,
                        pageable,
                        ChallengeFilterType.ALL,
                        ChallengeSortType.LATEST
                );

        // then
        // VS 챌린지만 20개여야 함
        assertThat(page.getTotalElements()).isEqualTo(20);

        // SOLO 챌린지는 이름으로 확인
        assertThat(page.getContent())
                .noneMatch(dto -> dto.getName().startsWith("솔로챌린지"));
    }

}