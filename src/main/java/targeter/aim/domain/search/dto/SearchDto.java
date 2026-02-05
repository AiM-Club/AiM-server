package targeter.aim.domain.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.domain.Page;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.entity.ChallengeMode;
import targeter.aim.domain.file.dto.FileDto;
import targeter.aim.domain.post.dto.PostDto;
import targeter.aim.domain.post.entity.PostType;
import targeter.aim.domain.user.dto.TierDto;
import targeter.aim.domain.user.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class SearchDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "전체 검색글 목록 조회 조건")
    public static class ListSearchCondition {
        @Builder.Default
        @Schema(
                description = """
                        정렬 기준
                        - LATEST   : 최신순
                        - OLDEST   : 오래된순
                        - LIKED    : 좋아요순
                        - TITLE    : 가나다순
                        """,
                example = "LATEST",
                allowableValues = {"LATEST", "OLDEST", "LIKED", "TITLE"}
        )
        private SortType sort = SortType.LATEST;

        @Schema(description = "검색 키워드 (제목 기준 포함 검색)", example = "개발")
        private String keyword;
    }

    public enum SortType {
        LATEST,        // 최신순
        OLDEST,        // 오래된순
        LIKED,         // 좋아요순
        TITLE          // 가나다순
    }

    public enum Type {
        CHALLENGE,  // 챌린지
        POST        // 게시글
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
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "글 작성자 정보")
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
    @Getter
    @Builder
    @Schema(description = "검색된 글 목록 응답")
    public static class SearchListResponse {
        @Schema(description = "글 종류", example = "CHALLENGE | POST")
        private Type type;

        @Schema(description = "챌린지/게시글 아이디", example = "1")
        private Long id;

        @Schema(
                description = """
                        게시글 종류
                        - VS_RECRUIT : VS 모집글
                        - Q_AND_A    : Q&A
                        - REVIEW     : 후기글
                        """,
                example = "VS_RECRUIT | Q_AND_A | REVIEW"
        )
        private PostType postType;

        @Schema(
                description = "연관된 챌린지 모드",
                example = "VS | SOLO"
        )
        private ChallengeMode challengeMode;

        @Schema(description = "썸네일 정보(uuid값 참고)")
        private FileDto.FileResponse thumbnail;

        @Schema(description = "작성자 정보")
        private UserResponse user;

        @Schema(description = "시작일", example = "2026-01-01")
        private LocalDate startedAt;

        @Schema(description = "챌린지 기간(주)", example = "4주")
        private Integer durationWeek;

        @Schema(description = "VS모집글 제목", example = "제목")
        private String name;

        @Schema(description = "분야 리스트", example = "[\"IT\", \"BUSINESS\"]")
        private List<String> fields;

        @Schema(description = "태그 리스트", example = "[\"태그1\", \"태그2\", \"태그3\"]")
        private List<String> tags;

        @Schema(description = "직무", example = "직무")
        private String job;

        @Schema(description = "좋아요 여부(좋아요 했으면 true)", example = "true | false")
        private Boolean isLiked;

        @Schema(description = "좋아요수", example = "1")
        private Integer likeCount;

        @Schema(description = "글 생성일", example = "ISO DateTime")
        private LocalDateTime createdAt;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "게시글 목록 페이지 응답")
    public static class SearchPageResponse {
        private List<SearchListResponse> content;
        private PostDto.PageInfo page;

        public static SearchPageResponse from(Page<SearchListResponse> page) {
            return new SearchPageResponse(
                    page.getContent(),
                    new PostDto.PageInfo(
                            page.getSize(),
                            page.getNumber(),
                            page.getTotalElements(),
                            page.getTotalPages()
                    )
            );
        }
    }
}