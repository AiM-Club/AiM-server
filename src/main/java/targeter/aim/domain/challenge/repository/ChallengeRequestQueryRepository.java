package targeter.aim.domain.challenge.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import targeter.aim.domain.challenge.dto.ChallengeRequestDto;
import targeter.aim.domain.challenge.entity.ApplyStatus;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.challenge.entity.ChallengeRequest;
import targeter.aim.domain.challenge.entity.QChallenge;
import targeter.aim.domain.label.dto.FieldDto;
import targeter.aim.domain.label.entity.Field;
import targeter.aim.domain.label.entity.QField;
import targeter.aim.domain.user.entity.User;
import targeter.aim.system.security.model.UserDetails;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static targeter.aim.domain.challenge.entity.QChallenge.challenge;
import static targeter.aim.domain.challenge.entity.QChallengeRequest.challengeRequest;
import static targeter.aim.domain.label.entity.QField.field;

@Repository
@RequiredArgsConstructor
public class ChallengeRequestQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<ChallengeRequestDto.RequestListResponse> paginateVsRequests(
            UserDetails userDetails,
            Pageable pageable,
            ChallengeRequestDto.RequestListCondition condition
    ) {
        return paginateByKeyword(userDetails, pageable, condition, null);
    }

    public Page<ChallengeRequestDto.RequestListResponse> paginateByKeyword(
            UserDetails userDetails,
            Pageable pageable,
            ChallengeRequestDto.RequestListCondition condition,
            String keyword
    ) {
        if (userDetails == null || userDetails.getUser() == null) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        Long hostId = userDetails.getUser().getId();

        // 1) base query (컬렉션 fetch join 금지 -> fields는 별도 조회)
        JPAQuery<ChallengeRequest> baseQuery = buildBaseQuery(hostId, keyword);
        applySorting(baseQuery, condition);

        List<ChallengeRequest> requests = baseQuery
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2) count query
        Long total = buildCountQuery(hostId, keyword).fetchOne();
        long totalElements = total == null ? 0 : total;

        if (requests.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, totalElements);
        }

        // 3) challengeId 모아서 fields를 한번에 조회
        List<Long> challengeIds = requests.stream()
                .map(r -> r.getChallenge().getId())
                .distinct()
                .toList();

        Map<Long, List<FieldDto.FieldResponse>> fieldMap = fetchFieldMap(challengeIds);

        // 4) DTO 조립 (challenge.getFields() 호출 안 함 -> N+1 방지)
        List<ChallengeRequestDto.RequestListResponse> content = requests.stream()
                .map(r -> toDto(r, fieldMap))
                .toList();

        return new PageImpl<>(content, pageable, totalElements);
    }

    private JPAQuery<ChallengeRequest> buildBaseQuery(
            Long hostId,
            String keyword) {
        BooleanExpression keywordPredicate = keywordCondition(keyword);

        return queryFactory
                .selectFrom(challengeRequest)
                .join(challengeRequest.requester).fetchJoin()
                .join(challengeRequest.requester.tier).fetchJoin()
                .leftJoin(challengeRequest.requester.profileImage).fetchJoin()
                .join(challengeRequest.challenge, challenge).fetchJoin()
                .where(
                        challengeRequest.applier.id.eq(hostId),
                        challengeRequest.applyStatus.eq(ApplyStatus.PENDING),
                        keywordPredicate
                );
    }

    private JPAQuery<Long> buildCountQuery(
            Long hostId,
            String keyword) {
        BooleanExpression keywordPredicate = keywordCondition(keyword);

        return queryFactory
                .select(challengeRequest.count())
                .from(challengeRequest)
                .join(challengeRequest.challenge, challenge)
                .where(
                        challengeRequest.applier.id.eq(hostId),
                        challengeRequest.applyStatus.eq(ApplyStatus.PENDING),
                        keywordPredicate
                );
    }

    private BooleanExpression keywordCondition(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;

        String k = keyword.trim();

        BooleanExpression inRequesterNickname = challengeRequest.requester.nickname.containsIgnoreCase(k);
        BooleanExpression inChallengeTitle = challenge.name.containsIgnoreCase(k);

        // fields 검색(존재 여부 서브쿼리)
        QChallenge subChallenge = new QChallenge("subChallenge");
        QField subField = new QField("subField");

        BooleanExpression inField = JPAExpressions
                .selectOne()
                .from(subChallenge)
                .join(subChallenge.fields, subField)
                .where(
                        subChallenge.id.eq(challenge.id),
                        subField.name.containsIgnoreCase(k)
                )
                .exists();

        return inRequesterNickname.or(inChallengeTitle).or(inField);
    }

    private void applySorting(JPAQuery<?> query, ChallengeRequestDto.RequestListCondition condition) {
        String sort = (condition == null || condition.getSort() == null) ? "LATEST" : condition.getSort();

        switch (sort) {
            case "OLDEST" -> query.orderBy(challengeRequest.createdAt.asc());
            case "TITLE" -> query.orderBy(challenge.name.asc(), challengeRequest.createdAt.desc());
            case "LATEST" -> query.orderBy(challengeRequest.createdAt.desc());
            default -> query.orderBy(challengeRequest.createdAt.desc());
        }
    }

    private Map<Long, List<FieldDto.FieldResponse>> fetchFieldMap(List<Long> challengeIds) {
        List<com.querydsl.core.Tuple> tuples = queryFactory
                .select(challenge.id, field)
                .from(challenge)
                .join(challenge.fields, field)
                .where(challenge.id.in(challengeIds))
                .fetch();

        return tuples.stream()
                .collect(Collectors.groupingBy(
                        t -> t.get(challenge.id),
                        Collectors.mapping(t -> FieldDto.FieldResponse.from(t.get(field)), Collectors.toList())
                ));
    }

    private ChallengeRequestDto.RequestListResponse toDto(
            ChallengeRequest r,
            Map<Long, List<FieldDto.FieldResponse>> fieldMap
    ) {
        User requester = r.getRequester();
        Challenge ch = r.getChallenge();

        ChallengeRequestDto.RequestedChallengeResponse challengeDto =
                ChallengeRequestDto.RequestedChallengeResponse.builder()
                        .name(ch.getName())
                        .fields(fieldMap.getOrDefault(ch.getId(), List.of()))
                        .build();

        return ChallengeRequestDto.RequestListResponse.builder()
                .id(r.getId())
                .requesterId(requester.getId())
                .requester(targeter.aim.domain.user.dto.UserDto.UserResponse.from(requester))
                .challenge(challengeDto)
                .build();
    }

    public Optional<ChallengeRequest> findByIdForUpdate(Long requestId) {
        ChallengeRequest cr = queryFactory
                .selectFrom(challengeRequest)
                .join(challengeRequest.requester).fetchJoin()
                .join(challengeRequest.applier).fetchJoin()
                .join(challengeRequest.challenge).fetchJoin()
                .where(challengeRequest.id.eq(requestId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne();

        return Optional.ofNullable(cr);
    }
}
