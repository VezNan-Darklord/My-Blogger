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
    Page<Comment> findByArticleAndParentCommentIsNull(@Param("article") Article article, Pageable pageable);

    // 2. 查询某个评论的所有回复
    @Query("SELECT c FROM Comment c WHERE c.parentComment = :parent")
    Page<Comment> findByParentComment(@Param("parent") Comment parent, Pageable pageable);

    // 3. 查询用户的所有评论
    Page<Comment> findByCommenter(User commenter, Pageable pageable);

    // 4. 统计文章评论数
    long countByArticle(Article article);

    // 在 CommentRepository.java 中添加
    List<Comment> findByArticle(Article article);


    // 5. 批量删除文章的评论
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Comment c WHERE c.article = :article")
    int deleteByArticle(@Param("article") Article article);

    // 批量删除指定ID列表的评论
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Comment c WHERE c.id IN :ids")
    int batchDeleteByIds(@Param("ids") List<Long> ids);

    // 批量更新评论审核状态
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Comment c SET c.isApproved = :approved WHERE c.id IN :ids")
    int batchUpdateApprovalStatus(@Param("ids") List<Long> ids, @Param("approved") boolean approved);

    long countByCommenter(User user);

    long countByIsApproved(boolean isApproved);
}