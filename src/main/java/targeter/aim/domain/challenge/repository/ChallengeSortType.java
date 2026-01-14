package targeter.aim.domain.challenge.repository;

public enum ChallengeSortType {
    CREATED_AT,      // 최신순
    END_DATE,       // 오래된순
    TITLE,         // 가나다순
    ONGOING,       // 진행 중
    FINISHED ,      // 진행 완료
    LATEST,
    OLDEST
}