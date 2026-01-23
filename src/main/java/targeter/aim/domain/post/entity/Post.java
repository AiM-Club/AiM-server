package targeter.aim.domain.post.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import targeter.aim.domain.challenge.entity.ChallengeMode;
import targeter.aim.domain.user.entity.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    private String job;

    @Column(name = "started_at", nullable = false)
    private LocalDate startedAt;

    @Column(name = "duration_week", nullable = false)
    private Integer durationWeek;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostType type;

    @Enumerated(EnumType.STRING)
    private ChallengeMode mode;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

}
