package csulzc.My_Personal_Blogger.service;

import csulzc.My_Personal_Blogger.domain.entity.*;
import csulzc.My_Personal_Blogger.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({AdminService.class})
@DisplayName("AdminService 测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminServiceTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CommentRepository commentRepository;

    private User testUser;
    private Category categoryTech;
    private Category categoryLife;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("password123")
                .displayName("测试用户")
                .status(User.UserStatus.ACTIVE)
                .role(User.UserRole.USER)
                .build();
        entityManager.persist(testUser);

        categoryTech = Category.builder()
                .name("技术")
                .description("技术分类")
                .build();
        entityManager.persist(categoryTech);

        categoryLife = Category.builder()
                .name("生活")
                .description("生活分类")
                .build();
        entityManager.persist(categoryLife);

        entityManager.flush();
    }

    @AfterEach
    void tearDown() {
        entityManager.clear();
    }

    @Test
    @Order(1)
    @DisplayName("测试分类统计 - 多分类多文章时计算正确")
    void testGetDashboardStats_CategoryStats_MultipleCategoriesAndArticles() {
        // Given - 创建3篇文章，技术类2篇，生活类1篇
        Article article1 = Article.builder()
                .title("Java文章")
                .content("Java文章内容")
                .author(testUser)
                .status(Article.ArticleStatus.RELEASE)
                .build();
        article1.addCategory(categoryTech);
        entityManager.persist(article1);

        Article article2 = Article.builder()
                .title("Spring文章")
                .content("Spring文章内容")
                .author(testUser)
                .status(Article.ArticleStatus.RELEASE)
                .build();
        article2.addCategory(categoryTech);
        entityManager.persist(article2);

        Article article3 = Article.builder()
                .title("生活随笔")
                .content("生活文章内容")
                .author(testUser)
                .status(Article.ArticleStatus.RELEASE)
                .build();
        article3.addCategory(categoryLife);
        entityManager.persist(article3);

        entityManager.flush();

        // When - 获取看板统计
        var result = adminService.getDashboardStats();

        // Then - 验证分类统计
        assertNotNull(result);
        assertNotNull(result.getAdditionalMetrics());

        @SuppressWarnings("unchecked")
        Map<String, Object> topCategories = (Map<String, Object>) result.getAdditionalMetrics().get("topCategories");
        assertNotNull(topCategories);
        assertThat(topCategories.get("totalCategories")).isEqualTo(2);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> distribution = (List<Map<String, Object>>) topCategories.get("categoryDistribution");
        assertThat(distribution).hasSize(2);

        // 按文章数降序排列：技术(2篇)应在前面
        assertThat(distribution.get(0).get("categoryName")).isEqualTo("技术");
        assertThat(distribution.get(0).get("articleCount")).isEqualTo(2L);
        assertThat(distribution.get(0).get("percentage")).isEqualTo(new BigDecimal("66.67"));

        assertThat(distribution.get(1).get("categoryName")).isEqualTo("生活");
        assertThat(distribution.get(1).get("articleCount")).isEqualTo(1L);
        assertThat(distribution.get(1).get("percentage")).isEqualTo(new BigDecimal("33.33"));
    }

    @Test
    @Order(2)
    @DisplayName("测试分类统计 - 无文章时占比为0")
    void testGetDashboardStats_CategoryStats_EmptyArticles() {
        // Given - 不创建任何文章

        // When
        var result = adminService.getDashboardStats();

        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> topCategories = (Map<String, Object>) result.getAdditionalMetrics().get("topCategories");
        assertThat(topCategories.get("totalCategories")).isEqualTo(2);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> distribution = (List<Map<String, Object>>) topCategories.get("categoryDistribution");
        assertThat(distribution).hasSize(2);
        assertThat(distribution.get(0).get("articleCount")).isEqualTo(0L);
        assertThat(distribution.get(0).get("percentage")).isEqualTo(new BigDecimal("0"));
        assertThat(distribution.get(1).get("articleCount")).isEqualTo(0L);
        assertThat(distribution.get(1).get("percentage")).isEqualTo(new BigDecimal("0"));
    }

    @Test
    @Order(3)
    @DisplayName("测试最近活动 - 包含用户、文章、评论数据")
    void testGetDashboardStats_RecentActivity_WithData() {
        // Given - 创建多个用户、文章和评论
        User user2 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .passwordHash("password123")
                .displayName("用户2")
                .status(User.UserStatus.ACTIVE)
                .build();
        entityManager.persist(user2);

        Article article1 = Article.builder()
                .title("文章一")
                .content("文章内容一")
                .author(testUser)
                .status(Article.ArticleStatus.RELEASE)
                .build();
        entityManager.persist(article1);

        Article article2 = Article.builder()
                .title("文章二")
                .content("文章内容二，长度足够用于测试")
                .author(user2)
                .status(Article.ArticleStatus.DRAFT)
                .build();
        entityManager.persist(article2);

        Comment comment1 = Comment.builder()
                .content("这是一条简短的评论")
                .commenter(testUser)
                .article(article1)
                .isApproved(true)
                .build();
        entityManager.persist(comment1);

        Comment comment2 = Comment.builder()
                .content("这是一条很长的评论内容，用于测试超过五十个字符时的截断功能，需要确保长度足够...")
                .commenter(user2)
                .article(article1)
                .isApproved(true)
                .build();
        entityManager.persist(comment2);

        entityManager.flush();

        // When
        var result = adminService.getDashboardStats();

        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> recentActivity = (Map<String, Object>) result.getAdditionalMetrics().get("recentActivity");
        assertNotNull(recentActivity);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recentUsers = (List<Map<String, Object>>) recentActivity.get("recentUsers");
        assertThat(recentUsers).hasSize(2);
        assertThat(recentUsers.get(0)).containsKey("userId");
        assertThat(recentUsers.get(0)).containsKey("username");
        assertThat(recentUsers.get(0)).containsKey("registeredAt");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recentArticles = (List<Map<String, Object>>) recentActivity.get("recentArticles");
        assertThat(recentArticles).hasSize(2);
        assertThat(recentArticles.get(0)).containsKey("articleId");
        assertThat(recentArticles.get(0)).containsKey("title");
        assertThat(recentArticles.get(0)).containsKey("authorName");
        assertThat(recentArticles.get(0)).containsKey("status");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recentComments = (List<Map<String, Object>>) recentActivity.get("recentComments");
        assertThat(recentComments).hasSize(2);

        // 验证评论截断：长评论应被截断并以 "..." 结尾
        String longCommentContent = (String) recentComments.get(0).get("content");
        assertThat(longCommentContent).endsWith("...");
        assertThat(longCommentContent.length()).isLessThanOrEqualTo(53); // 50 + "..."

        // 验证短评论未被截断
        String shortCommentContent = (String) recentComments.get(1).get("content");
        assertThat(shortCommentContent).doesNotEndWith("...");
    }

    @Test
    @Order(4)
    @DisplayName("测试最近活动 - 无数据时返回空列表")
    void testGetDashboardStats_RecentActivity_EmptyData() {
        // When - 没有任何文章、评论（除了setUp中的用户）
        var result = adminService.getDashboardStats();

        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> recentActivity = (Map<String, Object>) result.getAdditionalMetrics().get("recentActivity");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recentUsers = (List<Map<String, Object>>) recentActivity.get("recentUsers");
        // setUp中创建了一个用户，所以recentUsers不为空
        assertThat(recentUsers).hasSize(1);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recentArticles = (List<Map<String, Object>>) recentActivity.get("recentArticles");
        assertThat(recentArticles).isEmpty();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recentComments = (List<Map<String, Object>>) recentActivity.get("recentComments");
        assertThat(recentComments).isEmpty();
    }

    @Test
    @Order(5)
    @DisplayName("测试最近活动 - 评论内容截断边界")
    void testGetDashboardStats_RecentActivity_CommentTruncation() {
        // Given - 创建一条恰好50字符的评论和一条51字符的评论
        Article article = Article.builder()
                .title("测试文章")
                .content("测试文章内容")
                .author(testUser)
                .status(Article.ArticleStatus.RELEASE)
                .build();
        entityManager.persist(article);

        Comment commentExact50 = Comment.builder()
                .content("这是一段恰好五十个字符长度的评论内容用于边界测试")
                .commenter(testUser)
                .article(article)
                .isApproved(true)
                .build();
        entityManager.persist(commentExact50);

        Comment comment51 = Comment.builder()
                .content("这是一段超过五十个字符长度的评论内容用于测试截断边界情况...")
                .commenter(testUser)
                .article(article)
                .isApproved(true)
                .build();
        entityManager.persist(comment51);

        entityManager.flush();

        // When
        var result = adminService.getDashboardStats();

        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> recentActivity = (Map<String, Object>) result.getAdditionalMetrics().get("recentActivity");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recentComments = (List<Map<String, Object>>) recentActivity.get("recentComments");
        assertThat(recentComments).hasSize(2);

        // 51字符的评论应被截断
        String truncatedComment = (String) recentComments.get(0).get("content");
        assertThat(truncatedComment).endsWith("...");

        // 50字符的评论不应被截断
        String exactComment = (String) recentComments.get(1).get("content");
        assertThat(exactComment).doesNotEndWith("...");
    }
}
