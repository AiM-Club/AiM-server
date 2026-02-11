package targeter.aim.system.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.common.auditor.TimeStampedEntity;
import targeter.aim.domain.ai.llm.dto.RoutePayload;
import targeter.aim.domain.challenge.dto.ChallengeDto;
import targeter.aim.domain.challenge.entity.*;
import targeter.aim.domain.challenge.repository.ChallengeMemberRepository;
import targeter.aim.domain.challenge.repository.ChallengeRepository;
import targeter.aim.domain.challenge.repository.WeeklyCommentRepository;
import targeter.aim.domain.challenge.repository.WeeklyProgressRepository;
import targeter.aim.domain.challenge.service.ChallengeRouteGenerationService;
import targeter.aim.domain.file.entity.*;
import targeter.aim.domain.file.handler.FileHandler;
import targeter.aim.domain.label.entity.Field;
import targeter.aim.domain.label.entity.Tag;
import targeter.aim.domain.label.repository.FieldRepository;
import targeter.aim.domain.label.repository.TagRepository;
import targeter.aim.domain.post.entity.Comment;
import targeter.aim.domain.post.entity.Post;
import targeter.aim.domain.post.entity.PostType;
import targeter.aim.domain.post.repository.CommentRepository;
import targeter.aim.domain.post.repository.PostRepository;
import targeter.aim.domain.user.entity.Tier;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.user.repository.TierRepository;
import targeter.aim.domain.user.repository.UserRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
@Order(3)
public class DummyInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileHandler fileHandler;
    private final TierRepository tierRepository;
    private final ChallengeRepository challengeRepository;
    private final ChallengeRouteGenerationService challengeRouteGenerationService;
    private final WeeklyProgressRepository weeklyProgressRepository;
    private final ChallengeMemberRepository challengeMemberRepository;
    private final FieldRepository fieldRepository;
    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final WeeklyCommentRepository weeklyCommentRepository;
    private final CommentRepository commentRepository;

    @PersistenceContext
    private EntityManager em;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());


    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!isDatabaseEmpty()) {
            log.info("Dummy seed skipped: database is not empty.");
            return;
        }

        seedUsers();
        seedChallenges();
        seedVsRecruitPosts();
        seedVsWeeklyComments();
        seedTimerRecords();
        seedCommunityPosts();
        seedCommunityComments();

        //seedUserRecords();

        log.info("Dummy seed completed.");
    }

    private boolean isDatabaseEmpty() {
        return userRepository.count() == 0;
    }

    private void seedUsers() throws Exception {
        ClassPathResource usersRes = new ClassPathResource("dummy/users.json");

        try (InputStream in = usersRes.getInputStream()) {
            UsersPayload payload = objectMapper.readValue(in, UsersPayload.class);

            for (UserItem u : payload.getUsers()) {

                Tier tier = tierRepository.findByName(
                        u.getTier() != null ? u.getTier() : "BRONZE"
                ).orElseThrow(() ->
                        new IllegalStateException("Tier not found: " + u.getTier())
                );

                Integer level = u.getLevel() != null ? u.getLevel() : 1;

                LocalDate birthday = null;
                if (u.getBirthday() != null && !u.getBirthday().isBlank()) {
                    birthday = LocalDate.parse(u.getBirthday());
                }

                User user = User.builder()
                        .loginId(u.getId())
                        .email(u.getId())
                        .password(passwordEncoder.encode(u.getPassword()))
                        .nickname(u.getNickname())
                        .birthday(birthday)
                        .gender(u.getGender())
                        .socialLogin(null)
                        .tier(tier)
                        .level(level)
                        .build();

                userRepository.save(user);

                if (u.getProfileImage() != null && !u.getProfileImage().isBlank()) {
                    saveProfileImage(user, u.getProfileImage());
                }
            }
        }
    }

    private void saveProfileImage(User user, String fileName) {
        try {
            String path = "dummy/images/users/" + fileName;
            ClassPathResource imageRes = new ClassPathResource(path);

            if (!imageRes.exists()) {
                log.warn("Profile image not found: {}", path);
                return;
            }

            byte[] bytes;
            try (InputStream in = imageRes.getInputStream()) {
                bytes = in.readAllBytes();
            }

            MultipartFile mf = new BytesMultipartFile("file", fileName, bytes);

            ProfileImage profileImage = ProfileImage.from(mf, user);
            user.setProfileImage(profileImage);

            fileHandler.saveFile(mf, profileImage);

            em.persist(profileImage);
            em.flush();

            log.info("Seeded profile image for user {}", user.getLoginId());

        } catch (Exception e) {
            log.error("Failed to seed profile image for user {}", user.getLoginId(), e);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class UsersPayload {
        private java.util.List<UserItem> users;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class UserItem {
        private String id;
        private String password;
        private String nickname;
        private String birthday;
        private targeter.aim.domain.user.entity.Gender gender;
        private String profileImage;
        private String tier;
        private Integer level;
    }

    private static class BytesMultipartFile implements MultipartFile {

        private final String name;
        private final String originalFilename;
        private final byte[] content;
        private final String contentType;


        private BytesMultipartFile(String name, String originalFilename, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.content = content;
            this.contentType = resolveContentType(originalFilename);
        }

        private String resolveContentType(String filename) {
            if (filename == null) return "application/octet-stream";
            String lower = filename.toLowerCase();
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".gif")) return "image/gif";
            if (lower.endsWith(".webp")) return "image/webp";
            return "application/octet-stream";
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            Files.write(dest.toPath(), content);
        }
    }

    private void seedChallenges() throws Exception {
        ClassPathResource res = new ClassPathResource("dummy/challenges.json");
        DummyChallengePayload payload =
                objectMapper.readValue(res.getInputStream(), DummyChallengePayload.class);

        int soloIndex = 0;

        for (SoloChallengeItem item : payload.getSoloChallenges()) {
            soloIndex++;

            User creator = userRepository.findByLoginId(item.getCreatorId())
                    .orElseThrow(() ->
                            new IllegalStateException("User not found: " + item.getCreatorId()));

            Challenge challenge = Challenge.builder()
                    .name(item.getName())
                    .mode(ChallengeMode.SOLO)
                    .status(ChallengeStatus.IN_PROGRESS)
                    .job(item.getJob())
                    .startedAt(item.getStartDate())
                    .createdAt(item.getStartDate().atStartOfDay())
                    .durationWeek(item.getDurationWeeks())
                    .host(creator)
                    .visibility(ChallengeVisibility.PUBLIC)
                    .build();

            challengeRepository.save(challenge);

            challengeMemberRepository.save(
                    ChallengeMember.builder()
                            .id(ChallengeMemberId.of(challenge, creator))
                            .role(MemberRole.HOST)
                            .result(null)
                            .build()
            );

            Field field = fieldRepository.findByName(item.getField())
                    .orElseThrow(() -> new IllegalStateException("Field not found"));

            challenge.getFields().add(field);

            List<WeeklyProgress> weeklyProgresses =
                    generateWeeklyProgressByAI(challenge, creator, item);

            if (soloIndex == 3) {
                WeeklyProgress week2 = weeklyProgresses.get(1);
                seedWeeklyProgressAttachment(week2, "solo3_week2_cert.jpg");
            }

            if (soloIndex == 4) {
                WeeklyProgress week5 = weeklyProgresses.get(4);
                seedWeeklyProgressAttachment(week5, "solo4_week5_cert.jpg");
            }
        }

        for (VsChallengeItem item : payload.getVsChallenges()) {

            User hostUser = userRepository.findByLoginId(item.getCreatorId())
                    .orElseThrow(() ->
                            new IllegalStateException("User not found: " + item.getCreatorId()));

            User memberUser = userRepository.findByLoginId(item.getOpponentId())
                    .orElseThrow(() ->
                            new IllegalStateException("User not found: " + item.getOpponentId()));

            Challenge challenge = Challenge.builder()
                    .name(item.getName())
                    .mode(ChallengeMode.VS)
                    .status(ChallengeStatus.IN_PROGRESS)
                    .job(item.getJob())
                    .startedAt(item.getStartDate())
                    .createdAt(item.getStartDate().atStartOfDay())
                    .durationWeek(item.getDurationWeeks())
                    .host(hostUser)
                    .visibility(ChallengeVisibility.PUBLIC)
                    .build();

            challengeRepository.save(challenge);

            Field field = fieldRepository.findByName(item.getField())
                    .orElseThrow(() -> new IllegalStateException("Field not found"));

            challenge.getFields().add(field);

            if (item.getTags() != null) {
                for (String tagName : item.getTags()) {

                    Tag tag = tagRepository.findByName(tagName)
                            .orElseGet(() ->
                                    tagRepository.save(
                                            Tag.builder().name(tagName).build()
                                    )
                            );

                    challenge.getTags().add(tag);
                }
            }

            ChallengeMember host = ChallengeMember.builder()
                    .id(ChallengeMemberId.of(challenge, hostUser))
                    .role(MemberRole.HOST)
                    .result(null)
                    .build();

            challengeMemberRepository.save(host);

            generateWeeklyProgressByAI(
                    challenge,
                    hostUser,
                    convertVsItemToSolo(item)
            );

            challengeMemberRepository.save(
                    ChallengeMember.builder()
                            .id(ChallengeMemberId.of(challenge, memberUser))
                            .role(MemberRole.MEMBER)
                            .result(null)
                            .build()
            );

            copyHostWeeklyProgressToMember(challenge, hostUser, memberUser);
        }

        log.info("Dummy challenge seed completed");
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DummyChallengePayload {
        private List<SoloChallengeItem> soloChallenges;
        private List<VsChallengeItem> vsChallenges;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SoloChallengeItem {
        private String creatorId;
        private String name;
        private List<String> tags;
        private String job;
        private String field;
        private LocalDate startDate;
        private Integer durationWeeks;
        private String requirement;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class VsChallengeItem {
        private String creatorId;
        private String opponentId;
        private String name;
        private List<String> tags;
        private String job;
        private String field;
        private LocalDate startDate;
        private Integer durationWeeks;
        private String requirement;
    }

    private List<WeeklyProgress> generateWeeklyProgressByAI(
            Challenge challenge,
            User user,
            SoloChallengeItem item
    ) {
        ChallengeDto.ChallengeCreateRequest fakeReq =
                ChallengeDto.ChallengeCreateRequest.builder()
                        .name(item.getName())
                        .tags(item.getTags())
                        .fields(List.of(item.getField()))
                        .job(item.getJob())
                        .durationWeek(item.getDurationWeeks())
                        .startedAt(item.getStartDate())
                        .userRequest(item.getRequirement())
                        .build();

        RoutePayload payload =
                challengeRouteGenerationService.generateRoute(fakeReq);

        int week = 1;
        List<WeeklyProgress> result = new java.util.ArrayList<>();

        for (RoutePayload.Week w : payload.getWeeks()) {
            WeeklyProgress wp = WeeklyProgress.builder()
                    .challenge(challenge)
                    .user(user)
                    .weekNumber(week++)
                    .title(w.getTitle())
                    .content(w.getContent())
                    .weeklyStatus(WeeklyStatus.PENDING)
                    .isComplete(false)
                    .elapsedTimeSeconds(0)
                    .build();

            weeklyProgressRepository.save(wp);
            result.add(wp);
        }

        return result;
    }

    private void copyHostWeeklyProgressToMember(
            Challenge challenge,
            User hostUser,
            User memberUser
    ) {
        List<WeeklyProgress> hostList =
                weeklyProgressRepository.findAllByChallengeAndUser(challenge, hostUser);

        for (WeeklyProgress src : hostList) {

            WeeklyProgress cloned = WeeklyProgress.builder()
                    .challenge(challenge)
                    .user(memberUser)
                    .weekNumber(src.getWeekNumber())
                    .title(src.getTitle())
                    .content(src.getContent())
                    .weeklyStatus(WeeklyStatus.PENDING)
                    .isComplete(false)
                    .elapsedTimeSeconds(0)
                    .build();

            weeklyProgressRepository.save(cloned);
        }
    }

    private SoloChallengeItem convertVsItemToSolo(VsChallengeItem item) {
        SoloChallengeItem solo = new SoloChallengeItem();
        solo.setName(item.getName());
        solo.setTags(List.of());
        solo.setField(item.getField());
        solo.setJob(item.getJob());
        solo.setStartDate(item.getStartDate());
        solo.setDurationWeeks(item.getDurationWeeks());
        solo.setRequirement("");
        return solo;
    }

    private void seedWeeklyProgressAttachment(
            WeeklyProgress weeklyProgress,
            String fileName
    ) {
        try {
            String path = "dummy/images/challenges/" + fileName;
            ClassPathResource res = new ClassPathResource(path);

            if (!res.exists()) {
                log.warn("Certification image not found: {}", path);
                return;
            }

            byte[] bytes;
            try (InputStream in = res.getInputStream()) {
                bytes = in.readAllBytes();
            }

            MultipartFile mf = new BytesMultipartFile("file", fileName, bytes);

            ChallengeProofImage proofImage =
                    ChallengeProofImage.from(mf, weeklyProgress);

            fileHandler.saveFile(mf, proofImage);
            em.persist(proofImage);

            log.info("Seeded challenge proof image: {}", fileName);

        } catch (Exception e) {
            log.error("Failed to seed challenge proof image", e);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class VsRecruitPayload {
        private List<VsRecruitItem> vsRecruitPosts;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class VsRecruitItem {
        private String authorId;
        private String title;
        private String thumbnail;
        private List<String> tags;
        private String field;
        private String job;
        private LocalDate startDate;
        private Integer durationWeeks;
        private String vsType;
        private String content;
        private Integer fontSize;
        private Boolean isBold;
        private Boolean hasAttachment;
        private Attachment attachment;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Attachment {
        private String fileName;
        private String description;
    }

    private void seedVsRecruitPosts() throws Exception {
        ClassPathResource res = new ClassPathResource("dummy/vs_posts.json");
        VsRecruitPayload payload =
                objectMapper.readValue(res.getInputStream(), VsRecruitPayload.class);

        for (VsRecruitItem item : payload.getVsRecruitPosts()) {

            User author = userRepository.findByLoginId(item.getAuthorId())
                    .orElseThrow(() -> new IllegalStateException("User not found: " + item.getAuthorId()));

            //  VS 챌린지 찾기
            List<Challenge> challenges = challengeRepository
                    .findAllByHostAndModeAndVisibility(
                            author,
                            ChallengeMode.VS,
                            ChallengeVisibility.PUBLIC
                    );

            Challenge challenge = challenges.stream()
                    .filter(c -> c.getStartedAt().equals(item.getStartDate()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "VS Challenge not found for post: " + item.getTitle()
                    ));


            Post post = Post.builder()
                    .user(author)
                    .challenge(challenge)
                    .type(PostType.VS_RECRUIT)
                    .title(item.getTitle())
                    .content(item.getContent())
                    .job(item.getJob())
                    .build();

            postRepository.save(post);

            // 분야
            Field field = fieldRepository.findByName(item.getField())
                    .orElseThrow(() -> new IllegalStateException("Field not found"));
            post.getFields().add(field);

            if (item.getTags() != null) {
                for (String tagName : item.getTags()) {

                    Tag tag = tagRepository.findByName(tagName)
                            .orElseGet(() -> {
                                Tag newTag = Tag.builder()
                                        .name(tagName)
                                        .build();
                                return tagRepository.save(newTag);
                            });

                    post.getTags().add(tag);
                }
            }

            // 썸네일
            if (item.getThumbnail() != null) {
                seedPostThumbnail(post, item.getThumbnail());
            }

            // 첨부 이미지
            if (Boolean.TRUE.equals(item.getHasAttachment())) {
                seedPostAttachment(post, item.getAttachment());
            }

            log.info("Seeded VS recruit post: {}", post.getTitle());
        }
    }

    private void seedPostAttachment(Post post, Attachment attachment) {
        try {
            String fileName = attachment.getFileName();
            String path = "dummy/images/posts/" + fileName;

            ClassPathResource res = new ClassPathResource(path);
            if (!res.exists()) {
                log.warn("Post attachment image not found: {}", path);
                return;
            }

            byte[] bytes;
            try (InputStream in = res.getInputStream()) {
                bytes = in.readAllBytes();
            }

            MultipartFile mf = new BytesMultipartFile("file", fileName, bytes);

            PostAttachedImage image =
                    PostAttachedImage.from(mf, post);

            fileHandler.saveFile(mf, image);
            em.persist(image);

            log.info("Seeded post attachment image: {}", fileName);

        } catch (Exception e) {
            log.error("Failed to seed post attachment image", e);
        }
    }

    private void seedPostThumbnail(Post post, String fileName) {
        try {
            String path = "dummy/images/posts/" + fileName;
            ClassPathResource res = new ClassPathResource(path);

            if (!res.exists()) {
                log.warn("Post thumbnail image not found: {}", path);
                return;
            }

            byte[] bytes;
            try (InputStream in = res.getInputStream()) {
                bytes = in.readAllBytes();
            }

            MultipartFile mf = new BytesMultipartFile("file", fileName, bytes);

            PostImage thumbnail = PostImage.from(mf, post);

            fileHandler.saveFile(mf, thumbnail);
            em.persist(thumbnail);

            log.info("Seeded post thumbnail image: {}", fileName);

        } catch (Exception e) {
            log.error("Failed to seed post thumbnail image", e);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class VsWeeklyCommentPayload {
        private List<VsWeeklyCommentItem> comments;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class VsWeeklyCommentItem {
        private String challengeName;
        private String pageOwnerId;
        private Integer week;
        private String authorId;
        private String content;
        private LocalDateTime createdAt;
        private List<VsWeeklyReplyItem> replies;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class VsWeeklyReplyItem {
        private String authorId;
        private String content;
        private LocalDateTime createdAt;
    }

    private void seedVsWeeklyComments() throws Exception {
        ClassPathResource res =
                new ClassPathResource("dummy/vs_weekly_comments.json");

        VsWeeklyCommentPayload payload =
                objectMapper.readValue(res.getInputStream(), VsWeeklyCommentPayload.class);

        for (VsWeeklyCommentItem item : payload.getComments()) {

            // pageOwner 조회
            User pageOwner = userRepository
                    .findByLoginId(item.getPageOwnerId())
                    .orElseThrow(() ->
                            new IllegalStateException("Page owner not found: " + item.getPageOwnerId())
                    );

            // VS 챌린지 찾기
            Challenge challenge = challengeRepository
                    .findAll().stream()
                    .filter(c -> c.getName().equals(item.getChallengeName()))
                    .filter(c -> c.getMode() == ChallengeMode.VS)
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalStateException("VS Challenge not found: " + item.getChallengeName())
                    );

            // WeeklyProgress 찾기
            WeeklyProgress weeklyProgress = weeklyProgressRepository
                    .findByChallengeAndUserAndWeekNumber(
                            challenge,
                            pageOwner,
                            item.getWeek()
                    )
                    .orElseThrow(() ->
                            new IllegalStateException("WeeklyProgress not found (week " + item.getWeek() + ")")
                    );

            // 댓글 작성자
            User author = userRepository.findByLoginId(item.getAuthorId())
                    .orElseThrow(() ->
                            new IllegalStateException("User not found: " + item.getAuthorId())
                    );

            java.lang.reflect.Field createdAtField =
                    TimeStampedEntity.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);

            // 부모 댓글
            WeeklyComment parent = WeeklyComment.builder()
                    .user(author)
                    .weeklyProgress(weeklyProgress)
                    .content(item.getContent())
                    .depth(0)
                    .build();

            createdAtField.set(parent, item.getCreatedAt());
            weeklyCommentRepository.save(parent);

            // 대댓글
            if (item.getReplies() != null) {
                for (VsWeeklyReplyItem reply : item.getReplies()) {

                    User replyUser = userRepository.findByLoginId(reply.getAuthorId())
                            .orElseThrow(() ->
                                    new IllegalStateException("User not found: " + reply.getAuthorId())
                            );

                    WeeklyComment child = WeeklyComment.builder()
                            .parentComment(parent)
                            .user(replyUser)
                            .weeklyProgress(weeklyProgress)
                            .content(reply.getContent())
                            .depth(1)
                            .build();

                    createdAtField.set(child, reply.getCreatedAt());
                    weeklyCommentRepository.save(child);
                }
            }
        }

        log.info("VS weekly comments seeded");
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TimerRecordPayload {
        private List<TimerRecordItem> records;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TimerRecordItem {
        private String challengeName;
        private String userId;
        private Integer week;
        private LocalDateTime startedAt;
        private LocalDateTime finishedAt;
        private Integer elapsedSeconds;
        private String status;
    }

    private void seedTimerRecords() throws Exception {

        ClassPathResource res =
                new ClassPathResource("dummy/timer_records.json");

        TimerRecordPayload payload =
                objectMapper.readValue(res.getInputStream(), TimerRecordPayload.class);

        for (TimerRecordItem item : payload.getRecords()) {

            // 유저 조회 (Id 기준)
            User user = userRepository.findByLoginId(item.getUserId())
                    .orElseThrow(() ->
                            new IllegalStateException("User not found: " + item.getUserId())
                    );

            // 챌린지 조회
            Challenge challenge = challengeRepository.findAll().stream()
                    .filter(c -> c.getName().equals(item.getChallengeName()))
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalStateException("Challenge not found: " + item.getChallengeName())
                    );

            // WeeklyProgress 조회
            WeeklyProgress weeklyProgress = weeklyProgressRepository
                    .findByChallengeAndUserAndWeekNumber(
                            challenge,
                            user,
                            item.getWeek()
                    )
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "WeeklyProgress not found: "
                                            + item.getChallengeName()
                                            + " / " + item.getUserId()
                                            + " / week " + item.getWeek()
                            )
                    );

            // 경과 시간 세팅
            weeklyProgress.setElapsedTimeSeconds(item.getElapsedSeconds());

            // 상태 세팅
            switch (item.getStatus()) {

                case "COMPLETE" -> {
                    weeklyProgress.setIsComplete(true);
                    weeklyProgress.setWeeklyStatus(WeeklyStatus.SUCCESS);
                }

                case "IN_PROGRESS" -> {
                    weeklyProgress.setIsComplete(false);
                    weeklyProgress.setWeeklyStatus(WeeklyStatus.PENDING);
                }

                case "PENDING" -> {
                    weeklyProgress.setIsComplete(false);
                    weeklyProgress.setWeeklyStatus(WeeklyStatus.PENDING);
                }
            }

            weeklyProgressRepository.save(weeklyProgress);
        }

        log.info("Timer records seeded");
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CommunityPostPayload {
        private List<CommunityPostItem> posts;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CommunityPostItem {
        private String boardType;   // Q_AND_A / REVIEW
        private String authorId;
        private String title;
        private String content;
        private LocalDateTime createdAt;
        private String thumbnail;
        private String attachedImage;
        private List<String> tags;
        private String field;
        private String job;
        private Integer fontSize;
        private Boolean isBold;
        private String challengeName;
    }

    private void seedCommunityPosts() throws Exception {

        ClassPathResource res =
                new ClassPathResource("dummy/community_posts.json");

        CommunityPostPayload payload =
                objectMapper.readValue(res.getInputStream(), CommunityPostPayload.class);

        java.lang.reflect.Field createdAtField =
                TimeStampedEntity.class.getDeclaredField("createdAt");
        createdAtField.setAccessible(true);

        for (CommunityPostItem item : payload.getPosts()) {

            User author = userRepository.findByLoginId(item.getAuthorId())
                    .orElseThrow(() ->
                            new IllegalStateException("User not found: " + item.getAuthorId())
                    );

            PostType type = switch (item.getBoardType()) {
                case "Q_AND_A" -> PostType.Q_AND_A;
                case "REVIEW" -> PostType.REVIEW;
                default -> throw new IllegalStateException("Invalid boardType");
            };

            Post post = Post.builder()
                    .user(author)
                    .type(type)
                    .title(item.getTitle())
                    .content(item.getContent())
                    .job(item.getJob())
                    .build();

            Challenge challenge = challengeRepository.findAll().stream()
                    .filter(c -> c.getName().equals(item.getChallengeName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Challenge not found"));

            post.setChallenge(challenge);

            postRepository.save(post);

            createdAtField.set(post, item.getCreatedAt());
            em.flush();

            Field field = fieldRepository.findByName(item.getField())
                    .orElseThrow(() -> new IllegalStateException("Field not found"));
            post.getFields().add(field);

            if (item.getTags() != null) {
                for (String tagName : item.getTags()) {

                    Tag tag = tagRepository.findByName(tagName)
                            .orElseGet(() ->
                                    tagRepository.save(
                                            Tag.builder().name(tagName).build()
                                    )
                            );

                    post.getTags().add(tag);
                }
            }

            if (item.getThumbnail() != null) {
                seedPostThumbnail(post, item.getThumbnail());
            }

            if (item.getAttachedImage() != null) {
                seedPostAttachmentImage(post, item.getAttachedImage());
            }

            log.info("Seeded community post: {}", post.getTitle());
        }

        log.info("Community posts seeded");
    }

    private void seedPostAttachmentImage(Post post, String fileName) {

        try {
            String path = "dummy/images/posts/" + fileName;
            ClassPathResource res = new ClassPathResource(path);

            if (!res.exists()) {
                log.warn("Post attachment image not found: {}", path);
                return;
            }

            byte[] bytes;
            try (InputStream in = res.getInputStream()) {
                bytes = in.readAllBytes();
            }

            MultipartFile mf =
                    new BytesMultipartFile("file", fileName, bytes);

            PostAttachedImage image =
                    PostAttachedImage.from(mf, post);

            fileHandler.saveFile(mf, image);
            em.persist(image);

        } catch (Exception e) {
            log.error("Failed to seed board attachment image", e);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CommunityReplyItem {
        private String authorId;
        private String content;
        private String attachedImage;
        private LocalDateTime createdAt;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CommunityCommentPayload {
        private List<CommunityCommentItem> comments;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CommunityCommentItem {
        private String postTitle;
        private String authorId;
        private String content;
        private LocalDateTime createdAt;
        private List<CommunityReplyItem> replies;
    }

    private void seedCommunityComments() throws Exception {

        ClassPathResource res =
                new ClassPathResource("dummy/community_comments.json");

        CommunityCommentPayload payload =
                objectMapper.readValue(res.getInputStream(), CommunityCommentPayload.class);

        java.lang.reflect.Field createdAtField =
                TimeStampedEntity.class.getDeclaredField("createdAt");
        createdAtField.setAccessible(true);

        for (CommunityCommentItem item : payload.getComments()) {

            Post post = postRepository.findAll().stream()
                    .filter(p -> p.getTitle().equals(item.getPostTitle()))
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalStateException("Post not found: " + item.getPostTitle())
                    );

            User author = userRepository.findByLoginId(item.getAuthorId())
                    .orElseThrow(() ->
                            new IllegalStateException("User not found: " + item.getAuthorId())
                    );

            Comment parent = Comment.builder()
                    .post(post)
                    .user(author)
                    .contents(item.getContent())
                    .depth(0)
                    .build();

            commentRepository.save(parent);
            createdAtField.set(parent, item.getCreatedAt());
            em.flush();

            // 대댓글
            if (item.getReplies() != null) {
                for (CommunityReplyItem replyItem : item.getReplies()) {

                    User replyAuthor = userRepository.findByLoginId(replyItem.getAuthorId())
                            .orElseThrow(() ->
                                    new IllegalStateException("User not found: " + replyItem.getAuthorId())
                            );

                    Comment child = Comment.builder()
                            .post(post)
                            .user(replyAuthor)
                            .contents(replyItem.getContent())
                            .parent(parent)
                            .depth(1)
                            .build();

                    commentRepository.save(child);
                    createdAtField.set(child, replyItem.getCreatedAt());

                    // 첨부 이미지
                    if (replyItem.getAttachedImage() != null) {
                        seedCommentAttachmentImage(child, replyItem.getAttachedImage());
                    }
                }
            }

            log.info("Seeded comment for post: {}", post.getTitle());
        }

        log.info("Community comments seeded");
    }

    private void seedCommentAttachmentImage(Comment comment, String fileName) {

        try {
            String path = "dummy/images/posts/" + fileName;
            ClassPathResource res = new ClassPathResource(path);

            if (!res.exists()) {
                log.warn("Comment attachment image not found: {}", path);
                return;
            }

            byte[] bytes;
            try (InputStream in = res.getInputStream()) {
                bytes = in.readAllBytes();
            }

            MultipartFile mf =
                    new BytesMultipartFile("file", fileName, bytes);

            CommentImage image =
                    CommentImage.from(mf, comment);

            fileHandler.saveFile(mf, image);
            em.persist(image);

        } catch (Exception e) {
            log.error("Failed to seed comment attachment image", e);
        }
    }

    private void seedUserRecords() {

        recordForUser("kimdoy26", 20, 40);
        recordForUser("parksoy24", 40, 60);
        recordForUser("jungsej28", 90, 120);
        recordForUser("yoongae22", 150, 150);
        recordForUser("ohharin31", 70, 100);
        recordForUser("kangmin25", 58, 100);
        recordForUser("jangwoo23", 88, 110);
        recordForUser("seoyerin27", 33, 55);
        recordForUser("baesua30", 120, 140);

        log.info("Dummy user records seeded");
    }

    private void recordForUser(String loginId, int successCount, int totalCount) {

        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + loginId));

        int failCount = totalCount - successCount;

        for (int i = 0; i < totalCount; i++) {

            Challenge challenge = Challenge.builder()
                    .name("R" + i)
                    .mode(ChallengeMode.SOLO)
                    .status(ChallengeStatus.COMPLETED)
                    .job("DUMMY")
                    .startedAt(LocalDate.now().minusWeeks(1))
                    .createdAt(LocalDateTime.now().minusWeeks(1))
                    .durationWeek(1)
                    .host(user)
                    .visibility(ChallengeVisibility.PRIVATE)
                    .build();

            challengeRepository.save(challenge);

            ChallengeResult result =
                    i < successCount
                            ? ChallengeResult.SUCCESS
                            : ChallengeResult.FAIL;

            ChallengeMember member = ChallengeMember.builder()
                    .id(ChallengeMemberId.of(challenge, user))
                    .role(MemberRole.HOST)
                    .result(result)
                    .build();

            challengeMemberRepository.save(member);
        }
    }


}
