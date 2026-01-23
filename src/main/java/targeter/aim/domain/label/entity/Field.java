package targeter.aim.domain.label.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import targeter.aim.common.auditor.TimeStampedEntity;
import targeter.aim.domain.challenge.entity.Challenge;
import targeter.aim.domain.post.entity.Post;
import targeter.aim.domain.user.entity.User;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "FIELD")
public class Field extends TimeStampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String name;

    @ManyToMany(mappedBy = "fields")
    @Builder.Default
    @ToString.Exclude
    private Set<User> users = new HashSet<>();

    @ManyToMany(mappedBy = "fields")
    @Builder.Default
    @ToString.Exclude
    private Set<Challenge> challenges = new HashSet<>();

    @ManyToMany(mappedBy = "fields")
    @Builder.Default
    @ToString.Exclude
    private Set<Post> posts = new HashSet<>();
}
