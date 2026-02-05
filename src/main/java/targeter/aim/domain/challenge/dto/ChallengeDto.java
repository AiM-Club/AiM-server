package targeter.aim.domain.challenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.challenge.entity.*;
import targeter.aim.domain.file.dto.FileDto;
import targeter.aim.domain.label.dto.FieldDto;
import targeter.aim.domain.label.dto.TagDto;
import targeter.aim.domain.label.entity.Field;
import targeter.aim.domain.label.entity.Tag;
import targeter.aim.domain.user.dto.TierDto;
import targeter.aim.domain.user.dto.UserDto;
import targeter.aim.domain.user.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ChallengeDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "VS 챌린지 목록 조회 조건")
    public static class VsListSearchCondition {

        @Builder.Default
        @Schema(
                description = "탭 필터 (전체 / 내 챌린지)",
                example = "ALL",
                allowableValues = {"ALL", "MY"}
        )
        private ChallengeFilterType filterType = ChallengeFilterType.ALL;

        @Builder.Default
        @Schema(
                description = """
                    정렬 기준
                    - LATEST   : 최신순
                    - OLDEST   : 오래된순
                    - TITLE    : 가나다순
                    - IN_PROGRESS  : 진행 중 (종료일 기준 오름차순)
                    - COMPLETED : 진행 완료 (종료일 기준 내림차순)
                    """,
                example = "LATEST",
                allowableValues = {"LATEST", "OLDEST", "TITLE", "IN_PROGRESS", "COMPLETED"}
        )
        private ChallengeSortType sort = ChallengeSortType.LATEST;

        @Schema(description = "검색 키워드 (제목 기준 포함 검색)", example = "개발")
        private String keyword;

        @Schema(
                description = """
                분야 필터
                - ALL : 전체
                - IT : IT
                - BUSINESS  : 경영
                - ECONOMICS : 경제
                - POLITICS : 정치
                - LANGUAGE : 어문
                - SCIENCE : 자연(과학)
                - DESIGN : 디자인
                - SPORTS : 체육
                - MUSIC : 음악
                """,
                example = "ALL",
                allowableValues = {"ALL", "IT", "BUSINESS", "ECONOMICS", "POLITICS", "LANGUAGE", "SCIENCE", "DESIGN", "SPORTS", "MUSIC"}
        )
        private String field = "ALL";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "SOLO 챌린지 목록 조회 조건")
    public static class SoloListSearchCondition {

        @Builder.Default
        @Schema(
                description = "탭 필터 (진행 중 / 진행 완료)",
                example = "IN_PROGRESS",
                allowableValues = {"IN_PROGRESS", "COMPLETE"}
        )
        private ChallengeFilterType filterType = ChallengeFilterType.IN_PROGRESS;

        @Builder.Default
        @Schema(
                description = """
                정렬 기준
                - LATEST : 최신순
                - OLDEST : 오래된순
                - LIKED  : 좋아요순
                - TITLE  : 가나다순
                """,
                example = "LATEST",
                allowableValues = {"LATEST", "OLDEST", "LIKED", "TITLE"}
        )
        private ChallengeSortType sort = ChallengeSortType.LATEST;

        @Schema(description = "검색 키워드 (제목 기준 포함 검색)", example = "개발")
        private String keyword;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "전체 챌린지 목록 조회 조건")
    public static class AllListSearchCondition {

        @Builder.Default
        @Schema(
                description = """
                정렬 기준
                - LATEST : 최신순
                - OLDEST : 오래된순
                - LIKED  : 좋아요순
                - TITLE  : 가나다순
                """,
                example = "LATEST",
                allowableValues = {"LATEST", "OLDEST", "LIKED", "TITLE"}
        )
        private ChallengeSortType sort = ChallengeSortType.LATEST;

        @Schema(description = "검색 키워드 (제목 기준 포함 검색)", example = "개발")
        private String keyword;
    }

    public enum ChallengeSortType {
        LATEST,        // 최신순
        OLDEST,        // 오래된순
        LIKED,         // 좋아요순
        TITLE,         // 가나다순
        IN_PROGRESS,   // 진행 중
        COMPLETED      // 진행 완료
    }

    public enum ChallengeFilterType {
        ALL,           // 전체
        MY,            // 마이
        IN_PROGRESS,   // 진행 중
        COMPLETED      // 진행 완료
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "챌린지 작성자 정보")
    public static class UserResponse {

        @Schema(description = "유저 아이디", example = "1")
        private Long userId;

        @Schema(description = "유저 닉네임", example = "닉네임")
        private String nickname;

        @Schema(description = "티어명", example = "BRONZE")
        private TierDto.TierResponse tier;

        @Schema(description = "프로필 이미지")
        private FileDto.FileResponse profileImage;

        public static UserResponse from(User user) {
            return UserResponse.builder()
                    .userId(user.getId())
                    .nickname(user.getNickname())
                    .tier(TierDto.TierResponse.from(user.getTier()))
                    .profileImage(FileDto.FileResponse.from(user.getProfileImage()))
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "챌린지 목록 응답")
    public static class ChallengeListResponse {

        @Schema(description = "챌린지 아이디", example = "1")
        private Long challengeId;

        @Schema(description = "챌린지 모드", example = "VS | SOLO")
        private ChallengeMode mode;

        @Schema(description = "챌린지 썸네일")
        private FileDto.FileResponse thumbnail;

        @Schema(description = "챌린지 작성자 정보")
        private UserResponse user;

        @Schema(description = "챌린지 시작일", example = "2026-01-01")
        private LocalDate startedAt;

        @Schema(description = "챌린지 기간(주)", example = "4")
        private Integer durationWeek;

        @Schema(description = "챌린지 이름", example = "챌린지 제목")
        private String name;

        @Schema(description = "분야 목록(1~3개)", example = "[\"IT\", \"경영\"]")
        private List<String> fields;

        @Schema(description = "태그 목록(1~3개)", example = "[\"태그1\", \"태그2\", \"태그3\"]")
        private List<String> tags;

        @Schema(description = "직무", example = "직무")
        private String job;

        @Schema(description = "좋아요 여부", example = "true | false")
        private Boolean liked;

        @Schema(description = "좋아요 수", example = "1")
        private Integer likeCount;

        @Schema(description = "챌린지 상태", example = "IN_PROGRESS | COMPLETED")
        private ChallengeStatus status;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "페이지네이션 정보")
    public static class PageInfo {
        private int size;

        private int number;

        private long totalElements;

        private int totalPages;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "챌린지 목록 페이지 응답")
    public static class ChallengePageResponse {
        private List<ChallengeListResponse> content;
        private PageInfo page;

        public static ChallengePageResponse from(org.springframework.data.domain.Page<ChallengeListResponse> page) {
            return new ChallengePageResponse(
                    page.getContent(),
                    new PageInfo(
                            page.getSize(),
                            page.getNumber(),
                            page.getTotalElements(),
                            page.getTotalPages()
                    )
            );
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "챌린지 생성 요청")
    public static class ChallengeCreateRequest {
        @Schema(description = "챌린지 썸네일")
        private MultipartFile thumbnail;

        @Schema(description = "챌린지 이름", example = "다이어트 챌린지")
        private String name;

        @Schema(description = "시작일", example = "2026-01-01")
        private LocalDate startedAt;

        @Schema(description = "기간(주)", example = "6")
        private Integer durationWeek;

        @Schema(description = "태그 목록(1~3개)", example = "[\"태그1\", \"태그2\", \"태그3\"]")
        private List<String> tags;

        @Schema(description = "분야 목록(1~3개)", example = "[\"IT\", \"경영\"]")
        private List<String> fields;

        @Schema(description = "직무", example = "직무")
        private String job;

        @Schema(description = "AI 요청사항", example = "4주동안 빠르게 다이어트할 수 있는 방법을 알려줘. 금식은 최대한 자제할거야.")
        private String userRequest;

        @Schema(description = "챌린지 모드", example = "SOLO", allowableValues = {"SOLO", "VS"})
        private ChallengeMode mode;

        @Schema(description = "공개 여부", example = "PUBLIC", allowableValues = {"PUBLIC", "PRIVATE"})
        private ChallengeVisibility visibility;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "챌린지 아이디 응답 DTO")
    public static class ChallengeIdResponse {
        @Schema(description = "챌린지 id", example = "1")
        private Long challengeId;

        public static ChallengeIdResponse from(Challenge challenge) {
            return ChallengeIdResponse.builder()
                    .challengeId(challenge.getId())
                    .build();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "챌린지 수정 요청")
    public static class ChallengeUpdateRequest {
        @Schema(description = "챌린지 썸네일")
        private MultipartFile thumbnail;

        @Schema(description = "챌린지 이름", example = "다이어트 챌린지")
        private String name;

        @Schema(description = "시작일", example = "2026-01-01")
        private LocalDate startedAt;

        @Schema(description = "기간(주)", example = "6")
        private Integer durationWeek;

        @Schema(description = "태그 목록(1~3개)", example = "[\"태그1\", \"태그2\", \"태그3\"]")
        private List<String> tags;

        @Schema(description = "분야 목록(1~3개)", example = "[\"IT\", \"경영\"]")
        private List<String> fields;

        @Schema(description = "직무", example = "직무")
        private String job;

        @Schema(description = "AI 요청사항", example = "4주동안 빠르게 다이어트할 수 있는 방법을 알려줘. 금식은 최대한 자제할거야.")
        private String userRequest;

        @Schema(description = "챌린지 모드", example = "SOLO", allowableValues = {"SOLO", "VS"})
        private ChallengeMode mode;

        @Schema(description = "공개 여부", example = "PUBLIC", allowableValues = {"PUBLIC", "PRIVATE"})
        private ChallengeVisibility visibility;

        public void applyTo(Challenge challenge, Set<Tag> resolvedTags, Set<Field> resolvedFields) {
            if(name != null) {
                challenge.setName(name);
            }
            if(startedAt != null) {
                challenge.setStartedAt(startedAt);
            }
            if(durationWeek != null) {
                challenge.setDurationWeek(durationWeek);
            }
            if(job != null) {
                challenge.setJob(job);
            }
            if(mode != null) {
                challenge.setMode(mode);
            }
            if(visibility != null) {
                challenge.setVisibility(visibility);
            }
            if(tags != null) {
                challenge.getTags().clear();
                if(resolvedTags != null) challenge.getTags().addAll(resolvedTags);
            }
            if(fields != null) {
                challenge.getFields().clear();
                if(resolvedFields != null) challenge.getFields().addAll(resolvedFields);
            }
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "VS 챌린지 상세 Overview 응답 (챌린지 정보 + 우세현황 + 참여자 정보")
    public static class VsChallengeOverviewResponse {
        @Schema(description = "챌린지 기본 정보")
        private ChallengeInfo challengeInfo;

        @Schema(description = "우세현황")
        private Dominance dominance;

        @Schema(description = "참여자 정보")
        private Participants participants;

        public static VsChallengeOverviewResponse from(Challenge challenge, Boolean isLiked,
                                                       Dominance dominance,
                                                       User user, Integer myProgressRate, Integer mySuccessRate,
                                                       User opponent, Integer oppoProgressRate, Integer oppoSuccessRate) {
            return VsChallengeOverviewResponse.builder()
                    .challengeInfo(ChallengeInfo.from(challenge, isLiked))
                    .dominance(dominance)
                    .participants(Participants.from(user, myProgressRate, mySuccessRate, opponent, oppoProgressRate, oppoSuccessRate))
                    .build();
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class ChallengeInfo {
            @Schema(description = "작성자 아이디", example = "1")
            private Long writerId;

            @Schema(description = "작성자 닉네임", example = "닉네임")
            private String writerNickname;

            @Schema(description = "챌린지 썸네일")
            private FileDto.FileResponse thumbnail;

            @Schema(description = "챌린지 이름", example = "챌린지 제목")
            private String name;

            @Schema(description = "좋아요 여부", example = "true | false")
            private Boolean isLiked;

            @Schema(description = "좋아요수", example = "1")
            private Integer likedCount;

            @Schema(description = "분야 목록")
            private List<FieldDto.FieldResponse> fields;

            @Schema(description = "태그 목록")
            private List<TagDto.TagResponse> tags;

            @Schema(description = "직무", example = "개발자")
            private String job;

            @Schema(description = "시작 날짜", example = "2026-01-01")
            private LocalDate startDate;

            @Schema(description = "끝나는 날짜", example = "2026-01-31")
            private LocalDate endDate;

            @Schema(description = "총 기간(주)", example = "4")
            private Integer totalWeeks;

            public static ChallengeInfo from(Challenge challenge, boolean isLiked) {
                return ChallengeInfo.builder()
                        .writerId(challenge.getHost().getId())
                        .writerNickname(challenge.getHost().getNickname())
                        .thumbnail(challenge.getChallengeImage() == null ? null : FileDto.FileResponse.from(challenge.getChallengeImage()))
                        .name(challenge.getName())
                        .isLiked(isLiked)
                        .likedCount(challenge.getLikeCount())
                        .fields(challenge.getFields().stream()
                                .map(FieldDto.FieldResponse::from)
                                .collect(Collectors.toList()))
                        .tags(challenge.getTags().stream()
                                .map(TagDto.TagResponse::from)
                                .collect(Collectors.toList()))
                        .job(challenge.getJob())
                        .startDate(challenge.getStartedAt())
                        .endDate(challenge.getStartedAt().plusWeeks(challenge.getDurationWeek()).minusDays(1))
                        .totalWeeks(challenge.getDurationWeek())
                        .build();
            }
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class Dominance {
            @Schema(description = "상대방의 성공률", example = "50")
            private Integer opponentSuccessRate;

            @Schema(description = "내 성공률", example = "77")
            private Integer mySuccessRate;

            @Schema(description = "상대방 퍼센트, 막대폭 합 100", example = "40")
            private Integer opponentPercent;

            @Schema(description = "내 퍼센트, 막대폭 합 100", example = "60")
            private Integer myPercent;

            public static Dominance of(Integer opponentSuccessRate, Integer mySuccessRate,
                                       Integer opponentPercent, Integer myPercent) {
                return Dominance.builder()
                        .opponentSuccessRate(opponentSuccessRate)
                        .mySuccessRate(mySuccessRate)
                        .opponentPercent(opponentPercent)
                        .myPercent(myPercent)
                        .build();
            }
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class Participants {
            @Schema(description = "내 정보")
            private Me me;

            @Schema(description = "상대 정보")
            private Opponent opponent;

            public static Participants from(User user, Integer myProgressRate, Integer mySuccessRate,
                                            User opponent, Integer oppoProgressRate, Integer oppoSuccessRate) {
                return Participants.builder()
                        .me(Me.from(user, myProgressRate, mySuccessRate))
                        .opponent(opponent == null ? null : Opponent.from(opponent, oppoProgressRate, oppoSuccessRate))
                        .build();
            }
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class Me {
            @Schema(description = "유저 아이디(주차별 진행 불러오기 위함)", example = "1")
            private Long id;

            @Schema(description = "프로필 이미지")
            private FileDto.FileResponse profileImage;

            @Schema(description = "사용자 닉네임", example = "닉네임")
            private String nickname;

            @Schema(description = "진도율(완료 주차/전체 주자)", example = "77")
            private Integer progressRate;

            @Schema(description = "성공률(성공한 주차/현재 주차)", example = "80")
            private Integer successRate;

            @Schema(description = "성공 여부(successRate가 70 이상이어야 true)", example = "true")
            private Boolean isSuccess;

            public static Me from(User user, Integer progressRate, Integer successRate) {
                return Me.builder()
                        .id(user.getId())
                        .profileImage(user.getProfileImage() == null ? null : FileDto.FileResponse.from(user.getProfileImage()))
                        .nickname(user.getNickname())
                        .progressRate(progressRate == null ? 0 : progressRate)
                        .successRate(successRate == null ? 0 : successRate)
                        .isSuccess((successRate == null ? 0 : successRate) >= 70)
                        .build();
            }
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class Opponent {
            @Schema(description = "유저 아이디(주차별 진행 불러오기 위함)", example = "1")
            private Long id;

            @Schema(description = "프로필 이미지")
            private FileDto.FileResponse profileImage;

            @Schema(description = "사용자 닉네임", example = "닉네임")
            private String nickname;

            @Schema(description = "진도율(완료 주차/전체 주자)", example = "77")
            private Integer progressRate;

            @Schema(description = "성공률(성공한 주차/현재 주차)", example = "50")
            private Integer successRate;

            @Schema(description = "성공 여부(successRate가 70 이상이어야 true)", example = "false")
            private Boolean isSuccess;

            public static Opponent from(User user, Integer progressRate, Integer successRate) {
                return Opponent.builder()
                        .id(user.getId())
                        .profileImage(user.getProfileImage() == null ? null : FileDto.FileResponse.from(user.getProfileImage()))
                        .nickname(user.getNickname())
                        .progressRate(progressRate == null ? 0 : progressRate)
                        .successRate(successRate == null ? 0 : successRate)
                        .isSuccess((successRate == null ? 0 : successRate) >= 70)
                        .build();
            }
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "VS챌린지 최종 결과 응답")
    public static class VsResultResponse {
        private Long challengeId;

        private Integer durationWeek;

        private UserResponse winnerInfo;

        public static VsResultResponse from(Challenge challenge, User user) {
            return VsResultResponse.builder()
                    .challengeId(challenge.getId())
                    .durationWeek(challenge.getDurationWeek())
                    .winnerInfo(UserResponse.from(user))
                    .build();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "SOLO 챌린지 상세 Overview 응답 (챌린지 정보 + 주최자 정보)")
    public static class SoloChallengeOverviewResponse {

        @Schema(description = "챌린지 기본 정보")
        private ChallengeInfo challengeInfo;

        @Schema(description = "참여자 정보 (본인)")
        private Me participant;

        public static SoloChallengeOverviewResponse from(
                Challenge challenge, Boolean isLiked,
                User user,
                Integer progressRate,
                Integer successRate
        ) {
            return SoloChallengeOverviewResponse.builder()
                    .challengeInfo(ChallengeInfo.from(challenge, isLiked))
                    .participant(Me.from(user, progressRate, successRate))
                    .build();
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class ChallengeInfo {

            @Schema(description = "작성자 아이디", example = "1")
            private Long writerId;

            @Schema(description = "작성자 닉네임", example = "닉네임")
            private String writerNickname;

            @Schema(description = "챌린지 썸네일")
            private FileDto.FileResponse thumbnail;

            @Schema(description = "챌린지 이름", example = "챌린지 제목")
            private String name;

            @Schema(description = "챌린지 좋아요 여부", example = "true | false")
            private Boolean isLiked;

            @Schema(description = "챌린지 좋아요수", example = "1")
            private Integer likedCount;

            @Schema(description = "분야 목록")
            private List<FieldDto.FieldResponse> fields;

            @Schema(description = "태그 목록")
            private List<TagDto.TagResponse> tags;

            @Schema(description = "직무", example = "개발자")
            private String job;

            @Schema(description = "시작 날짜", example = "2026-01-01")
            private LocalDate startDate;

            @Schema(description = "끝나는 날짜", example = "2026-01-31")
            private LocalDate endDate;

            @Schema(description = "총 기간(주)", example = "4")
            private Integer totalWeeks;

            @Schema(description = "챌린지 상태", example = "IN_PROGRESS")
            private ChallengeStatus state;

            @Schema(description = "공개 여부", example = "PUBLIC")
            private ChallengeVisibility visibility;

            public static ChallengeInfo from(Challenge challenge, boolean isLiked) {
                return ChallengeInfo.builder()
                        .writerId(challenge.getHost().getId())
                        .writerNickname(challenge.getHost().getNickname())
                        .thumbnail(
                                challenge.getChallengeImage() == null
                                        ? null
                                        : FileDto.FileResponse.from(challenge.getChallengeImage())
                        )
                        .name(challenge.getName())
                        .isLiked(isLiked)
                        .likedCount(challenge.getLikeCount())
                        .fields(
                                challenge.getFields().stream()
                                        .map(FieldDto.FieldResponse::from)
                                        .collect(Collectors.toList())
                        )
                        .tags(
                                challenge.getTags().stream()
                                        .map(TagDto.TagResponse::from)
                                        .collect(Collectors.toList())
                        )
                        .job(challenge.getJob())
                        .startDate(challenge.getStartedAt())
                        .endDate(
                                challenge.getStartedAt()
                                        .plusWeeks(challenge.getDurationWeek())
                                        .minusDays(1)
                        )
                        .totalWeeks(challenge.getDurationWeek())
                        .state(challenge.getStatus())
                        .visibility(challenge.getVisibility())
                        .build();
            }
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class Me {

            @Schema(description = "유저 아이디", example = "1")
            private Long id;

            @Schema(description = "프로필 이미지")
            private FileDto.FileResponse profileImage;

            @Schema(description = "사용자 닉네임", example = "닉네임")
            private String nickname;

            @Schema(description = "진도율(완료 주차 / 전체 주차)", example = "77")
            private Integer progressRate;

            @Schema(description = "성공률(성공한 주차 / 현재 주차)", example = "80")
            private Integer successRate;

            @Schema(description = "성공 여부(successRate >= 70)", example = "true")
            private Boolean isSuccess;

            public static Me from(User user, Integer progressRate, Integer successRate) {
                int safeProgress = progressRate == null ? 0 : progressRate;
                int safeSuccess = successRate == null ? 0 : successRate;

                return Me.builder()
                        .id(user.getId())
                        .profileImage(
                                user.getProfileImage() == null
                                        ? null
                                        : FileDto.FileResponse.from(user.getProfileImage())
                        )
                        .nickname(user.getNickname())
                        .progressRate(safeProgress)
                        .successRate(safeSuccess)
                        .isSuccess(safeSuccess >= 70)
                        .build();
            }
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "챌린지 기록 응답")
    public static class RecordResponse {

        @Schema(description = "전체 챌린지 성공률(퍼센트)", example = "70")
        private Integer allSuccessRate;

        @Schema(description = "SOLO 기록")
        private RecordDetail soloRecord;

        @Schema(description = "VS 기록")
        private RecordDetail vsRecord;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "챌린지 기록 상세 정보 응답")
    public static class RecordDetail {

        @Schema(description = "성공률(퍼센트)", example = "70")
        private Integer successRate;

        @Schema(description = "시도 횟수", example = "30")
        private Integer attemptCount;

        @Schema(description = "성공 횟수", example = "21")
        private Integer successCount;

        @Schema(description = "실패 횟수", example = "9")
        private Integer failCount;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "게시글용 챌린지 목록 응답")
    public static class ChallengeToPostResponse {
        @Schema(description = "챌린지 아이디", example = "1")
        private Long challengeId;

        @Schema(description = "챌린지 이름", example = "챌린지 제목")
        private String name;

        @Schema(description = "직무", example = "직무")
        private String job;

        @Schema(description = "분야 목록")
        private List<FieldDto.FieldResponse> fields;

        @Schema(description = "태그 목록")
        private List<TagDto.TagResponse> tags;

        @Schema(description = "챌린지 시작일", example = "2026-01-01")
        private LocalDate startedAt;

        @Schema(description = "챌린지 기간(주)", example = "4")
        private Integer durationWeek;

        @Schema(description = "챌린지 모드(확인용)", example = "VS | SOLO", allowableValues = { "VS", "SOLO" })
        private ChallengeMode mode;

        public static ChallengeToPostResponse from(Challenge challenge) {
            return ChallengeToPostResponse.builder()
                    .challengeId(challenge.getId())
                    .name(challenge.getName())
                    .job(challenge.getJob())
                    .fields(
                            challenge.getFields().stream()
                                    .map(FieldDto.FieldResponse::from)
                                    .collect(Collectors.toList())
                    )
                    .tags(
                            challenge.getTags().stream()
                                    .map(TagDto.TagResponse::from)
                                    .collect(Collectors.toList())
                    )
                    .startedAt(challenge.getStartedAt())
                    .durationWeek(challenge.getDurationWeek())
                    .mode(challenge.getMode())
                    .build();
        }
    }
}