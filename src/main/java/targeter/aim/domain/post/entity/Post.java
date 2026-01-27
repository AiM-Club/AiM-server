package targeter.aim.domain.post.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import targeter.aim.common.auditor.TimeStampedEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import targeter.aim.domain.challenge.entity.ChallengeMode;
import targeter.aim.domain.file.entity.PostImage;
import targeter.aim.domain.label.entity.Field;
import targeter.aim.domain.label.entity.Tag;
import targeter.aim.domain.user.entity.User;
import targeter.aim.domain.file.entity.PostAttachedFile;
import targeter.aim.domain.file.entity.PostAttachedImage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "post")
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends TimeStampedEntity {

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
    @Builder.Default
    private Integer likeCount = 0;

    @OneToOne(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private PostImage postImage;

    @ManyToMany
    @JoinTable(
            name = "POST_TAG_MAPPING",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    @ToString.Exclude
    private Set<Tag> tags = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "POST_FIELD_MAPPING",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "field_id")
    )
    @Builder.Default
    @ToString.Exclude
    private Set<Field> fields = new HashSet<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "thumbnail_image_id")
    private PostImage thumbnail;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostAttachedImage> attachedImages = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostAttachedFile> attachedFiles = new ArrayList<>();

    public void setThumbnail(PostImage image) {
        this.thumbnail = image;
        image.setPost(this);
    }

    public void addAttachedImage(PostAttachedImage image) {
        this.attachedImages.add(image);
        image.setPost(this);
    }

    public void addAttachedFile(PostAttachedFile file) {
        this.attachedFiles.add(file);
        file.setPost(this);
    }

    public void removeAttachedImage(PostAttachedImage image) {
        this.attachedImages.remove(image);
        image.setPost(null);
    }

    public void removeAttachedFile(PostAttachedFile file) {
        this.attachedFiles.remove(file);
        file.setPost(null);
    }

    public void addLikedCount() {
        this.likeCount++;
    }

    public void subtractLikedCount() {
        this.likeCount--;
    }
}
