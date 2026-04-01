package csulzc.My_Personal_Blogger.service;

import csulzc.My_Personal_Blogger.api.dto.category.CategoryDTO;
import csulzc.My_Personal_Blogger.api.dto.comment.CommentDTO;
import csulzc.My_Personal_Blogger.api.dto.user.UserProfileDTO;
import csulzc.My_Personal_Blogger.domain.entity.*;
import csulzc.My_Personal_Blogger.repository.*;
import csulzc.My_Personal_Blogger.api.dto.article.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ArticleService 测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestArticleService {
    @Autowired
    private ArticleService articleService;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private ArticleCreateRequest articleCreateRequest;
    private ArticleUpdateRequest articleUpdateRequest;
    private ArticleDetailDTO articleDetailDTO;
    private ArticleListItemDTO  articleListItemDTO;

    private Article testArticle;
    private Long testUserId;
    private Long testArticleId;

    @BeforeEach
    void setUp()
    {
        articleCreateRequest = ArticleCreateRequest.builder()
                .title("测试文章")
                .content("这是测试文章的内容")
                .summary("这是测试文章的摘要")
                .coverImage("https://example.com/cover.jpg")
                .categoryIds(Set.of(1L))
                .status(Article.ArticleStatus.DRAFT)
                .tags(List.of("测试", "文章"))
                .build();
        articleUpdateRequest = ArticleUpdateRequest.builder()
                .title("更新后的标题")
                .content("这是更新后的内容")
                .summary("这是更新后的摘要")
                .coverImage("https://example.com/cover.jpg")
                .categoryIds(Set.of(1L))
                .status(Article.ArticleStatus.DRAFT)
                .tags(List.of("更新", "文章"))
                .build();
        articleDetailDTO = ArticleDetailDTO.builder()
                .title("测试文章")
                .content("这是测试文章的内容")
                .summary("这是测试文章的摘要")
                .coverImage("https://example.com/cover.jpg")
                .author(UserProfileDTO.builder()
                        .id(1L)
                        .username("test")
                        .displayName("测试用户")
                        .avatar("https://example.com/avatar.jpg")
                        .bio("这是测试用户的简介")
                        .createdAt(LocalDateTime.now())
                        .articleCount(0L)
                        .followerCount(0L)
                        .build()
                )
                .categories(List.of(CategoryDTO.builder()
                        .id(1L)
                        .name("测试分类")
                        .description("这是测试分类的描述")
                        .build())
                )
                .tags(List.of("测试", "评论"))
                .status(Article.ArticleStatus.DRAFT)
                .likeCount(0)
                .favoriteCount(0)
                .commentCount(0)
                .isLiked(false)
                .isFavorite(false)
                .build();
        articleListItemDTO = ArticleListItemDTO.builder()
                .id(1L)
                .title("测试文章")
                .summary("这是测试文章的摘要")
                .coverImage("https://example.com/cover.jpg")
                .author(UserProfileDTO.builder()
                        .id(1L)
                        .username("test")
                        .displayName("测试用户")
                        .avatar("https://example.com/avatar.jpg")
                        .bio("这是测试用户的简介")
                        .createdAt(LocalDateTime.now())
                        .articleCount(0L)
                        .followerCount(0L)
                        .build()
                )
                .categories(List.of(CategoryDTO.builder()
                        .id(1L)
                        .name("测试分类")
                        .description("这是测试分类的描述")
                        .build())
                )
                .createdAt(LocalDateTime.now())
                .likeCount(0)
                .commentCount(0)
                .favoriteCount(0)
                .build();
        testArticle = Article.builder()
                .title("测试文章")
                .content("这是测试文章的内容")
                .summary("这是测试文章的摘要")
                .coverImage("https://example.com/cover.jpg")
                .status(Article.ArticleStatus.DRAFT)
                .likeCount(0)
                .favoriteCount(0)
                .build();
        testUserId = 1L;
        testArticleId = 1L;
    }

    @AfterEach
    void tearDown()
    {
        entityManager.clear();
    }

    // TODO: 继续添加测试用例
}
