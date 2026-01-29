package targeter.aim.domain.challenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import targeter.aim.domain.challenge.entity.ApplyStatus;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.challenge.entity.ChallengeRequest;
import targeter.aim.domain.label.dto.FieldDto;
import targeter.aim.domain.user.dto.UserDto;

import java.util.List;
import java.util.stream.Collectors;

public class ChallengeRequestDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "VS 챌린지 초대 목록 조회 조건")
    public static class SendRequestResponse {
        @Schema(description = "생성된 요청 아이디", example = "1")
        private Long requestId;

        @Schema(description = "요청한 챌린지 아이디", example = "1")
        private Long challengeId;

        @Schema(description = "요청 상태", example = "PENDING")
        private ApplyStatus status;

        public static SendRequestResponse from(ChallengeRequest challengeRequest) {
            return SendRequestResponse.builder()
                    .requestId(challengeRequest.getId())
                    .challengeId(challengeRequest.getChallenge().getId())
                    .status(challengeRequest.getApplyStatus())
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "VS 챌린지 초대 목록 조회 조건")
    public static class RequestListCondition {
        @Builder.Default
        @Schema(
                description = """
                    정렬 기준
                    - LATEST   : 최신순
                    - OLDEST   : 오래된순
                    - TITLE    : 가나다순
                    """,
                example = "LATEST",
                allowableValues = {"LATEST", "OLDEST", "TITLE"}
        )
        private String sort = "LATEST";

        @Schema(description = "검색 키워드 (닉네임,제목,분야로 검색)", example = "개발")
        private String keyword;
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "VS 챌린지 초대 목록 페이지 응답")
    public static class ChallengeRequestPageResponse {
        private List<RequestListResponse> content;
        private PageInfo page;

        public static ChallengeRequestPageResponse from(Page<RequestListResponse> page) {
            return new ChallengeRequestPageResponse(
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
    @Schema(description = "신청한 VS 챌린지 조회 응답")
    public static class RequestedChallengeResponse {
        private String name;

        private List<FieldDto.FieldResponse> fields;

        public static RequestedChallengeResponse from(Challenge challenge) {
            return RequestedChallengeResponse.builder()
                    .name(challenge.getName())
                    .fields(challenge.getFields().stream()
                            .map(FieldDto.FieldResponse::from)
                            .collect(Collectors.toList()))
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "VS 챌린지 초대 목록 조회 응답")
    public static class RequestListResponse {
        @Schema(description = "리퀘스트 아이디", example = "1")
        private Long id;

        @Schema(description = "신청자 아이디", example = "1")
        private Long requesterId;

        @Schema(description = "챌린지 신청 유저 정보(nickname, tier, level, profileImage)")
        private UserDto.UserResponse requester;

        @Schema(description = "신청한 챌린지(challengeId, name, fields")
        private RequestedChallengeResponse challenge;

        public static RequestListResponse from(ChallengeRequest challengeRequest) {
            return RequestListResponse.builder()
                    .id(challengeRequest.getId())
                    .requesterId(challengeRequest.getRequester().getId())
                    .requester(UserDto.UserResponse.from(challengeRequest.getRequester()))
                    .challenge(RequestedChallengeResponse.from(challengeRequest.getChallenge()))
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "VS 챌린지 초대 승인 응답")
    public static class RequestAccessResponse {
        @Schema(description = "챌린지 아이디", example = "1")
        private Long challengeId;

        public static RequestAccessResponse from(ChallengeRequest challengeRequest) {
            return RequestAccessResponse.builder()
                    .challengeId(challengeRequest.getChallenge().getId())
                    .build();
        }
    }
}
