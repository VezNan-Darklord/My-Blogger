package csulzc.My_Personal_Blogger;

import csulzc.My_Personal_Blogger.api.dto.dashboard.DashboardStatsDTO;
import csulzc.My_Personal_Blogger.config.JwtProperties;
import csulzc.My_Personal_Blogger.domain.entity.Article;
import csulzc.My_Personal_Blogger.domain.entity.Comment;
import csulzc.My_Personal_Blogger.domain.entity.User;
import csulzc.My_Personal_Blogger.repository.ArticleRepository;
import csulzc.My_Personal_Blogger.repository.CommentRepository;
import csulzc.My_Personal_Blogger.repository.UserRepository;
import csulzc.My_Personal_Blogger.security.JwtTokenProvider;
import csulzc.My_Personal_Blogger.service.AdminService;
import csulzc.My_Personal_Blogger.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableConfigurationProperties(JwtProperties.class)
@Import({AdminService.class, UserService.class, AdminIntegrationTest.TestConfig.class, JwtTokenProvider.class})
@Transactional
@DisplayName("Admin 集成测试 - 管理员权限与看板功能")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
        public JwtProperties jwtProperties() {
            return new JwtProperties();
        }
    }

    @Autowired
    private AdminService adminService;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;
    private User adminUser;
    private Article testArticle1;
    private Article testArticle2;
    private Comment testComment1;

    @BeforeEach
    void setUp() {
        // 创建普通用户1
        testUser1 = User.builder()
                .username("user1")
                .email("user1@example.com")
                .passwordHash(encodePassword("password123"))
                .displayName("用户1")
                .bio("这是用户1的简介")
                .avatar("https://example.com/avatar1.jpg")
                .status(User.UserStatus.ACTIVE)
                .role(User.UserRole.USER)
                .build();
        entityManager.persist(testUser1);

        // 创建普通用户2
        testUser2 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .passwordHash(encodePassword("password456"))
                .displayName("用户2")
                .bio("这是用户2的简介")
                .avatar("https://example.com/avatar2.jpg")
                .status(User.UserStatus.ACTIVE)
                .role(User.UserRole.USER)
                .build();
        entityManager.persist(testUser2);

        // 创建管理员用户
        adminUser = User.builder()
                .username("admin")
                .email("admin@example.com")
                .passwordHash(encodePassword("admin123"))
                .displayName("系统管理员")
                .bio("系统管理员账号")
                .status(User.UserStatus.ACTIVE)
                .role(User.UserRole.SUPER_ADMIN)
                .build();
        entityManager.persist(adminUser);

        // 创建测试文章1
        testArticle1 = Article.builder()
                .title("测试文章1")
                .content("这是测试文章1的内容，用于集成测试")
                .summary("测试文章1摘要")
                .status(Article.ArticleStatus.RELEASE)
                .viewCount(100)
                .likeCount(10)
                .favoriteCount(5)
                .author(testUser1)
                .build();
        entityManager.persist(testArticle1);

        // 创建测试文章2（草稿）
        testArticle2 = Article.builder()
                .title("测试文章2")
                .content("这是测试文章2的内容，处于草稿状态")
                .summary("测试文章2摘要")
                .status(Article.ArticleStatus.DRAFT)
                .viewCount(0)
                .likeCount(0)
                .favoriteCount(0)
                .author(testUser2)
                .build();
        entityManager.persist(testArticle2);

        // 创建测试评论1
        testComment1 = Comment.builder()
                .content("这是测试评论")
                .isApproved(true)
                .commenter(testUser1)
                .article(testArticle1)
                .build();
        entityManager.persist(testComment1);

        entityManager.flush();
    }

    @AfterEach
    void tearDown() {
        entityManager.clear();
    }

    private String encodePassword(String password) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.encode(password);
    }

    @Test
    @Order(1)
    @DisplayName("测试获取看板统计数据 - 验证用户统计")
    void testGetDashboardStats_WithUserStatistics() {
        // When - 获取看板统计
        DashboardStatsDTO stats = adminService.getDashboardStats();

        // Then - 验证用户统计数据
        assertThat(stats).isNotNull();
        assertThat(stats.getUserStats()).isNotNull();
        assertThat(stats.getUserStats().getTotalUsers()).isEqualTo(3L); // 2个普通用户 + 1个管理员
        assertThat(stats.getUserStats().getActiveUsers()).isEqualTo(3L);
        assertThat(stats.getUserStats().getNewUsersToday()).isGreaterThanOrEqualTo(3L);
        assertThat(stats.getUserStats().getNewUsersThisMonth()).isGreaterThanOrEqualTo(3L);
    }

    @Test
    @Order(2)
    @DisplayName("测试获取看板统计数据 - 验证文章统计")
    void testGetDashboardStats_WithArticleStatistics() {
        // When - 获取看板统计
        DashboardStatsDTO stats = adminService.getDashboardStats();

        // Then - 验证文章统计数据
        assertThat(stats).isNotNull();
        assertThat(stats.getArticleStats()).isNotNull();
        assertThat(stats.getArticleStats().getTotalArticles()).isEqualTo(2L);
        assertThat(stats.getArticleStats().getPublishedArticles()).isEqualTo(1L);
        assertThat(stats.getArticleStats().getDraftArticles()).isEqualTo(1L);
        assertThat(stats.getArticleStats().getTotalViews()).isEqualTo(100L);
        assertThat(stats.getArticleStats().getTotalLikes()).isEqualTo(10L);
    }

    @Test
    @Order(3)
    @DisplayName("测试获取看板统计数据 - 验证评论统计")
    void testGetDashboardStats_WithCommentStatistics() {
        // When - 获取看板统计
        DashboardStatsDTO stats = adminService.getDashboardStats();

        // Then - 验证评论统计数据
        assertThat(stats).isNotNull();
        assertThat(stats.getCommentStats()).isNotNull();
        assertThat(stats.getCommentStats().getTotalComments()).isEqualTo(1L);
        assertThat(stats.getCommentStats().getApprovedComments()).isEqualTo(1L);
        assertThat(stats.getCommentStats().getPendingComments()).isEqualTo(0L);
    }

    @Test
    @Order(4)
    @DisplayName("测试判断用户是否为管理员 - 成功场景")
    void testIsAdmin_WithAdminUser() {
        // When & Then - 验证管理员用户
        assertThat(adminService.isAdmin(adminUser.getId())).isTrue();

        // 验证超级管理员
        User superAdmin = User.builder()
                .username("superadmin")
                .email("superadmin@example.com")
                .passwordHash(encodePassword("admin123"))
                .role(User.UserRole.SUPER_ADMIN)
                .build();
        entityManager.persist(superAdmin);
        entityManager.flush();

        assertThat(adminService.isAdmin(superAdmin.getId())).isTrue();
    }

    @Test
    @Order(5)
    @DisplayName("测试判断用户是否为管理员 - 普通用户场景")
    void testIsAdmin_WithRegularUser() {
        // When & Then - 验证普通用户不是管理员
        assertThat(adminService.isAdmin(testUser1.getId())).isFalse();
        assertThat(adminService.isAdmin(testUser2.getId())).isFalse();
    }

    @Test
    @Order(6)
    @DisplayName("测试判断用户是否为管理员 - 用户不存在场景")
    void testIsAdmin_WithNonExistentUser() {
        // When & Then - 应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.isAdmin(999L);
        });
    }

    @Test
    @Order(7)
    @DisplayName("测试提升用户为管理员 - 成功场景")
    void testPromoteToAdmin_Success() {
        // Given - 普通用户
        Long userId = testUser1.getId();
        assertThat(testUser1.getRole()).isEqualTo(User.UserRole.USER);

        // When - 提升为管理员
        adminService.promoteToAdmin(userId);

        // Then - 验证角色已变更
        entityManager.flush();
        entityManager.clear();

        User updatedUser = entityManager.find(User.class, userId);
        assertThat(updatedUser.getRole()).isEqualTo(User.UserRole.ADMIN);
    }

    @Test
    @Order(8)
    @DisplayName("测试提升已经是管理员的用户 - 失败场景")
    void testPromoteToAdmin_AlreadyAdmin() {
        // Given - 已经是管理员的用户
        Long adminId = adminUser.getId();

        // When & Then - 应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.promoteToAdmin(adminId);
        });
    }

    @Test
    @Order(9)
    @DisplayName("测试提升不存在的用户 - 失败场景")
    void testPromoteToAdmin_UserNotFound() {
        // When & Then - 应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.promoteToAdmin(999L);
        });
    }

    @Test
    @Order(10)
    @DisplayName("测试降级管理员为普通用户 - 成功场景")
    void testDemoteFromAdmin_Success() {
        // Given - 先提升一个用户为管理员
        Long userId = testUser1.getId();
        adminService.promoteToAdmin(userId);

        User promotedUser = entityManager.find(User.class, userId);
        assertThat(promotedUser.getRole()).isEqualTo(User.UserRole.ADMIN);

        // When - 降级为普通用户
        adminService.demoteFromAdmin(userId);

        // Then - 验证角色已变更
        entityManager.flush();
        entityManager.clear();

        User demotedUser = entityManager.find(User.class, userId);
        assertThat(demotedUser.getRole()).isEqualTo(User.UserRole.USER);
    }

    @Test
    @Order(11)
    @DisplayName("测试降级普通用户 - 失败场景")
    void testDemoteFromAdmin_NotAdmin() {
        // Given - 普通用户
        Long userId = testUser1.getId();

        // When & Then - 应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.demoteFromAdmin(userId);
        });
    }

    @Test
    @Order(12)
    @DisplayName("测试降级超级管理员 - 失败场景")
    void testDemoteFromAdmin_SuperAdmin() {
        // Given - 超级管理员
        Long superAdminId = adminUser.getId();

        // When & Then - 应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.demoteFromAdmin(superAdminId);
        });
    }

    @Test
    @Order(13)
    @DisplayName("测试降级不存在的用户 - 失败场景")
    void testDemoteFromAdmin_UserNotFound() {
        // When & Then - 应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.demoteFromAdmin(999L);
        });
    }

    @Test
    @Order(14)
    @DisplayName("测试看板统计包含额外指标")
    void testGetDashboardStats_WithAdditionalMetrics() {
        // When - 获取看板统计
        DashboardStatsDTO stats = adminService.getDashboardStats();

        // Then - 验证额外指标存在
        assertThat(stats.getAdditionalMetrics()).isNotNull();
        assertThat(stats.getAdditionalMetrics()).containsKey("topCategories");
        assertThat(stats.getAdditionalMetrics()).containsKey("recentActivity");
    }

    @Test
    @Order(15)
    @DisplayName("测试复杂场景 - 多用户、多文章的统计准确性")
    void testComplexScenario_AccurateStatsWithMultipleEntities() {
        // Given - 创建更多测试数据

        // 创建新用户（昨天创建的）
        LocalDateTime yesterday = LocalDate.now().minusDays(1).atStartOfDay();
        User newUser = User.builder()
                .username("newuser")
                .email("newuser@example.com")
                .passwordHash(encodePassword("password789"))
                .role(User.UserRole.USER)
                .build();
        newUser.setCreatedAt(yesterday);
        newUser.setUpdatedAt(yesterday);
        entityManager.persist(newUser);

        // 创建新文章
        Article newArticle = Article.builder()
                .title("新文章")
                .content("这是新文章的内容")
                .summary("新文章摘要")
                .status(Article.ArticleStatus.RELEASE)
                .viewCount(50)
                .likeCount(8)
                .favoriteCount(3)
                .author(testUser1)
                .build();
        entityManager.persist(newArticle);

        // 创建未审核评论
        Comment pendingComment = Comment.builder()
                .content("待审核评论")
                .isApproved(false)
                .commenter(testUser2)
                .article(testArticle1)
                .build();
        entityManager.persist(pendingComment);

        // Deleted:entityManager.flush();

        // When - 获取看板统计
        DashboardStatsDTO stats = adminService.getDashboardStats();

        // Then - 验证统计数据的准确性
        assertThat(stats.getUserStats().getTotalUsers()).isEqualTo(4L); // 原有3个 + 新增1个
        assertThat(stats.getUserStats().getNewUsersToday()).isEqualTo(4L); // 今天创建的4个用户（newUser的时间被@PrePersist覆盖）
        assertThat(stats.getUserStats().getNewUsersThisMonth()).isEqualTo(4L); // 本月创建的4个用户

        assertThat(stats.getArticleStats().getTotalArticles()).isEqualTo(3L); // 原有2个 + 新增1个
        assertThat(stats.getArticleStats().getPublishedArticles()).isEqualTo(2L);
        assertThat(stats.getArticleStats().getTotalViews()).isEqualTo(150L); // 100 + 50
        assertThat(stats.getArticleStats().getTotalLikes()).isEqualTo(18L); // 10 + 8

        assertThat(stats.getCommentStats().getTotalComments()).isEqualTo(2L); // 原有1个 + 新增1个
        assertThat(stats.getCommentStats().getPendingComments()).isEqualTo(1L);
        assertThat(stats.getCommentStats().getApprovedComments()).isEqualTo(1L);
    }

    @Test
    @Order(16)
    @DisplayName("测试角色枚举值 - 验证所有角色类型")
    void testUserRoleEnumValues() {
        // When & Then - 验证所有角色枚举值存在
        assertThat(User.UserRole.values()).hasSize(3);
        assertThat(User.UserRole.USER).isNotNull();
        assertThat(User.UserRole.ADMIN).isNotNull();
        assertThat(User.UserRole.SUPER_ADMIN).isNotNull();
    }

    @Test
    @Order(17)
    @DisplayName("测试管理员用户创建时的默认角色")
    void testAdminUserCreation_DefaultRole() {
        // Given - 创建新的管理员用户
        User newAdmin = User.builder()
                .username("newadmin")
                .email("newadmin@example.com")
                .passwordHash(encodePassword("adminpass"))
                .role(User.UserRole.ADMIN)
                .status(User.UserStatus.ACTIVE)
                .build();
        entityManager.persist(newAdmin);
        entityManager.flush();

        // When - 查询用户
        Optional<User> foundUser = userRepository.findByUsername("newadmin");

        // Then - 验证角色正确保存
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getRole()).isEqualTo(User.UserRole.ADMIN);
    }

    @Test
    @Order(18)
    @DisplayName("测试根据角色查询用户")
    void testFindByRole() {
        // When - 查询所有管理员
        java.util.List<User> admins = userRepository.findByRole(User.UserRole.SUPER_ADMIN);

        // Then - 验证找到管理员
        assertThat(admins).isNotEmpty();
        assertThat(admins).anyMatch(u -> u.getUsername().equals("admin"));

        // 查询所有普通用户
        java.util.List<User> users = userRepository.findByRole(User.UserRole.USER);
        assertThat(users).hasSize(2); // testUser1 和 testUser2
    }

    @Test
    @Order(19)
    @DisplayName("测试统计指定角色的用户数量")
    void testCountByRole() {
        // When - 统计各角色数量
        long userCount = userRepository.countByRole(User.UserRole.USER);
        long adminCount = userRepository.countByRole(User.UserRole.SUPER_ADMIN);

        // Then - 验证统计结果
        assertThat(userCount).isEqualTo(2L);
        assertThat(adminCount).isEqualTo(1L);
    }

    @Test
    @Order(20)
    @DisplayName("测试完整的管理员工作流程 - 从提升到看板验证")
    void testCompleteWorkflow_PromoteAndVerifyDashboard() {
        // Given - 初始状态
        assertThat(testUser1.getRole()).isEqualTo(User.UserRole.USER);

        // Step 1: 提升用户为管理员
        adminService.promoteToAdmin(testUser1.getId());
        entityManager.flush();
        entityManager.clear();

        // Step 2: 验证用户现在是管理员
        assertThat(adminService.isAdmin(testUser1.getId())).isTrue();

        // Step 3: 获取看板统计，验证数据完整性
        DashboardStatsDTO stats = adminService.getDashboardStats();
        assertThat(stats).isNotNull();
        assertThat(stats.getUserStats().getTotalUsers()).isEqualTo(3L);
        assertThat(stats.getArticleStats().getTotalArticles()).isEqualTo(2L);
        assertThat(stats.getCommentStats().getTotalComments()).isEqualTo(1L);

        // Step 4: 降级回普通用户
        adminService.demoteFromAdmin(testUser1.getId());
        entityManager.flush();
        entityManager.clear();

        // Step 5: 验证用户不再是管理员
        assertThat(adminService.isAdmin(testUser1.getId())).isFalse();
        assertThat(entityManager.find(User.class, testUser1.getId()).getRole())
                .isEqualTo(User.UserRole.USER);
    }
}
