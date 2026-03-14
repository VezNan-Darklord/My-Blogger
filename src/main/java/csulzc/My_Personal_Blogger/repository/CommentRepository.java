package csulzc.My_Personal_Blogger.repository;

import csulzc.My_Personal_Blogger.domain.entity.Comment;
import csulzc.My_Personal_Blogger.domain.entity.Article;
import csulzc.My_Personal_Blogger.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends BaseRepository<Comment, Long> {

    // 1. 查询文章的所有顶级评论（不是回复）
    @Query("SELECT c FROM Comment c WHERE c.article = :article AND c.parentComment IS NULL")
    Page<Comment> findByArticleAndParentCommentIsNull(Article article, Pageable pageable);

    // 2. 查询某个评论的所有回复
    @Query("SELECT c FROM Comment c WHERE c.parentComment = :parent")
    Page<Comment> findByParentComment(Comment parent, Pageable pageable);

    // 3. 查询用户的所有评论
    Page<Comment> findByCommenter(User commenter, Pageable pageable);

    // 4. 统计文章评论数
    long countByArticle(Article article);

    // 5. 批量删除文章的评论
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.article = :article")
    int deleteByArticle(@Param("article") Article article);
}

// TODO: 添加其测试类