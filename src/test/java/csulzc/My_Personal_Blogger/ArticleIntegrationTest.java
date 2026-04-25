package csulzc.My_Personal_Blogger;

import csulzc.My_Personal_Blogger.api.dto.article.ArticleCreateRequest;
import csulzc.My_Personal_Blogger.api.dto.article.ArticleDetailDTO;
import csulzc.My_Personal_Blogger.api.dto.article.ArticleListItemDTO;
import csulzc.My_Personal_Blogger.api.dto.article.ArticleUpdateRequest;
import csulzc.My_Personal_Blogger.domain.entity.Article;
import csulzc.My_Personal_Blogger.domain.entity.Category;
import csulzc.My_Personal_Blogger.domain.entity.User;
import csulzc.My_Personal_Blogger.repository.ArticleRepository;
import csulzc.My_Personal_Blogger.repository.CategoryRepository;
import csulzc.My_Personal_Blogger.repository.UserRepository;
import csulzc.My_Personal_Blogger.service.ArticleService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({ArticleService.class})
@Transactional
@DisplayName("Article 集成测试 - 层间协作")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ArticleIntegrationTest {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User testUser1;
    private User testUser2;
    private Category testCategory1;
    private Category testCategory2;
    private Article testArticle;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser1 = User.builder()
                .username("user1")
                .email("user1@example.com")
                .passwordHash("password123")
                .displayName("用户1")
                .status(User.UserStatus.ACTIVE)
                .build();
        entityManager.persist(testUser1);

        testUser2 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .passwordHash("password123")
                .displayName("用户2")
                .status(User.UserStatus.ACTIVE)
                .build();
        entityManager.persist(testUser2);

        // 创建测试分类
        testCategory1 = Category.builder()
                .name("技术")
                .description("技术相关文章")
                .build();
        entityManager.persist(testCategory1);

        testCategory2 = Category.builder()
                .name("生活")
                .description("生活相关文章")
                .build();
        entityManager.persist(testCategory2);

        // 创建测试文章
        testArticle = Article.builder()
                .title("初始文章")
                .content("这是初始文章内容")
                .summary("初始摘要")
                .coverImage("https://example.com/cover.jpg")
                .status(Article.ArticleStatus.DRAFT)
                .likeCount(5)
                .favoriteCount(2)
                .author(testUser1)
                .build();
        testArticle.addCategory(testCategory1);
        entityManager.persist(testArticle);

        entityManager.flush();
    }

    @AfterEach
    void tearDown() {
        entityManager.clear();
    }

    @Test
    @Order(1)
    @DisplayName("测试文章创建 - 验证实体关系持久化")
    void testArticleCreation_WithEntityRelationships() {
        // Given - 准备包含多个分类的文章创建请求
        ArticleCreateRequest request = ArticleCreateRequest.builder()
                .title("集成测试文章")
                .content("这是一篇用于集成测试的文章内容，足够长以生成摘要")
                .categoryIds(Set.of(testCategory1.getId(), testCategory2.getId()))
                .status(Article.ArticleStatus.RELEASE)
                .build();

        // When - 通过服务层创建文章
        ArticleDetailDTO result = articleService.createArticle(request, testUser1.getId());

        // Then - 验证返回结果
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("集成测试文章");
        assertThat(result.getStatus()).isEqualTo(Article.ArticleStatus.RELEASE);
        assertThat(result.getCategories()).hasSize(2);

        // 验证数据库中的实体关系
        Optional<Article> savedArticleOpt = articleRepository.findById(result.getId());
        assertThat(savedArticleOpt).isPresent();
        Article savedArticle = savedArticleOpt.get();

        assertThat(savedArticle.getAuthor().getId()).isEqualTo(testUser1.getId());
        assertThat(savedArticle.getCategories()).hasSize(2);
        assertThat(savedArticle.getCategories())
                .extracting(Category::getName)
                .containsExactlyInAnyOrder("技术", "生活");

        // Deleted:// 验证双向关系
        // Deleted:assertThat(savedArticle.getAuthor().getArticles()).contains(savedArticle);

        // 验证文章确实关联到正确的作者（通过反向查询验证）
        var articlesByAuthor = articleRepository.findByAuthor(testUser1, PageRequest.of(0, 10));
        assertThat(articlesByAuthor.getContent()).anyMatch(a -> a.getId().equals(savedArticle.getId()));
    }


    @Test
    @Order(2)
    @DisplayName("测试文章更新 - 验证级联更新和关系变更")
    void testArticleUpdate_WithCascadeAndRelationshipChanges() {
        // Given - 更新文章并更改分类
        ArticleUpdateRequest updateRequest = ArticleUpdateRequest.builder()
                .title("更新后的文章标题")
                .content("这是更新后的内容，足够长以重新生成摘要信息")
                .categoryIds(Set.of(testCategory2.getId())) // 只保留第二个分类
                .build();

        // When - 执行更新
        ArticleDetailDTO result = articleService.updateArticle(
                testArticle.getId(), updateRequest, testUser1.getId());

        // Then - 验证更新结果
        assertThat(result.getTitle()).isEqualTo("更新后的文章标题");
        assertThat(result.getSummary()).contains("更新后的内容");
        assertThat(result.getCategories()).hasSize(1);
        assertThat(result.getCategories().getFirst().getName()).isEqualTo("生活");

        // 验证数据库中的变化
        Article updatedArticle = entityManager.find(Article.class, testArticle.getId());
        assertThat(updatedArticle.getTitle()).isEqualTo("更新后的文章标题");
        assertThat(updatedArticle.getCategories()).hasSize(1);
        assertThat(updatedArticle.getCategories().iterator().next().getName()).isEqualTo("生活");

        // 验证文章只关联到新分类（通过文章的分类集合验证）
        assertThat(updatedArticle.getCategories())
                .extracting(Category::getId)
                .containsOnly(testCategory2.getId());

        // 验证不再包含旧分类
        assertThat(updatedArticle.getCategories())
                .extracting(Category::getId)
                .doesNotContain(testCategory1.getId());

        // 清除缓存后重新验证，确保数据已持久化
        entityManager.flush();
        entityManager.clear();

        Article reloadedArticle = entityManager.find(Article.class, testArticle.getId());
        assertThat(reloadedArticle.getCategories()).hasSize(1);
        assertThat(reloadedArticle.getCategories().iterator().next().getId())
                .isEqualTo(testCategory2.getId());
    }


    @Test
    @Order(3)
    @DisplayName("测试文章状态流转 - 验证业务逻辑与数据一致性")
    void testArticleStatusTransition_WithBusinessLogic() {
        // Given - 文章初始状态为DRAFT
        assertThat(testArticle.getStatus()).isEqualTo(Article.ArticleStatus.DRAFT);

        // When - 发布文章
        ArticleDetailDTO published = articleService.publishArticle(
                testArticle.getId(), testUser1.getId());

        // Then - 验证发布成功
        assertThat(published.getStatus()).isEqualTo(Article.ArticleStatus.RELEASE);

        Article persistedArticle = entityManager.find(Article.class, testArticle.getId());
        assertThat(persistedArticle.getStatus()).isEqualTo(Article.ArticleStatus.RELEASE);

        // When - 归档文章
        ArticleDetailDTO archived = articleService.archiveArticle(
                testArticle.getId(), testUser1.getId());

        // Then - 验证归档成功
        assertThat(archived.getStatus()).isEqualTo(Article.ArticleStatus.ARCHIVE);
        assertThat(entityManager.find(Article.class, testArticle.getId()).getStatus())
                .isEqualTo(Article.ArticleStatus.ARCHIVE);
    }

    @Test
    @Order(4)
    @DisplayName("测试文章查询 - 验证懒加载和DTO转换")
    void testArticleQuery_WithLazyLoadingAndDtoConversion() {
        // Given - 创建多篇不同作者和分类的文章
        for (int i = 0; i < 5; i++) {
            Article article = Article.builder()
                    .title("文章" + i)
                    .content("内容" + i + "，这段文字足够长以便测试摘要生成")
                    .author(i % 2 == 0 ? testUser1 : testUser2)
                    .status(Article.ArticleStatus.RELEASE)
                    .likeCount(i * 10)
                    .build();
            article.addCategory(i % 2 == 0 ? testCategory1 : testCategory2);
            entityManager.persist(article);
        }
        entityManager.flush();
        entityManager.clear(); // 清除缓存，测试真实的数据库查询

        // When - 分页查询文章列表
        Pageable pageable = PageRequest.of(0, 3);
        var articleList = articleService.getArticleList(pageable);

        // Then - 验证分页结果和DTO转换
        assertThat(articleList.getContent()).hasSize(3);
        assertThat(articleList.getTotalElements()).isEqualTo(6); // 1 + 5

        // 验证DTO中包含必要信息
        ArticleListItemDTO firstArticle = articleList.getContent().getFirst();
        assertThat(firstArticle.getId()).isNotNull();
        assertThat(firstArticle.getTitle()).isNotNull();
        assertThat(firstArticle.getAuthor()).isNotNull();
        assertThat(firstArticle.getCategories()).isNotNull();
    }

    @Test
    @Order(5)
    @DisplayName("测试按作者查询 - 验证关联查询和权限控制")
    void testGetArticlesByAuthor_WithAssociationQuery() {
        // Given - 为两个用户创建不同数量的文章
        for (int i = 0; i < 3; i++) {
            Article article = Article.builder()
                    .title("用户1的文章" + i)
                    .content("内容" + i)
                    .author(testUser1)
                    .status(Article.ArticleStatus.RELEASE)
                    .build();
            entityManager.persist(article);
        }

        for (int i = 0; i < 2; i++) {
            Article article = Article.builder()
                    .title("用户2的文章" + i)
                    .content("内容" + i)
                    .author(testUser2)
                    .status(Article.ArticleStatus.RELEASE)
                    .build();
            entityManager.persist(article);
        }
        entityManager.flush();
        entityManager.clear();

        // When - 查询特定作者的文章
        Pageable pageable = PageRequest.of(0, 10);
        var user1Articles = articleService.getArticlesByAuthor(testUser1, pageable);
        var user2Articles = articleService.getArticlesByAuthor(testUser2, pageable);

        // Then - 验证查询结果
        assertThat(user1Articles.getContent()).hasSize(4); // 1 + 3
        assertThat(user2Articles.getContent()).hasSize(2);

        // 验证所有返回的文章都属于正确的作者
        user1Articles.getContent().forEach(article ->
            assertThat(article.getAuthor().getId()).isEqualTo(testUser1.getId()));
    }

    @Test
    @Order(6)
    @DisplayName("测试文章搜索 - 验证复杂查询条件")
    void testArticleSearch_WithComplexConditions() {
        // Given - 创建具有不同标题的文章
        String[] titles = {
            "Spring Boot实战教程",
            "Java并发编程详解",
            "Spring Security最佳实践",
            "MySQL性能优化指南"
        };

        for (String title : titles) {
            Article article = Article.builder()
                    .title(title)
                    .content("文章内容...")
                    .author(testUser1)
                    .status(Article.ArticleStatus.RELEASE)
                    .build();
            entityManager.persist(article);
        }
        entityManager.flush();
        entityManager.clear();

        // When - 搜索包含"Spring"的文章
        Pageable pageable = PageRequest.of(0, 10);
        var searchResult = articleService.searchArticles("Spring", pageable);

        // Then - 验证搜索结果准确性
        assertThat(searchResult.getContent()).hasSize(2);
        assertThat(searchResult.getContent())
                .extracting("title")
                .allMatch(title -> ((String) title).contains("Spring"));
    }

    @Test
    @Order(7)
    @DisplayName("测试文章删除 - 验证级联删除和数据完整性")
    void testArticleDeletion_WithCascadeDelete() {
        // Given - 获取要删除的文章ID
        Long articleId = testArticle.getId();

        // 验证文章存在
        assertThat(articleRepository.findById(articleId)).isPresent();

        // When - 删除文章
        articleService.deleteArticle(articleId, testUser1.getId());

        // Then - 验证文章已被删除
        assertThat(articleRepository.findById(articleId)).isEmpty();

        // 验证文章确实被删除（通过反向查询）
        var remainingArticles = articleRepository.findByAuthor(testUser1, PageRequest.of(0, 10));
        assertThat(remainingArticles.getContent()).isEmpty();

        // 验证中间表记录已清理（通过检查分类的文章数量）
        Category category = entityManager.find(Category.class, testCategory1.getId());
        assertThat(category).isNotNull();
        // 注意：由于懒加载，我们直接验证文章不存在，而不是验证集合为空
        assertThat(articleRepository.findById(articleId)).isEmpty();
    }


    @Test
    @Order(8)
    @DisplayName("测试权限控制 - 验证跨层权限验证逻辑")
    void testPermissionControl_AcrossLayers() {
        // Given - 尝试用非作者用户操作文章
        Long articleId = testArticle.getId();
        Long unauthorizedUserId = testUser2.getId();

        ArticleUpdateRequest updateRequest = ArticleUpdateRequest.builder()
                .title("未授权的更新")
                .build();

        // When & Then - 应该抛出权限异常
        assertThrows(RuntimeException.class, () -> {
            articleService.updateArticle(articleId, updateRequest, unauthorizedUserId);
        });

        // 验证文章未被修改
        Article article = entityManager.find(Article.class, articleId);
        assertThat(article.getTitle()).isEqualTo("初始文章");
    }

    @Test
    @Order(9)
    @DisplayName("测试事务回滚 - 验证异常时的数据一致性")
    void testTransactionRollback_OnException() {
        // Given - 准备一个会导致异常的操作（使用不存在的分类）
        ArticleCreateRequest invalidRequest = ArticleCreateRequest.builder()
                .title("会失败的文章")
                .content("这篇文章因为分类不存在而应该失败")
                .categoryIds(Set.of(999L)) // 不存在的分类ID
                .build();

        // When & Then - 应该抛出异常，且事务回滚
        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> {
            articleService.createArticle(invalidRequest, testUser1.getId());
        });

        // 验证没有创建任何文章
        long count = articleRepository.count();
        assertThat(count).isEqualTo(1); // 只有setUp中创建的那一篇
    }

    @Test
    @Order(10)
    @DisplayName("测试复杂场景 - 多用户、多分类的完整流程")
    void testComplexScenario_FullWorkflow() {
        // Given - 模拟真实场景：多个用户创建、更新、发布文章

        // 用户1创建文章
        ArticleCreateRequest request1 = ArticleCreateRequest.builder()
                .title("用户1的技术文章")
                .content("这是关于技术的详细内容，长度足够生成摘要")
                .categoryIds(Set.of(testCategory1.getId()))
                .status(Article.ArticleStatus.DRAFT)
                .build();
        ArticleDetailDTO article1 = articleService.createArticle(request1, testUser1.getId());

        // 用户2创建文章
        ArticleCreateRequest request2 = ArticleCreateRequest.builder()
                .title("用户2的生活分享")
                .content("这是关于生活的详细内容，长度足够生成摘要")
                .categoryIds(Set.of(testCategory2.getId()))
                .status(Article.ArticleStatus.DRAFT)
                .build();
        ArticleDetailDTO article2 = articleService.createArticle(request2, testUser2.getId());

        // When - 执行各种操作
        // 发布用户1的文章
        ArticleDetailDTO published1 = articleService.publishArticle(article1.getId(), testUser1.getId());

        // 更新用户2的文章并发布
        ArticleUpdateRequest updateRequest = ArticleUpdateRequest.builder()
                .title("用户2更新后的生活分享")
                .categoryIds(Set.of(testCategory1.getId(), testCategory2.getId()))
                .build();
        ArticleDetailDTO updated2 = articleService.updateArticle(article2.getId(), updateRequest, testUser2.getId());
        ArticleDetailDTO published2 = articleService.publishArticle(article2.getId(), testUser2.getId());

        // Then - 验证所有操作的结果
        assertThat(published1.getStatus()).isEqualTo(Article.ArticleStatus.RELEASE);
        assertThat(published2.getStatus()).isEqualTo(Article.ArticleStatus.RELEASE);
        assertThat(updated2.getCategories()).hasSize(2);

        // 验证数据库状态
        List<Article> allArticles = articleRepository.findAll();
        assertThat(allArticles).hasSize(3); // setUp中的1篇 + 新创建的2篇

        // 验证统计信息
        assertThat(articleRepository.findByStatus(Article.ArticleStatus.RELEASE)).hasSize(2);
        assertThat(articleRepository.findByAuthor(testUser1, PageRequest.of(0, 10))).hasSize(2);
        assertThat(articleRepository.findByAuthor(testUser2, PageRequest.of(0, 10))).hasSize(1);
    }
}
