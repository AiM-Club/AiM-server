package targeter.aim.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.challenge.entity.ChallengeMode;
import targeter.aim.domain.file.dto.FileDto;
import targeter.aim.domain.label.dto.FieldDto;
import targeter.aim.domain.label.dto.TagDto;
import targeter.aim.domain.label.entity.Field;
import targeter.aim.domain.label.entity.Tag;
import targeter.aim.domain.post.entity.Post;
import targeter.aim.domain.user.dto.TierDto;
import targeter.aim.domain.user.dto.UserDto;
import targeter.aim.domain.user.entity.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public class PostDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "VS 모집글 목록 조회 조건")
    public static class ListSearchCondition {

        @Builder.Default
        @Schema(
                description = """
                        정렬 기준
                        - ALL  : 전체 챌린지
                        - VS   : VS 챌린지
                        - SOLO : SOLO 챌린지
                        """,
                example = "ALL",
                allowableValues = {"ALL", "VS", "SOLO"}
        )
        private PostFilterType filter = PostFilterType.ALL;

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
        private PostSortType sort = PostSortType.LATEST;

        @Schema(description = "검색 키워드 (제목 기준 포함 검색)", example = "개발")
        private String keyword;
    }

    public enum PostFilterType {
        ALL,
        VS,
        SOLO,
        VS_RECRUIT,
        COMMUNITY
    }

    public enum PostSortType {
        LATEST,        // 최신순
        OLDEST,        // 오래된순
        LIKED,         // 좋아요순
        TITLE          // 가나다순
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
    @Schema(description = "게시글 작성자 정보")
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
    @Schema(description = "VS모집글 목록 응답")
    public static class VSRecruitListResponse {
        @Schema(description = "VS모집글 게시글 아이디", example = "1")
        private Long postId;

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
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "VS 모집글 목록 페이지 응답")
    public static class VSRecruitPageResponse {
        private List<VSRecruitListResponse> content;
        private PageInfo page;

        public static VSRecruitPageResponse from(Page<VSRecruitListResponse> page) {
            return new VSRecruitPageResponse(
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
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "게시글 공통 목록 응답 DTO (QnA / 후기)")
    public static class PostListResponse {

        @Schema(description = "게시글 아이디", example = "1")
        private Long postId;

        @Schema(description = "썸네일 정보")
        private FileDto.FileResponse thumbnail;

        @Schema(description = "작성자 정보")
        private UserResponse user;

        @Schema(description = "시작일", example = "2026-01-01")
        private LocalDate startedAt;

        @Schema(description = "챌린지 기간(주)", example = "4")
        private Integer durationWeek;

        @Schema(description = "게시글 제목", example = "제목")
        private String name;

        @Schema(description = "분야 리스트")
        private List<String> fields;

        @Schema(description = "태그 리스트")
        private List<String> tags;

        @Schema(description = "직무")
        private String job;

        @Schema(description = "좋아요 여부")
        private Boolean isLiked;

        @Schema(description = "좋아요 수")
        private Integer likeCount;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "게시글 목록 페이지 응답")
    public static class PostPageResponse {
        private List<PostListResponse> content;
        private PageInfo page;

        public static PostPageResponse from(Page<PostListResponse> page) {
            return new PostPageResponse(
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
    @Schema(description = "HOT 게시글 카드 응답")
    public static class HotPostListResponse {

        @Schema(description = "게시글 ID", example = "1")
        private Long postId;

        @Schema(description = "작성자 정보(profile, nickname, tier값 참고)")
        private UserResponse user;

        @Schema(description = "시작일", example = "2026-01-01")
        private LocalDate startedAt;

        @Schema(description = "챌린지 기간(주)", example = "4")
        private Integer durationWeek;

        @Schema(description = "게시글 제목", example = "게시글 제목")
        private String title;

        @Schema(description = "분야 리스트", example = "[\"IT\", \"BUSINESS\"]")
        private List<String> fields;

        @Schema(description = "태그 리스트", example = "[\"태그1\", \"태그2\", \"태그3\"]")
        private List<String> tags;

        @Schema(description = "직무", example = "직무")
        private String job;

        @Schema(description = "좋아요 여부(비로그인 false)", example = "true | false")
        private Boolean liked;

        @Schema(description = "좋아요 수", example = "10")
        private Integer likeCount;

        @Schema(description = "챌린지 모드", example = "SOLO | VS")
        private ChallengeMode mode;

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "HOT 게시글 페이지 응답")
    public static class HotPostPageResponse {
        private List<HotPostListResponse> content;
        private PageInfo page;

        public static HotPostPageResponse from(Page<HotPostListResponse> page) {
            return new HotPostPageResponse(
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
    @Schema(description = "VS 챌린지 모집 게시글 생성 요청 DTO")
    public static class CreateChallengePostRequest {

        @Schema(description = "모집 게시글 썸네일 이미지")
        private MultipartFile thumbnail;

        @Schema(description = "모집 게시글 제목 (최대 15자)")
        private String title;

        @Schema(description = "챌린지 태그")
        private List<String> tags;

        @Schema(description = "챌린지 분야")
        private List<String> fields;

        @Schema(description = "직무명")
        private String job;

        @Schema(description = "챌린지 ID")
        private Long challengeId;

        @Schema(description = "챌린지 시작일", example = "2026-01-01")
        private LocalDate startedAt;

        @Schema(description = "챌린지 기간(주)", example = "4")
        private Integer durationWeek;

        @Schema(description = "모집 게시글 본문 내용")
        private String content;

        @Schema(description = "첨부 이미지 목록")
        private List<MultipartFile> attachedImages;

        @Schema(description = "첨부 파일 목록")
        private List<MultipartFile> attachedFiles;

        public Post toEntity() {
            return Post.builder()
                    .title(title)
                    .content(content)
                    .job(job)
                    .build();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "QnA/후기 생성 요청 DTO")
    public static class CreatePostRequest {

        @Schema(description = "QnA/후기 썸네일 이미지")
        private MultipartFile thumbnail;

        @Schema(description = "QnA/후기 제목 (최대 15자)")
        private String title;

        @Schema(description = "챌린지 태그")
        private List<String> tags;

        @Schema(description = "챌린지 분야")
        private List<String> fields;

        @Schema(description = "직무명")
        private String job;

        @Schema(description = "챌린지 시작일", example = "2026-01-01")
        private LocalDate startedAt;

        @Schema(description = "챌린지 기간(주)", example = "4")
        private Integer durationWeek;

        @Schema(description = "챌린지 ID")
        private Long challengeId;

        @Schema(description = "챌린지 모드")
        private ChallengeMode mode;

        @Schema(description = "Qna/후기 게시글 본문 내용")
        private String content;

        @Schema(description = "첨부 이미지 목록")
        private List<MultipartFile> attachedImages;

        @Schema(description = "첨부 파일 목록")
        private List<MultipartFile> attachedFiles;

        public Post toEntity() {
            return Post.builder()
                    .title(title)
                    .content(content)
                    .job(job)
                    .build();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "게시글 생성 응답 DTO")
    public static class CreatePostResponse {

        @Schema(description = "게시글 id", example = "1")
        private Long postId;

        public static CreatePostResponse from(Post post) {
            return CreatePostResponse.builder()
                    .postId(post.getId())
                    .build();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "VS 챌린지 모집 게시글 상세 조회 응답 DTO")
    public static class PostVsDetailResponse {
        @Schema(description = "VS 챌린지 아이디", example = "1")
        private Long challengeId;

        @Schema(description = "VS 챌린지 이름", example = "아침 루틴 챌린지")
        private String challengeName;

        @Schema(description = "작성자 아이디", example = "1")
        private Long writerId;

        @Schema(description = "작성자 닉네임", example = "닉네임")
        private String nickname;

        @Schema(description = "썸네일 이미지")
        private FileDto.FileResponse thumbnail;

        @Schema(description = "모집글 제목")
        private String title;

        @Schema(description = "태그 리스트")
        private List<TagDto.TagResponse> tags;

        @Schema(description = "분야 리스트")
        private List<FieldDto.FieldResponse> fields;

        @Schema(description = "직무")
        private String job;

        @Schema(description = "챌린지 시작일")
        private LocalDate startDate;

        @Schema(description = "총 진행 기간(주)")
        private Integer totalWeeks;

        @Schema(description = "좋아요 여부")
        private Boolean isLiked;

        @Schema(description = "좋아요 수")
        private Integer likeCount;

        @Schema(description = "모집글 본문 내용")
        private String content;

        @Schema(description = "첨부 이미지 목록")
        private List<FileDto.FileResponse> attachedImages;

        @Schema(description = "첨부 파일 목록")
        private List<FileDto.FileResponse> attachedFiles;

        public static PostVsDetailResponse from(
                Post post,
                boolean isLiked
        ) {
            return PostVsDetailResponse.builder()
                    .challengeId(post.getChallenge().getId())
                    .challengeName(post.getChallenge().getName())
                    .writerId(post.getUser().getId())
                    .nickname(post.getUser().getNickname())
                    .thumbnail(post.getThumbnail() == null
                            ? null
                            : FileDto.FileResponse.from(post.getThumbnail()))
                    .title(post.getTitle())
                    .tags(post.getTags().stream()
                            .map(TagDto.TagResponse::from)
                            .toList())
                    .fields(post.getFields().stream()
                            .map(FieldDto.FieldResponse::from)
                            .toList())
                    .job(post.getJob())
                    .startDate(post.getChallenge().getStartedAt())
                    .totalWeeks(post.getChallenge().getDurationWeek())
                    .isLiked(isLiked)
                    .likeCount(post.getLikeCount())
                    .content(post.getContent())
                    .attachedImages(post.getAttachedImages().stream()
                            .map(FileDto.FileResponse::from)
                            .toList())
                    .attachedFiles(post.getAttachedFiles().stream()
                            .map(FileDto.FileResponse::from)
                            .toList())
                    .build();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "Q&A/후기 게시글 상세 조회 응답 DTO")
    public static class PostDetailResponse {
        @Schema(description = "VS 챌린지 아이디", example = "1")
        private Long challengeId;

        @Schema(description = "VS 챌린지 이름", example = "아침 루틴 챌린지")
        private String challengeName;

        @Schema(description = "작성자 아이디", example = "1")
        private Long writerId;

        @Schema(description = "작성자 닉네임", example = "닉네임")
        private String nickname;

        @Schema(description = "썸네일 이미지")
        private FileDto.FileResponse thumbnail;

        @Schema(description = "모집글 제목")
        private String title;

        @Schema(description = "태그 리스트")
        private List<TagDto.TagResponse> tags;

        @Schema(description = "분야 리스트")
        private List<FieldDto.FieldResponse> fields;

        @Schema(description = "직무")
        private String job;

        @Schema(description = "챌린지 시작일")
        private LocalDate startDate;

        @Schema(description = "총 진행 기간(주)")
        private Integer totalWeeks;

        @Schema(description = "챌린지 모드", example = "VS | SOLO")
        private ChallengeMode mode;

        @Schema(description = "좋아요 여부")
        private Boolean isLiked;

        @Schema(description = "좋아요 수")
        private Integer likeCount;

        @Schema(description = "모집글 본문 내용")
        private String content;

        @Schema(description = "첨부 이미지 목록")
        private List<FileDto.FileResponse> attachedImages;

        @Schema(description = "첨부 파일 목록")
        private List<FileDto.FileResponse> attachedFiles;

        public static PostDetailResponse from(Post post, boolean isLiked) {
            return PostDetailResponse.builder()
                    .challengeId(post.getChallenge().getId())
                    .challengeName(post.getChallenge().getName())
                    .writerId(post.getUser().getId())
                    .nickname(post.getUser().getNickname())
                    .thumbnail(post.getThumbnail() == null
                            ? null
                            : FileDto.FileResponse.from(post.getThumbnail()))
                    .title(post.getTitle())
                    .tags(post.getTags().stream()
                            .map(TagDto.TagResponse::from)
                            .toList())
                    .fields(post.getFields().stream()
                            .map(FieldDto.FieldResponse::from)
                            .toList())
                    .job(post.getJob())
                    .startDate(post.getChallenge().getStartedAt())
                    .totalWeeks(post.getChallenge().getDurationWeek())
                    .mode(post.getChallenge().getMode())
                    .isLiked(isLiked)
                    .likeCount(post.getLikeCount())
                    .content(post.getContent())
                    .attachedImages(post.getAttachedImages().stream()
                            .map(FileDto.FileResponse::from)
                            .toList())
                    .attachedFiles(post.getAttachedFiles().stream()
                            .map(FileDto.FileResponse::from)
                            .toList())
                    .build();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "HOT 후기글 목록 조회 응답")
    public static class HotReviewResponse {
        @Schema(description = "후기글 아이디", example = "1")
        private Long postId;

        @Schema(description = "후기글 제목", example = "게시글 제목")
        private String title;

        @Schema(description = "좋아요 수", example = "1")
        private Integer likeCount;

        public static HotReviewResponse from(Post post) {
            return HotReviewResponse.builder()
                    .postId(post.getId())
                    .title(post.getTitle())
                    .likeCount(post.getLikeCount())
                    .build();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "HOT VS 모집글 응답 DTO")
    public static class HotVsPostResponse {

        @Schema(description = "게시글 ID", example = "1")
        private Long postId;

        @Schema(description = "모집글 제목", example = "VS 챌린지 모집합니다")
        private String title;

        @Schema(description = "분야 리스트", example = "[\"IT\", \"BUSINESS\"]")
        private List<String> fields;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "QnA/후기 수정 요청 DTO")
    public static class UpdatePostRequest {

        @Schema(description = "QnA/후기 썸네일 이미지")
        private MultipartFile thumbnail;

        @Schema(description = "QnA/후기 제목 (최대 15자)")
        private String title;

        @Schema(description = "챌린지 태그")
        private List<String> tags;

        @Schema(description = "챌린지 분야")
        private List<String> fields;

        @Schema(description = "직무명")
        private String job;

        @Schema(description = "챌린지 시작일", example = "2026-01-01")
        private LocalDate startedAt;

        @Schema(description = "챌린지 기간(주)", example = "4")
        private Integer durationWeek;

        @Schema(description = "챌린지 ID")
        private Long challengeId;

        @Schema(description = "챌린지 모드", example = "VS | SOLO")
        private ChallengeMode mode;

        @Schema(description = "Qna/후기 게시글 본문 내용")
        private String content;

        @Schema(description = "첨부 이미지 목록")
        private List<MultipartFile> attachedImages;

        @Schema(description = "첨부 파일 목록")
        private List<MultipartFile> attachedFiles;

        public void applyTo(Post post, Set<Tag> resolvedTags, Set<Field> resolvedFields) {
            if (title != null) {
                post.setTitle(title);
            }
            if (tags != null) {
                post.getTags().clear();
                if (resolvedTags != null) post.getTags().addAll(resolvedTags);
            }
            if (fields != null) {
                post.getFields().clear();
                if (resolvedFields != null) post.getFields().addAll(resolvedFields);
            }
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "게시글 아이디 응답 DTO")
    public static class PostIdResponse {
        @Schema(description = "게시글 id", example = "1")
        private Long postId;

        public static PostDto.PostIdResponse from(Post post) {
            return PostIdResponse.builder()
                    .postId(post.getId())
                    .build();
        }
    }
}