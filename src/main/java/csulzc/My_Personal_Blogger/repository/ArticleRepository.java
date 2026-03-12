package csulzc.My_Personal_Blogger.repository;

import csulzc.My_Personal_Blogger.domain.entity.Article;
import csulzc.My_Personal_Blogger.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ArticleRepository extends BaseRepository<Article, Long> {

    // 1. 根据作者查询（分页）
    Page<Article> findByAuthor(User author, Pageable pageable);

    // 2. 根据状态查询
    List<Article> findByStatus(Article.ArticleStatus status);

    // 3. 标题包含关键字
    Page<Article> findByTitleContaining(String keyword, Pageable pageable);

    // 4. 复杂查询：发布时间在之后，按点赞数排序
    @Query("SELECT a FROM Article a WHERE a.createdAt >= :since ORDER BY a.likeCount DESC")
    List<Article> findPopularArticlesSince(@Param("since") LocalDateTime since, Pageable pageable);

    // 5. 统计某作者的文章数
    long countByAuthor(User author);

    // 6. 更新文章状态
    @Modifying
    @Query("UPDATE Article a SET a.status = :status WHERE a.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") Article.ArticleStatus status);

    // 7. 批量查询（使用IN子句）
    @Query("SELECT a FROM Article a LEFT JOIN FETCH a.categories WHERE a.id IN :ids")
    List<Article> findByIdsWithCategories(@Param("ids") List<Long> ids);
}