package targeter.aim.domain.challenge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.challenge.dto.RankDto;
import targeter.aim.domain.challenge.entity.ChallengeMode;
import targeter.aim.domain.challenge.repository.ChallengeRankQueryRepository;
import targeter.aim.domain.file.dto.FileDto;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChallengeRankService {

    private final UserRepository userRepository;
    private final ChallengeRankQueryRepository challengeRankQueryRepository;

    @Transactional(readOnly = true)
    public List<RankDto.Top20RankResponse> getTop20Rank() {

        List<User> users = userRepository.findAllByOrderByLevelDescIdAsc(PageRequest.of(0, 20)).getContent();
        if (users.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = users.stream().map(User::getId).toList();

        Map<Long, ChallengeRankQueryRepository.Record> overallMap =
                challengeRankQueryRepository.calcRecordByUsers(userIds, null);

        Map<Long, ChallengeRankQueryRepository.Record> soloMap =
                challengeRankQueryRepository.calcRecordByUsers(userIds, ChallengeMode.SOLO);

        Map<Long, ChallengeRankQueryRepository.Record> vsMap =
                challengeRankQueryRepository.calcRecordByUsers(userIds, ChallengeMode.VS);

        Map<Long, Integer> rankByUserId = new HashMap<>();
        for (int i = 0; i < users.size(); i++) {
            rankByUserId.put(users.get(i).getId(), i + 1);
        }

        return users.stream()
                .map(u -> {
                    int rank = rankByUserId.get(u.getId());

                    RankDto.UserInfo userInfo = RankDto.UserInfo.builder()
                            .nickname(u.getNickname())
                            .profileImage(u.getProfileImage() == null ? null : FileDto.FileResponse.from(u.getProfileImage()))
                            .tier(RankDto.TierResponse.builder().name(u.getTier().getName()).build())
                            .level(u.getLevel())
                            .build();

                    RankDto.ChallengeRecord allRecord = toRecordDto(overallMap.get(u.getId()));

                    // 1~3위만 solo/vs 포함
                    RankDto.ChallengeRecord soloRecord = (rank <= 3) ? toRecordDto(soloMap.get(u.getId())) : null;
                    RankDto.ChallengeRecord vsRecord = (rank <= 3) ? toRecordDto(vsMap.get(u.getId())) : null;

                    return RankDto.Top20RankResponse.builder()
                            .rank(rank)
                            .userId(u.getId())
                            .userInfo(userInfo)
                            .allRecord(allRecord)
                            .soloRecord(soloRecord)
                            .vsRecord(vsRecord)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private RankDto.ChallengeRecord toRecordDto(ChallengeRankQueryRepository.Record record) {
        if (record == null) {
            return RankDto.ChallengeRecord.builder()
                    .attemptCount(0)
                    .successCount(0)
                    .failCount(0)
                    .successRate(0)
                    .build();
        }

        long attempt = record.attempt();
        long success = record.success();
        long fail = attempt - success;
        int successRate = attempt == 0 ? 0 : (int) Math.round((success * 100.0) / attempt);

        return RankDto.ChallengeRecord.builder()
                .attemptCount(attempt)
                .successCount(success)
                .failCount(fail)
                .successRate(successRate)
                .build();
    }
}
