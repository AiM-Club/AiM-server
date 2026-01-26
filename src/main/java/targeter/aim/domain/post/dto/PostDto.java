package targeter.aim.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.file.dto.FileDto;
import targeter.aim.domain.user.dto.UserDto;

import java.time.LocalDate;
import java.util.List;

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
                    - LATEST   : 최신순
                    - OLDEST   : 오래된순
                    - LIKED    : 좋아요순
                    - TITLE    : 가나다순
                    """,
                example = "LATEST",
                allowableValues = {"LATEST", "OLDEST", "LIKED", "TITLE"}
        )
        private String sort = "LATEST";

        @Schema(description = "검색 키워드 (제목 기준 포함 검색)", example = "개발")
        private String keyword;
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
    @Getter
    @Builder
    @Schema(description = "VS모집글 목록 응답")
    public static class VSRecruitListResponse {
        @Schema(description = "VS모집글 게시글 아이디", example = "1")
        private Long postId;

        @Schema(description = "썸네일 정보(uuid값 참고)")
        private FileDto.FileResponse thumbnail;

        @Schema(description = "작성자 정보(profile, nickname, tier값 참고)")
        private UserDto.UserResponse user;

        @Schema(description = "시작일", example = "2026-01-01")
        private LocalDate startDate;

        @Schema(description = "챌린지 기간(주)", example = "4주")
        private String duration;

        @Schema(description = "VS모집글 제목", example = "제목")
        private String name;

        @Schema(description = "분야 리스트", example = "[\"IT\", \"BUSINESS\"]")
        private List<String> fields;

        @Schema(description = "태그 리스트", example = "[\"태그1\", \"태그2\", \"태그3\"]")
        private List<String> tags;

        @Schema(description = "직무", example = "직무")
        private String job;

        @Schema(description = "좋아요 여부(좋아요 했으면 true)", example = "true | false")
        private Boolean liked;

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
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "VS 챌린지 모집 게시글 생성 요청 DTO")
    public static class CreateChallengePostRequest {

        @Schema(description = "챌린지 ID")
        private Long challengeId;

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

        @Schema(description = "모집 게시글 본문 내용")
        private String contents;

        @Schema(description = "첨부 파일 목록")
        private List<MultipartFile> files;

        @Schema(description = "첨부 이미지 목록")
        private List<MultipartFile> images;
    }

}
