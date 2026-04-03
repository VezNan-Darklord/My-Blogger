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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({ArticleService.class, CategoryService.class})
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

    @Autowired
    private UserRepository userRepository;

    private ArticleCreateRequest articleCreateRequest;
    private ArticleUpdateRequest articleUpdateRequest;
    private ArticleDetailDTO articleDetailDTO;
    private ArticleListItemDTO  articleListItemDTO;

    private Article testArticle;
    private Long testUserId;
    private Long testArticleId;
    private User testUser;
    private Category testCategory;

    @BeforeEach
    void setUp()
    {
        // 创建测试用户
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("password123")
                .displayName("测试用户")
                .status(User.UserStatus.ACTIVE)
                .build();
        entityManager.persist(testUser);
        testUserId = testUser.getId();

        // 创建测试分类
        testCategory = Category.builder()
                .name("测试分类")
                .description("这是测试分类的描述")
                .build();
        entityManager.persist(testCategory);

        // 创建测试文章
        testArticle = Article.builder()
                .title("测试文章")
                .content("这是测试文章的内容")
                .summary("这是测试文章的摘要")
                .coverImage("https://example.com/cover.jpg")
                .status(Article.ArticleStatus.DRAFT)
                .likeCount(0)
                .favoriteCount(0)
                .author(testUser)
                .build();
        testArticle.addCategory(testCategory);
        entityManager.persist(testArticle);
        testArticleId = testArticle.getId();

        // 准备 DTO 对象
        articleCreateRequest = ArticleCreateRequest.builder()
                .title("测试文章")
                .content("这是测试文章的内容")
                .summary("这是测试文章的摘要")
                .coverImage("https://example.com/cover.jpg")
                .categoryIds(Set.of(testCategory.getId()))
                .status(Article.ArticleStatus.DRAFT)
                .tags(List.of("测试", "文章"))
                .build();
        articleUpdateRequest = ArticleUpdateRequest.builder()
                .title("更新后的标题")
                .content("这是更新后的内容")
                .summary("这是更新后的摘要")
                .coverImage("https://example.com/cover.jpg")
                .categoryIds(Set.of(testCategory.getId()))
                .status(Article.ArticleStatus.DRAFT)
                .tags(List.of("更新", "文章"))
                .build();
        articleDetailDTO = ArticleDetailDTO.builder()
                .title("测试文章")
                .content("这是测试文章的内容")
                .summary("这是测试文章的摘要")
                .coverImage("https://example.com/cover.jpg")
                .author(UserProfileDTO.builder()
                        .id(testUserId)
                        .username("testuser")
                        .displayName("测试用户")
                        .avatar("https://example.com/avatar.jpg")
                        .bio("这是测试用户的简介")
                        .createdAt(LocalDateTime.now())
                        .articleCount(0L)
                        .followerCount(0L)
                        .build()
                )
                .categories(List.of(CategoryDTO.builder()
                        .id(testCategory.getId())
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
                .id(testArticleId)
                .title("测试文章")
                .summary("这是测试文章的摘要")
                .coverImage("https://example.com/cover.jpg")
                .author(UserProfileDTO.builder()
                        .id(testUserId)
                        .username("testuser")
                        .displayName("测试用户")
                        .avatar("https://example.com/avatar.jpg")
                        .bio("这是测试用户的简介")
                        .createdAt(LocalDateTime.now())
                        .articleCount(0L)
                        .followerCount(0L)
                        .build()
                )
                .categories(List.of(CategoryDTO.builder()
                        .id(testCategory.getId())
                        .name("测试分类")
                        .description("这是测试分类的描述")
                        .build())
                )
                .createdAt(LocalDateTime.now())
                .likeCount(0)
                .commentCount(0)
                .favoriteCount(0)
                .build();
    }

    @AfterEach
    void tearDown()
    {
        entityManager.clear();
    }

    @Test
    @Order(1)
    @DisplayName("测试创建文章 - 成功")
    void testCreateArticle_Success() {
        // Given - 准备数据（已在 setUp 中准备）

        // When - 执行创建操作
        ArticleDetailDTO result = articleService.createArticle(articleCreateRequest, testUserId);

        // Then - 验证结果
        assertNotNull(result);
        assertEquals("测试文章", result.getTitle());
        assertEquals("这是测试文章的内容", result.getContent());
        assertEquals(Article.ArticleStatus.DRAFT, result.getStatus());
        assertThat(result.getCategories()).hasSize(1);
        assertEquals("测试分类", result.getCategories().get(0).getName());
    }

    @Test
    @Order(2)
    @DisplayName("测试更新文章 - 成功")
    void testUpdateArticle_Success() {
        // Given
        Long articleId = testArticleId;

        // When - 执行更新操作
        ArticleDetailDTO result = articleService.updateArticle(articleId, articleUpdateRequest, testUserId);

        // Then - 验证结果
        assertNotNull(result);
        assertEquals("更新后的标题", result.getTitle());
        assertEquals("这是更新后的内容", result.getContent());
        assertEquals("这是更新后的摘要", result.getSummary());
    }

    @Test
    @Order(3)
    @DisplayName("测试更新文章 - 文章不存在")
    void testUpdateArticle_NotFound() {
        // Given
        Long nonExistentId = 999L;

        // When & Then - 应该抛出异常
        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> {
            articleService.updateArticle(nonExistentId, articleUpdateRequest, testUserId);
        });
    }

    @Test
    @Order(4)
    @DisplayName("测试更新文章 - 无权限")
    void testUpdateArticle_NoPermission() {
        // Given
        Long anotherUserId = 999L;
        Long articleId = testArticleId;

        // When & Then - 应该抛出权限异常
        assertThrows(RuntimeException.class, () -> {
            articleService.updateArticle(articleId, articleUpdateRequest, anotherUserId);
        });
    }

    @Test
    @Order(5)
    @DisplayName("测试发布文章 - 成功")
    void testPublishArticle_Success() {
        // Given
        Long articleId = testArticleId;

        // When - 执行发布操作
        ArticleDetailDTO result = articleService.publishArticle(articleId, testUserId);

        // Then - 验证状态已变更
        assertNotNull(result);
        assertEquals(Article.ArticleStatus.RELEASE, result.getStatus());
    }

    @Test
    @Order(6)
    @DisplayName("测试发布文章 - 文章不存在")
    void testPublishArticle_NotFound() {
        // Given
        Long nonExistentId = 999L;

        // When & Then
        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> {
            articleService.publishArticle(nonExistentId, testUserId);
        });
    }

    @Test
    @Order(7)
    @DisplayName("测试归档文章 - 成功")
    void testArchiveArticle_Success() {
        // Given
        Long articleId = testArticleId;

        // When - 执行归档操作
        ArticleDetailDTO result = articleService.archiveArticle(articleId, testUserId);

        // Then - 验证状态已变更
        assertNotNull(result);
        assertEquals(Article.ArticleStatus.ARCHIVE, result.getStatus());
    }

    @Test
    @Order(8)
    @DisplayName("测试归档文章 - 无权限")
    void testArchiveArticle_NoPermission() {
        // Given
        Long articleId = testArticleId;
        Long anotherUserId = 999L;

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            articleService.archiveArticle(articleId, anotherUserId);
        });
    }

    @Test
    @Order(9)
    @DisplayName("测试根据 ID 获取文章详情")
    void testGetArticleById() {
        // Given
        Long articleId = testArticleId;

        // When - 获取文章详情
        ArticleDetailDTO result = articleService.getArticleById(articleId);

        // Then - 验证结果
        assertNotNull(result);
        assertEquals("测试文章", result.getTitle());
        assertEquals(testUserId, result.getAuthor().getId());
    }

    @Test
    @Order(10)
    @DisplayName("测试获取文章列表")
    void testGetArticleList() {
        // Given - 创建更多文章
        for (int i = 1; i <= 5; i++) {
            Article article = Article.builder()
                    .title("测试文章" + i)
                    .content("这是测试文章的内容" + i)
                    .author(testUser)
                    .status(Article.ArticleStatus.RELEASE)
                    .build();
            entityManager.persist(article);
        }
        entityManager.flush();

        // When - 分页查询
        Pageable pageable = PageRequest.of(0, 3);
        var result = articleService.getArticleList(pageable);

        // Then - 验证结果
        assertNotNull(result);
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(6); // setUp 中的 1 篇 + 新加的 5 篇
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @Order(11)
    @DisplayName("测试根据作者获取文章列表")
    void testGetArticlesByAuthor() {
        // Given - 创建更多文章
        for (int i = 1; i <= 3; i++) {
            Article article = Article.builder()
                    .title("作者文章" + i)
                    .content("内容" + i)
                    .author(testUser)
                    .status(Article.ArticleStatus.RELEASE)
                    .build();
            entityManager.persist(article);
        }
        entityManager.flush();

        // When
        Pageable pageable = PageRequest.of(0, 10);
        var result = articleService.getArticlesByAuthor(testUser, pageable);

        // Then
        assertNotNull(result);
        assertThat(result.getContent()).hasSize(4); // setUp 中的 1 篇 + 新加的 3 篇
    }

    @Test
    @Order(12)
    @DisplayName("测试搜索文章")
    void testSearchArticles() {
        // Given - 创建不同标题的文章
        Article springArticle = Article.builder()
                .title("Spring Boot 教程")
                .content("内容")
                .author(testUser)
                .build();
        entityManager.persist(springArticle);

        Article javaArticle = Article.builder()
                .title("Java 并发编程")
                .content("内容")
                .author(testUser)
                .build();
        entityManager.persist(javaArticle);
        entityManager.flush();

        // When - 搜索包含"Spring"的文章
        Pageable pageable = PageRequest.of(0, 10);
        var result = articleService.searchArticles("Spring", pageable);

        // Then
        assertNotNull(result);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).contains("Spring");
    }

    @Test
    @Order(13)
    @DisplayName("测试删除文章 - 成功")
    void testDeleteArticle_Success() {
        // Given
        Long articleId = testArticleId;

        // When - 执行删除操作
        articleService.deleteArticle(articleId, testUserId);

        // Then - 验证文章已被删除
        Optional<Article> deleted = articleRepository.findById(articleId);
        assertThat(deleted).isEmpty();
    }

    @Test
    @Order(14)
    @DisplayName("测试删除文章 - 文章不存在")
    void testDeleteArticle_NotFound() {
        // Given
        Long nonExistentId = 999L;

        // When & Then
        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> {
            articleService.deleteArticle(nonExistentId, testUserId);
        });
    }

    @Test
    @Order(15)
    @DisplayName("测试删除文章 - 无权限")
    void testDeleteArticle_NoPermission() {
        // Given
        Long articleId = testArticleId;
        Long anotherUserId = 999L;

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            articleService.deleteArticle(articleId, anotherUserId);
        });
    }

    @Test
    @Order(16)
    @DisplayName("测试创建文章 - 分类不存在")
    void testCreateArticle_CategoryNotFound() {
        // Given - 使用不存在的分类 ID
        ArticleCreateRequest invalidRequest = ArticleCreateRequest.builder()
                .title("测试文章")
                .content("这是测试文章的内容")
                .categoryIds(Set.of(999L))  // 不存在的分类 ID
                .build();

        // When & Then - 应该抛出异常
        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> {
            articleService.createArticle(invalidRequest, testUserId);
        });
    }

    @Test
    @Order(17)
    @DisplayName("测试创建多篇文章并验证分页")
    void testCreateMultipleArticlesWithPagination() {
        // Given - 创建 10 篇文章
        for (int i = 0; i < 10; i++) {
            ArticleCreateRequest request = ArticleCreateRequest.builder()
                    .title("批量文章" + i)
                    .content("这是批量创建的文章内容，长度足够 20 个字符以上")
                    .categoryIds(Set.of(testCategory.getId()))
                    .status(Article.ArticleStatus.RELEASE)
                    .build();
            articleService.createArticle(request, testUserId);
        }
        entityManager.flush();

        // When - 查询第 2 页
        Pageable pageable = PageRequest.of(1, 5);
        var result = articleService.getArticleList(pageable);

        // Then
        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(5);
    }

    @Test
    @Order(18)
    @DisplayName("测试文章状态流转")
    void testArticleStatusTransition() {
        // Given - 创建草稿文章
        ArticleCreateRequest draftRequest = ArticleCreateRequest.builder()
                .title("状态流转测试")
                .content("这是用于测试状态流转的文章内容")
                .categoryIds(Set.of(testCategory.getId()))
                .status(Article.ArticleStatus.DRAFT)
                .build();
        ArticleDetailDTO draftArticle = articleService.createArticle(draftRequest, testUserId);
        Long articleId = draftArticle.getId();

        // When & Then - 验证状态流转
        // 1. DRAFT -> RELEASE
        ArticleDetailDTO releasedArticle = articleService.publishArticle(articleId, testUserId);
        assertThat(releasedArticle.getStatus()).isEqualTo(Article.ArticleStatus.RELEASE);

        // 2. RELEASE -> ARCHIVE
        ArticleDetailDTO archivedArticle = articleService.archiveArticle(articleId, testUserId);
        assertThat(archivedArticle.getStatus()).isEqualTo(Article.ArticleStatus.ARCHIVE);
    }

    @Test
    @Order(19)
    @DisplayName("测试更新文章的部分字段")
    void testUpdateArticlePartialFields() {
        // Given
        ArticleUpdateRequest partialUpdate = ArticleUpdateRequest.builder()
                .title("只更新标题")
                .build();
        Long articleId = testArticleId;

        // When
        ArticleDetailDTO result = articleService.updateArticle(articleId, partialUpdate, testUserId);

        // Then - 验证只有标题被更新
        assertEquals("只更新标题", result.getTitle());
        assertEquals("这是测试文章的内容", result.getContent());  // 内容未变
    }

    @Test
    @Order(20)
    @DisplayName("测试空文章列表")
    void testEmptyArticleList() {
        // Given - 清空所有文章
        articleRepository.deleteAll();
        entityManager.flush();

        // When
        Pageable pageable = PageRequest.of(0, 10);
        var result = articleService.getArticleList(pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }
}
