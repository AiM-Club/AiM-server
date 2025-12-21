package targeter.aim.domain.file.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.web.multipart.MultipartFile;
import targeter.aim.domain.user.entity.User;

import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@DiscriminatorValue("PROFILE_IMAGE")
public class ProfileImage extends AttachedFile {

    @OneToOne(fetch = FetchType.LAZY)
    private User user;

    public static ProfileImage from(MultipartFile file, String filePath) {
        if(file == null || file.isEmpty()) {
            return null;
        }

        return ProfileImage.builder()
                .uuid(UUID.randomUUID().toString())
                .handlingType(HandlingType.IMAGE)
                .fileName(file.getOriginalFilename())
                .size(file.getSize())
                .filePath(filePath)
                .build();
    }
}
