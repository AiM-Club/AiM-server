package targeter.aim.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.file.dto.FileDto;
import targeter.aim.domain.file.entity.ProfileImage;
import targeter.aim.domain.file.handler.FileHandler;
import targeter.aim.domain.label.entity.Field;
import targeter.aim.domain.label.entity.Tag;
import targeter.aim.domain.label.repository.FieldRepository;
import targeter.aim.domain.label.repository.TagRepository;
import targeter.aim.domain.user.dto.ProfileDto;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.ProfileQueryRepository;
import targeter.aim.domain.user.repository.UserRepository;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;
import targeter.aim.system.security.model.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final ProfileQueryRepository profileQueryRepository;

    private final TagRepository tagRepository;
    private final FieldRepository fieldRepository;

    private final FileHandler fileHandler;

    @Transactional(readOnly = true)
    public ProfileDto.ProfileResponse getProfile(Long targetUserId, UserDetails viewer) {

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RestException(ErrorCode.USER_NOT_FOUND));

        // 관심사 / 관심 분야
        List<String> interests = profileQueryRepository.findUserTagNames(targetUserId);
        List<String> fields = profileQueryRepository.findUserFieldNames(targetUserId);

        // 챌린지 기록
        ProfileQueryRepository.Record overall = profileQueryRepository.calcOverallRecord(targetUserId);
        ProfileQueryRepository.Record solo = profileQueryRepository.calcSoloRecord(targetUserId);
        ProfileQueryRepository.Record vs = profileQueryRepository.calcVsRecord(targetUserId);

        boolean isMine = viewer != null && viewer.getUser().getId().equals(targetUserId);

        return ProfileDto.ProfileResponse.builder()
                .userId(target.getId())
                .loginId(target.getLoginId())
                .nickname(target.getNickname())
                .tier(ProfileDto.TierResponse.builder()
                        .name(target.getTier().getName())
                        .build())
                .level(target.getLevel())
                .profileImage(
                        target.getProfileImage() == null
                                ? null
                                : FileDto.FileResponse.from(target.getProfileImage())
                )
                .interests(interests)
                .fields(fields)
                .overall(toRecordDto(overall))
                .solo(toRecordDto(solo))
                .vs(toRecordDto(vs))
                .isMine(isMine)
                .build();
    }

    @Transactional
    public ProfileDto.ProfileResponse updateMyProfile(ProfileDto.ProfileUpdateRequest request, UserDetails viewer) {
        if (viewer == null) {
            throw new RestException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }

        User me = userRepository.findById(viewer.getUser().getId())
                .orElseThrow(() -> new RestException(ErrorCode.USER_NOT_FOUND));

        if (request.getNickname() != null) {
            String nickname = request.getNickname().trim();
            if (nickname.isBlank() || nickname.length() > 10) {
                throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST, "닉네임은 1~10글자여야 합니다.");
            }
            if (!nickname.equals(me.getNickname()) && userRepository.existsByNickname(nickname)) {
                throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST, "이미 사용 중인 닉네임입니다.");
            }
            me.setNickname(nickname);
        }

        if (request.getInterests() != null) {
            Set<Tag> nextTags = upsertTags(request.getInterests());
            me.getTags().clear();
            me.getTags().addAll(nextTags);
        }

        if (request.getFields() != null) {
            Set<Field> nextFields = resolveFields(request.getFields());
            me.getFields().clear();
            me.getFields().addAll(nextFields);
        }

        if (request.getProfileImage() != null && !request.getProfileImage().isEmpty()) {
            replaceProfileImage(me, request.getProfileImage());
        }

        return getProfile(me.getId(), viewer);
    }

    private void replaceProfileImage(User user, MultipartFile file) {
        if (file.getSize() > 10L * 1024 * 1024) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST, "프로필 이미지는 10MB 이하만 업로드 가능합니다.");
        }

        ProfileImage prev = user.getProfileImage();
        if (prev != null) {
            fileHandler.deleteIfExists(prev);
            user.setProfileImage(null);
        }

        ProfileImage next = ProfileImage.from(file);
        next.setUser(user);
        user.setProfileImage(next);
        fileHandler.saveFile(file, next);
    }

    private Set<Tag> upsertTags(List<String> rawNames) {
        List<String> names = normalizeNames(rawNames, 30);

        if (names.isEmpty()) {
            return new HashSet<>();
        }

        List<Tag> existing = tagRepository.findAllByNameIn(names);
        Map<String, Tag> byName = existing.stream()
                .collect(Collectors.toMap(t -> t.getName().toLowerCase(), t -> t));

        List<Tag> toSave = new ArrayList<>();
        for (String n : names) {
            String key = n.toLowerCase();
            if (!byName.containsKey(key)) {
                Tag newTag = Tag.builder().name(n).build();
                toSave.add(newTag);
            }
        }

        if (!toSave.isEmpty()) {
            List<Tag> saved = tagRepository.saveAll(toSave);
            saved.forEach(t -> byName.put(t.getName().toLowerCase(), t));
        }

        return names.stream()
                .map(n -> byName.get(n.toLowerCase()))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Field> resolveFields(List<String> rawNames) {
        List<String> names = normalizeNames(rawNames, 30);

        if (names.isEmpty()) {
            return new HashSet<>();
        }

        List<Field> foundFields = fieldRepository.findAllByNameIn(names);

        Set<String> found = foundFields.stream()
                .map(f -> f.getName().toLowerCase())
                .collect(Collectors.toSet());

        for (String n : names) {
            if (!found.contains(n.toLowerCase())) {
                throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST, "존재하지 않는 관심 분야입니다: " + n);
            }
        }

        return new LinkedHashSet<>(foundFields);
    }

    private List<String> normalizeNames(List<String> raw, int maxLen) {
        if (raw == null) return List.of();

        return raw.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.length() > maxLen ? s.substring(0, maxLen) : s)
                .distinct()
                .toList();
    }

    private ProfileDto.ChallengeRecord toRecordDto(ProfileQueryRepository.Record record) {
        long attempt = record.attempt();
        long success = record.success();
        long fail = attempt - success;
        int successRate = attempt == 0
                ? 0
                : (int) Math.round((success * 100.0) / attempt);

        return ProfileDto.ChallengeRecord.builder()
                .attemptCount(attempt)
                .successCount(success)
                .failCount(fail)
                .successRate(successRate)
                .build();
    }
}