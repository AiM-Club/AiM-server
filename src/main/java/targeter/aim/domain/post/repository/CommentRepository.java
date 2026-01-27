package targeter.aim.domain.post.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import targeter.aim.domain.post.entity.Comment;
import targeter.aim.domain.post.entity.Post;

import java.util.List;


public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"user"})
    Page<Comment> findAllByPostAndParentIsNull(
            Post post,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user"})
    List<Comment> findAllByParent_Id(
            Long parentId,
            Sort sort
    );
}