package csulzc.My_Personal_Blogger;

import csulzc.My_Personal_Blogger.api.dto.user.UserDetailDTO;
import csulzc.My_Personal_Blogger.api.dto.user.UserLoginRequest;
import csulzc.My_Personal_Blogger.api.dto.user.UserRegisterRequest;
import csulzc.My_Personal_Blogger.api.dto.user.UserUpdateRequest;
import csulzc.My_Personal_Blogger.domain.entity.Article;
import csulzc.My_Personal_Blogger.domain.entity.Comment;
import csulzc.My_Personal_Blogger.domain.entity.User;
import csulzc.My_Personal_Blogger.repository.ArticleRepository;
import csulzc.My_Personal_Blogger.repository.CommentRepository;
import csulzc.My_Personal_Blogger.repository.UserRepository;
import csulzc.My_Personal_Blogger.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({UserService.class, UserIntegrationTest.TestSecurityConfig.class})
@Transactional
@DisplayName("User 集成测试 - 层间协作")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserIntegrationTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    @Autowired
    private UserService userService;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        // 创建测试用户1
        testUser1 = User.builder()
                .username("user1")
                .email("user1@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .displayName("用户1")
                .bio("这是用户1的简介")
                .avatar("https://example.com/avatar1.jpg")
                .status(User.UserStatus.ACTIVE)
                .build();
        entityManager.persist(testUser1);

        // 创建测试用户2
        testUser2 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .passwordHash(passwordEncoder.encode("password456"))
                .displayName("用户2")
                .bio("这是用户2的简介")
                .avatar("https://example.com/avatar2.jpg")
                .status(User.UserStatus.ACTIVE)
                .build();
        entityManager.persist(testUser2);

        entityManager.flush();
    }

    @AfterEach
    void tearDown() {
        entityManager.clear();
    }

    @Test
    @Order(1)
    @DisplayName("测试用户注册 - 验证实体持久化和密码加密")
    void testUserRegistration_WithEntityPersistence() {
        // Given - 准备注册请求
        UserRegisterRequest request = UserRegisterRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("newpassword123")
                .displayName("新用户")
                .build();

        // When - 通过服务层注册用户
        UserDetailDTO result = userService.register(request);

        // Then - 验证返回结果
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getEmail()).isEqualTo("newuser@example.com");
        assertThat(result.getDisplayName()).isEqualTo("新用户");
        assertThat(result.getStatus()).isEqualTo(User.UserStatus.ACTIVE);

        // 验证数据库中的实体
        Optional<User> savedUserOpt = userRepository.findByUsername("newuser");
        assertThat(savedUserOpt).isPresent();
        User savedUser = savedUserOpt.get();

        assertThat(savedUser.getEmail()).isEqualTo("newuser@example.com");
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("newpassword123"); // 密码已加密
        assertThat(passwordEncoder.matches("newpassword123", savedUser.getPasswordHash())).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("测试用户登录 - 验证身份认证和状态检查")
    void testUserLogin_WithAuthentication() {
        // Given - 使用用户名登录
        UserLoginRequest loginRequest = UserLoginRequest.builder()
                .loginId("user1")
                .password("password123")
                .build();

        // When - 执行登录
        UserDetailDTO result = userService.login(loginRequest);

        // Then - 验证登录成功
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("user1");
        assertThat(result.getEmail()).isEqualTo("user1@example.com");
        assertThat(result.getLastLoginAt()).isNotNull();

        // 验证数据库中最后登录时间已更新
        entityManager.flush();
        entityManager.clear();
        User updatedUser = entityManager.find(User.class, testUser1.getId());
        assertThat(updatedUser.getLastLoginAt()).isNotNull();
    }

    @Test
    @Order(3)
    @DisplayName("测试用户登录 - 验证邮箱登录")
    void testUserLogin_WithEmailLogin() {
        // Given - 使用邮箱登录
        UserLoginRequest loginRequest = UserLoginRequest.builder()
                .loginId("user1@example.com")
                .password("password123")
                .build();

        // When - 执行登录
        UserDetailDTO result = userService.login(loginRequest);

        // Then - 验证登录成功
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("user1");
    }

    @Test
    @Order(4)
    @DisplayName("测试用户信息更新 - 验证字段更新")
    void testUserUpdate_WithFieldUpdates() {
        // Given - 准备更新请求
        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .displayName("更新后的用户1")
                .bio("更新后的简介")
                .avatar("https://example.com/new-avatar.jpg")
                .build();

        // When - 执行更新
        UserDetailDTO result = userService.updateUser(testUser1.getId(), updateRequest);

        // Then - 验证更新结果
        assertThat(result.getDisplayName()).isEqualTo("更新后的用户1");
        assertThat(result.getBio()).isEqualTo("更新后的简介");
        assertThat(result.getAvatar()).isEqualTo("https://example.com/new-avatar.jpg");

        // 验证数据库中的变化
        entityManager.flush();
        entityManager.clear();
        User updatedUser = entityManager.find(User.class, testUser1.getId());
        assertThat(updatedUser.getDisplayName()).isEqualTo("更新后的用户1");
        assertThat(updatedUser.getBio()).isEqualTo("更新后的简介");
    }

    @Test
    @Order(5)
    @DisplayName("测试用户活动统计 - 验证文章和评论计数")
    void testUserActivityStats_WithArticleAndCommentCounts() {
        // Given - 为用户创建文章和评论
        for (int i = 0; i < 3; i++) {
            Article article = Article.builder()
                    .title("用户1的文章" + i)
                    .content("这是文章内容，长度足够生成摘要信息")
                    .author(testUser1)
                    .status(Article.ArticleStatus.RELEASE)
                    .likeCount(i * 5)
                    .build();
            entityManager.persist(article);
        }

        // 为用户创建评论（需要先有文章）
        Article article = entityManager.find(Article.class,
                articleRepository.findByAuthor(testUser1, org.springframework.data.domain.PageRequest.of(0, 1))
                        .getContent().getFirst().getId());

        Comment comment = Comment.builder()
                .content("这是一条评论")
                .article(article)
                .commenter(testUser1)
                .likeCount(0)
                .build();
        entityManager.persist(comment);
        entityManager.flush();
        entityManager.clear();

        // When - 获取用户活动统计
        var activityStats = userService.getUserActivity(testUser1.getId());

        // Then - 验证统计数据
        assertThat(activityStats).isNotNull();
        assertThat(activityStats.getUserId()).isEqualTo(testUser1.getId());
        assertThat(activityStats.getArticleCount()).isEqualTo(3L);
        assertThat(activityStats.getCommentCount()).isEqualTo(1L);
    }

    @Test
    @Order(6)
    @DisplayName("测试用户状态管理 - 验证禁用和启用")
    void testUserStatusManagement_WithDeactivationAndActivation() {
        // Given - 用户初始状态为 ACTIVE
        assertThat(testUser1.getStatus()).isEqualTo(User.UserStatus.ACTIVE);

        // When - 禁用用户
        userService.deactivateUser(testUser1.getId());

        // Then - 验证用户被禁用
        entityManager.flush();
        entityManager.clear();
        User disabledUser = entityManager.find(User.class, testUser1.getId());
        assertThat(disabledUser.getStatus()).isEqualTo(User.UserStatus.INACTIVE);

        // When - 重新启用用户
        userService.activateUser(testUser1.getId());

        // Then - 验证用户被启用
        entityManager.flush();
        entityManager.clear();
        User activatedUser = entityManager.find(User.class, testUser1.getId());
        assertThat(activatedUser.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
    }

    @Test
    @Order(7)
    @DisplayName("测试用户状态管理 - 验证锁定和解锁")
    void testUserStatusManagement_WithLockAndUnlock() {
        // Given - 用户初始状态为 ACTIVE

        // When - 锁定用户
        userService.lockUser(testUser1.getId());

        // Then - 验证用户被锁定
        entityManager.flush();
        entityManager.clear();
        User lockedUser = entityManager.find(User.class, testUser1.getId());
        assertThat(lockedUser.getStatus()).isEqualTo(User.UserStatus.LOCKED);

        // When - 解锁用户
        userService.unlockUser(testUser1.getId());

        // Then - 验证用户被解锁
        entityManager.flush();
        entityManager.clear();
        User unlockedUser = entityManager.find(User.class, testUser1.getId());
        assertThat(unlockedUser.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
    }

    @Test
    @Order(8)
    @DisplayName("测试登录失败 - 验证禁用用户无法登录")
    void testLoginFailed_WithInactiveUser() {
        // Given - 禁用用户
        userService.deactivateUser(testUser1.getId());
        entityManager.flush();
        entityManager.clear();

        UserLoginRequest loginRequest = UserLoginRequest.builder()
                .loginId("user1")
                .password("password123")
                .build();

        // When & Then - 尝试登录应该失败
        assertThrows(IllegalStateException.class, () -> {
            userService.login(loginRequest);
        });
    }

    @Test
    @Order(9)
    @DisplayName("测试登录失败 - 验证锁定用户无法登录")
    void testLoginFailed_WithLockedUser() {
        // Given - 锁定用户
        userService.lockUser(testUser1.getId());
        entityManager.flush();
        entityManager.clear();

        UserLoginRequest loginRequest = UserLoginRequest.builder()
                .loginId("user1")
                .password("password123")
                .build();

        // When & Then - 尝试登录应该失败
        assertThrows(IllegalStateException.class, () -> {
            userService.login(loginRequest);
        });
    }

    @Test
    @Order(10)
    @DisplayName("测试登录失败 - 验证错误密码")
    void testLoginFailed_WithWrongPassword() {
        // Given - 错误的密码
        UserLoginRequest loginRequest = UserLoginRequest.builder()
                .loginId("user1")
                .password("wrongpassword")
                .build();

        // When & Then - 尝试登录应该失败
        assertThrows(IllegalArgumentException.class, () -> {
            userService.login(loginRequest);
        });
    }

    @Test
    @Order(11)
    @DisplayName("测试注册失败 - 验证用户名重复")
    void testRegistrationFailed_WithDuplicateUsername() {
        // Given - 使用已存在的用户名
        UserRegisterRequest request = UserRegisterRequest.builder()
                .username("user1") // 已存在
                .email("different@example.com")
                .password("password123")
                .build();

        // When & Then - 应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            userService.register(request);
        });
    }

    @Test
    @Order(12)
    @DisplayName("测试注册失败 - 验证邮箱重复")
    void testRegistrationFailed_WithDuplicateEmail() {
        // Given - 使用已存在的邮箱
        UserRegisterRequest request = UserRegisterRequest.builder()
                .username("differentuser")
                .email("user1@example.com") // 已存在
                .password("password123")
                .build();

        // When & Then - 应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            userService.register(request);
        });
    }

    @Test
    @Order(13)
    @DisplayName("测试事务回滚 - 验证异常时的数据一致性")
    void testTransactionRollback_OnException() {
        // Given - 准备一个会导致异常的操作
        UserRegisterRequest invalidRequest = UserRegisterRequest.builder()
                .username("user1") // 重复的用户名
                .email("user1@example.com") // 重复的邮箱
                .password("password123")
                .build();

        // When & Then - 应该抛出异常，且事务回滚
        assertThrows(IllegalArgumentException.class, () -> {
            userService.register(invalidRequest);
        });

        // 验证没有创建新的用户
        long count = userRepository.count();
        assertThat(count).isEqualTo(2); // 只有 setUp 中创建的 2 个
    }

    @Test
    @Order(14)
    @DisplayName("测试用户查询 - 验证分页和排序")
    void testUserQuery_WithPaginationAndSorting() {
        // Given - 创建更多用户
        for (int i = 0; i < 8; i++) {
            User user = User.builder()
                    .username("user" + (i + 3))
                    .email("user" + (i + 3) + "@example.com")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .displayName("用户" + (i + 3))
                    .status(User.UserStatus.ACTIVE)
                    .build();
            entityManager.persist(user);
        }
        entityManager.flush();

        // When - 分页查询
        var pageResult = userService.getAllUsers(0, 5, "createdAt");

        // Then - 验证分页结果
        assertThat(pageResult.getContent()).hasSize(5);
        assertThat(pageResult.getPage()).isEqualTo(0);
        assertThat(pageResult.getSize()).isEqualTo(5);
        assertThat(pageResult.getTotalElements()).isEqualTo(10); // 2 + 8
    }

    @Test
    @Order(15)
    @DisplayName("测试复杂场景 - 用户注册、登录、发布文章的完整流程")
    void testComplexScenario_FullWorkflow() {
        // Given - 模拟真实场景

        // 1. 新用户注册
        UserRegisterRequest registerRequest = UserRegisterRequest.builder()
                .username("blogger")
                .email("blogger@example.com")
                .password("securepass123")
                .displayName("博客作者")
                .build();
        UserDetailDTO registeredUser = userService.register(registerRequest);

        // 2. 用户登录
        UserLoginRequest loginRequest = UserLoginRequest.builder()
                .loginId("blogger")
                .password("securepass123")
                .build();
        UserDetailDTO loggedInUser = userService.login(loginRequest);

        // 3. 更新用户资料
        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .displayName("专业博主")
                .bio("专注于技术分享")
                .avatar("https://example.com/blogger.jpg")
                .build();
        UserDetailDTO updatedUser = userService.updateUser(loggedInUser.getId(), updateRequest);

        // Then - 验证所有操作的结果
        assertThat(registeredUser.getUsername()).isEqualTo("blogger");
        assertThat(loggedInUser.getLastLoginAt()).isNotNull();
        assertThat(updatedUser.getDisplayName()).isEqualTo("专业博主");
        assertThat(updatedUser.getBio()).isEqualTo("专注于技术分享");

        // 验证数据库状态
        Optional<User> savedUser = userRepository.findByUsername("blogger");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getEmail()).isEqualTo("blogger@example.com");
        assertThat(passwordEncoder.matches("securepass123", savedUser.get().getPasswordHash())).isTrue();
    }
}
