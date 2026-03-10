package csulzc.My_Personal_Blogger.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Date;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "articles")
public class Article
{
    // 基础属性

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String content = "";

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Article.ArticleStatus status = Article.ArticleStatus.DRAFT;

    public enum ArticleStatus
    {
        DRAFT, RELEASE, ARCHIVE
    }

    private int likeCount = 0;

    private int favoriteCount = 0;

    // 与其他类的关联关系（索引关系）

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "article_category",
            joinColumns = @JoinColumn(name = "article_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Category> categories = new HashSet<>();

    // 辅助方法

    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setArticle(this);
    }

    public void removeComment(Comment comment) {
        comments.remove(comment);
        comment.setArticle(null);
    }

    // Article.java 中修改
    public void addCategory(Category category) {
        if (category == null || categories.contains(category)) {
            return;
        }
        categories.add(category);

        // 安全地添加双向关系
        if (category.getArticles() != null && !category.getArticles().contains(this)) {
            category.getArticles().add(this);
        }
    }

    public void removeCategory(Category category) {
        if (category == null || !categories.contains(category)) {
            return;
        }
        categories.remove(category);

        // 安全地移除双向关系
        if (category.getArticles() != null) {
            category.getArticles().remove(this);
        }
    }
}
