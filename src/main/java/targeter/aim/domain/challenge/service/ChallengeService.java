package targeter.aim.domain.challenge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.ai.llm.dto.RoutePayload;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.challenge.entity.WeeklyProgress;
import targeter.aim.domain.challenge.repository.ChallengeRepository;
import targeter.aim.domain.challenge.repository.WeeklyProgressRepository;
import targeter.aim.domain.label.entity.Field;
import targeter.aim.domain.label.entity.Tag;
import targeter.aim.domain.label.repository.FieldRepository;
import targeter.aim.domain.label.repository.TagRepository;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.UserRepository;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final WeeklyProgressRepository weeklyProgressRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final FieldRepository fieldRepository;

    private final ChallengeRoutePersistService persistService;
    private final ChallengeRouteGenerationService generationService;

    @Transactional
    public ChallengeDto.ChallengeDetailsResponse createChallenge(UserDetails userDetails, ChallengeDto.ChallengeCreateRequest request) {

        User user = userDetails.getUser();

        // 1. AI 주차별 생성 로직을 위한 request 생성
        ChallengeDto.ProgressCreateRequest progressRequest = ChallengeDto.ProgressCreateRequest.builder()
                .name(request.getName())
                .startedAt(request.getStartedAt())
                .duration(request.getDuration())
                .tags(request.getTags())
                .fields(request.getFields())
                .jobs(request.getJobs())
                .userRequest(request.getUserRequest())
                .build();

        // 2. 주차별 계획(Payload) 생성
        RoutePayload routePayload = generationService.generateRoute(progressRequest);

        // 3. 생성된 데이터 저장
        Long challengeId = persistService.persistAtomic(user.getId(), progressRequest, routePayload);

        // 4. 생성된 챌린지 조회 및 추가 정보
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RestException(ErrorCode.CHALLENGE_NOT_FOUND));

        challenge.setMode(request.getMode());
        challenge.setVisibility(request.getVisibility());

        // 5. 태그 / 분야 연관관계 매핑
        updateChallengeLabels(challenge, request.getTags(), request.getFields());

        return toChallengeDetailsResponse(challenge, user.getId());
    }

    private void updateChallengeLabels(Challenge challenge, List<String> tagNames, List<String> fieldNames) {
        // Tag 처리
        if(tagNames != null) {
            Set<Tag> tags = new HashSet<>();
            for (String name: tagNames) {
                String normalizedName = name.trim();
                Tag tag = tagRepository.findByName(normalizedName)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(normalizedName).build()));
                tags.add(tag);
            }
            challenge.setTags(tags);
        }

        // field 처리
        if(fieldNames != null) {
            Set<Field> fields = new HashSet<>();
            for (String name: fieldNames) {
                String normalizedName = name.trim();
                Field field = fieldRepository.findByName(normalizedName)
                        .orElseGet(() -> fieldRepository.save(Field.builder().name(normalizedName).build()));
                fields.add(field);
            }
            challenge.setFields(fields);
        }
    }

    private ChallengeDto.ChallengeDetailsResponse toChallengeDetailsResponse(Challenge challenge, Long currentUserId) {
        User host = challenge.getHost();

        // 1. ChallengeInfo
        ChallengeDto.ChallengeDetailsResponse.ChallengeInfo info = ChallengeDto.ChallengeDetailsResponse.ChallengeInfo.builder()
                .challengeThumbnail(null)
                .title(challenge.getName())
                .tags(challenge.getTags().stream().map(Tag::getName).collect(Collectors.toList()))
                .fields(challenge.getFields().stream().map(Field::getName).collect(Collectors.toList()))
                .jobs(List.of(challenge.getJob().split(",")))
                .startedAt(challenge.getStartedAt())
                .durationWeek(challenge.getDurationWeek())
                .status(challenge.getStatus())
                .build();

        // 2. Participants
        ChallengeDto.ChallengeDetailsResponse.ParticipantDetails me = ChallengeDto.ChallengeDetailsResponse.ParticipantDetails.builder()
                .profileImage(null)
                .nickname(host.getNickname())
                .progressRate("0%")
                .successRate(0)
                .isSuccess(false)
                .isRealTimeActive(false)
                .build();

        ChallengeDto.ChallengeDetailsResponse.Participants participants = ChallengeDto.ChallengeDetailsResponse.Participants.builder()
                .me(me)
                .opponent(null)
                .build();

        // 3. CurrentWeekDetails
        WeeklyProgress week1Progress = weeklyProgressRepository.findByChallengeAndWeekNumber(challenge, 1)
                .orElse(WeeklyProgress.builder() // 예외 방지용 더미
                        .weekNumber(1)
                        .title("생성 중...")
                        .content("데이터를 불러오는 중입니다.")
                        .isComplete(false)
                        .stopwatchTimeSeconds(0)
                        .build());

        ChallengeDto.ChallengeDetailsResponse.CurrentWeekDetails currentWeek = ChallengeDto.ChallengeDetailsResponse.CurrentWeekDetails.builder()
                .weekNumber(week1Progress.getWeekNumber())
                .period(null) // TODO: 날짜 계산 로직 필요 (start date 기준 1주차 기간)
                .weekTitle(week1Progress.getTitle())
                .weekContent(week1Progress.getContent())
                .recordTime(String.valueOf(week1Progress.getStopwatchTimeSeconds())) // 포맷팅 필요 시 수정
                .certifiedFile(null)
                .isFinished(week1Progress.getIsComplete())
                .comments(Collections.emptyList()) // 생성 시 댓글 없음
                .build();

        return ChallengeDto.ChallengeDetailsResponse.builder()
                .challengeInfo(info)
                .participants(participants)
                .currentWeekDetails(currentWeek)
                .build();
    }
}
