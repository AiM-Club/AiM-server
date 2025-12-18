package targeter.aim.domain.user.file.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("IMAGE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TestImageFile extends AttachedFile {
}
