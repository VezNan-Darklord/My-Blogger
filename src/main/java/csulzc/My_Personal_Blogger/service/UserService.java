package csulzc.My_Personal_Blogger.service;

import csulzc.My_Personal_Blogger.api.dto.common.PageResponseDTO;
import csulzc.My_Personal_Blogger.api.dto.user.*;
import csulzc.My_Personal_Blogger.domain.entity.Article;
import csulzc.My_Personal_Blogger.domain.entity.User;
import csulzc.My_Personal_Blogger.repository.ArticleRepository;
import csulzc.My_Personal_Blogger.repository.CommentRepository;
import csulzc.My_Personal_Blogger.repository.UserRepository;
import csulzc.My_Personal_Blogger.security.JwtTokenProvider;
import csulzc.My_Personal_Blogger.security.PasswordValidator;
import csulzc.My_Personal_Blogger.security.SecurityContextUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityContextUtil securityContextUtil;
    private final PasswordValidator passwordValidator;

    // ==================== 用户注册与登录 ====================

    /**
     * 用户注册
     */
    @Transactional
    public UserDetailDTO register(UserRegisterRequest request) {
        passwordValidator.validate(request.getPassword());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("邮箱已被注册");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(StringUtils.hasText(request.getDisplayName())
                        ? request.getDisplayName()
                        : request.getUsername())
                .status(User.UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);
        return convertToDetailDTO(savedUser);
    }

    /**
     * 用户登录
     */
    @Transactional
    public LoginResponseDTO loginWithToken(UserLoginRequest request) {
        User user = findUserByLoginId(request.getLoginId());

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("密码错误");
        }

        // 检查用户状态
        if (user.getStatus() == User.UserStatus.INACTIVE) {
            throw new IllegalStateException("用户已被禁用");
        }
        if (user.getStatus() == User.UserStatus.LOCKED) {
            throw new IllegalStateException("用户已被锁定");
        }

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        // 生成Token（包含角色信息）
        String role = updatedUser.getRole().name();
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), User.UserRole.valueOf(role), jwtTokenProvider.getJwtProperties().getExpiration());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername(), User.UserRole.valueOf(role), jwtTokenProvider.getJwtProperties().getRefreshExpiration());

        UserDetailDTO userDetailDTO = convertToDetailDTO(updatedUser);

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getJwtProperties().getExpiration())
                .user(userDetailDTO)
                .build();
    }

    public LoginResponseDTO refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("无效的刷新令牌");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));

        // 生成新的Token（包含角色信息）
        String role = user.getRole().name();
        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, username, User.UserRole.valueOf(role), jwtTokenProvider.getJwtProperties().getExpiration());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId, username, User.UserRole.valueOf(role), jwtTokenProvider.getJwtProperties().getRefreshExpiration());

        UserDetailDTO userDetailDTO = convertToDetailDTO(user);

        return LoginResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getJwtProperties().getExpiration())
                .user(userDetailDTO)
                .build();
    }


    /**
     * 根据登录标识查找用户（用户名或邮箱）
     */
    private User findUserByLoginId(String loginId) {
        Optional<User> user = userRepository.findByUsername(loginId);
        if (user.isEmpty()) {
            user = userRepository.findByEmail(loginId);
        }
        if (user.isEmpty()) {
            throw new EntityNotFoundException("用户不存在");
        }
        return user.get();
    }

    // ==================== 用户信息查询 ====================

    /**
     * 根据 ID 获取用户详情
     */
    public UserDetailDTO getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        return convertToDetailDTO(user);
    }

    /**
     * 根据用户名获取用户详情
     */
    public UserDetailDTO getUserDetailByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        return convertToDetailDTO(user);
    }

    /**
     * 获取用户公开资料
     */
    public UserProfileDTO getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        return convertToProfileDTO(user);
    }

    /**
     * 获取用户公开资料（通过用户名）
     */
    public UserProfileDTO getUserProfileByUsername(String username) {
        User user = userRepository.findByUsernameWithArticles(username)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        return convertToProfileDTO(user);
    }

    // ==================== 用户信息更新 ====================

    /**
     * 更新用户信息
     */
    @Transactional
    public UserDetailDTO updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));

        // 更新字段
        if (StringUtils.hasText(request.getDisplayName())) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        User updatedUser = userRepository.save(user);
        return convertToDetailDTO(updatedUser);
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        Long currentUserId = securityContextUtil.getCurrentUserId();

        if (!currentUserId.equals(userId) && !securityContextUtil.isAdmin()) {
            throw new SecurityException("无权限修改此用户密码");
        }

        passwordValidator.validate(newPassword);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("原密码错误");
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("新密码不能与原密码相同");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * 重置密码（管理员功能）
     */
    public void resetPassword(Long userId, String newPassword) {
        passwordValidator.validate(newPassword);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }


    // ==================== 用户统计信息 ====================

    /**
     * 获取用户活动统计
     */
    public UserActivityDTO getUserActivity(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));

        long articleCount = articleRepository.countByAuthor(user);
        long commentCount = commentRepository.countByCommenter(user);

        return UserActivityDTO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .articleCount(articleCount)
                .commentCount(commentCount)
                .likeReceived(calculateTotalLikes(user))
                .lastActiveAt(user.getUpdatedAt())
                .build();
    }

    /**
     * 计算用户获得的总点赞数
     */
    private long calculateTotalLikes(User user) {
        return user.getArticles().stream()
                .mapToLong(Article::getLikeCount)
                .sum();
    }

    /**
     * 统计用户文章数
     */
    public long countUserArticles(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        return articleRepository.countByAuthor(user);
    }

    /**
     * 统计用户评论数
     */
    public long countUserComments(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        return commentRepository.countByCommenter(user);
    }

    // ==================== 用户管理功能 ====================

    /**
     * 启用用户
     */
    @Transactional
    public void activateUser(Long userId) {
        if (!securityContextUtil.isAdmin()) {
            throw new SecurityException("只有管理员可以启用用户");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);
    }

    /**
     * 禁用用户
     */
    @Transactional
    public void deactivateUser(Long userId) {
        if (!securityContextUtil.isAdmin()) {
            throw new SecurityException("只有管理员可以禁用用户");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        user.setStatus(User.UserStatus.INACTIVE);
        userRepository.save(user);
    }

    /**
     * 锁定用户
     */
    @Transactional
    public void lockUser(Long userId) {
        if (!securityContextUtil.isAdmin()) {
            throw new SecurityException("只有管理员可以锁定用户");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        user.setStatus(User.UserStatus.LOCKED);
        userRepository.save(user);
    }

    /**
     * 解锁用户
     */
    @Transactional
    public void unlockUser(Long userId) {
        if (!securityContextUtil.isAdmin()) {
            throw new SecurityException("只有管理员可以解锁用户");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);
    }

    /**
     * 删除用户（软删除或硬删除）
     */
    @Transactional
    public void deleteUser(Long userId, boolean softDelete) {
        if (!securityContextUtil.isAdmin()) {
            throw new SecurityException("只有管理员可以删除用户");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));

        if (softDelete) {
            user.setStatus(User.UserStatus.INACTIVE);
            userRepository.save(user);
        } else {
            commentRepository.deleteByArticle(null);
            userRepository.delete(user);
        }
    }

    // ==================== 用户查询与搜索 ====================

    /**
     * 分页查询所有用户
     */
    public PageResponseDTO<UserProfileDTO> getAllUsers(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<User> userPage = userRepository.findAll(pageable);

        List<UserProfileDTO> dtos = userPage.getContent().stream()
                .map(this::convertToProfileDTO)
                .collect(Collectors.toList());

        return PageResponseDTO.<UserProfileDTO>builder()
                .content(dtos)
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .first(userPage.isFirst())
                .last(userPage.isLast())
                .build();
    }

    /**
     * 根据状态查询用户
     */
    public List<UserDetailDTO> getUsersByStatus(User.UserStatus status) {
        return userRepository.findAll().stream()
                .filter(u -> u.getStatus() == status)
                .map(this::convertToDetailDTO)
                .collect(Collectors.toList());
    }

    /**
     * 搜索用户（根据用户名或显示名称）
     */
    public PageResponseDTO<UserProfileDTO> searchUsers(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getUsername().contains(keyword) ||
                        (u.getDisplayName() != null && u.getDisplayName().contains(keyword)))
                .toList();

        int start = page * size;
        int end = Math.min(start + size, users.size());

        List<UserProfileDTO> dtos = users.stream()
                .skip(start)
                .limit(size)
                .map(this::convertToProfileDTO)
                .collect(Collectors.toList());

        return PageResponseDTO.<UserProfileDTO>builder()
                .content(dtos)
                .page(page)
                .size(size)
                .totalElements((long) users.size())
                .totalPages((int) Math.ceil((double) users.size() / size))
                .first(page == 0)
                .last(end >= users.size())
                .build();
    }

    // ==================== 数据统计功能 ====================

    /**
     * 统计活跃用户数
     */
    public long countActiveUsers() {
        return userRepository.countByStatus(User.UserStatus.ACTIVE);
    }

    /**
     * 统计禁用用户数
     */
    public long countInactiveUsers() {
        return userRepository.countByStatus(User.UserStatus.INACTIVE);
    }

    /**
     * 统计锁定用户数
     */
    public long countLockedUsers() {
        return userRepository.countByStatus(User.UserStatus.LOCKED);
    }

    /**
     * 获取总用户数
     */
    public long getTotalUserCount() {
        return userRepository.count();
    }

    /**
     * 获取最近登录的用户列表
     */
    public List<UserActivityDTO> getRecentlyActiveUsers(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("lastLoginAt").descending());
        Page<User> userPage = userRepository.findAll(pageable);

        return userPage.getContent().stream()
                .map(user -> {
                    long articleCount = articleRepository.countByAuthor(user);
                    long commentCount = commentRepository.countByCommenter(user);

                    return UserActivityDTO.builder()
                            .userId(user.getId())
                            .username(user.getUsername())
                            .displayName(user.getDisplayName())
                            .articleCount(articleCount)
                            .commentCount(commentCount)
                            .likeReceived(calculateTotalLikes(user))
                            .lastActiveAt(user.getLastLoginAt() != null ? user.getLastLoginAt() : user.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ==================== 辅助方法 ====================

    /**
     * 转换为用户详情 DTO
     */
    private UserDetailDTO convertToDetailDTO(User user) {
        long articleCount = articleRepository.countByAuthor(user);
        long commentCount = commentRepository.countByCommenter(user);

        return UserDetailDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .articleCount(articleCount)
                .commentCount(commentCount)
                .favoriteCount(0L) // 可根据需求扩展
                .build();
    }

    /**
     * 转换为用户资料 DTO
     */
    private UserProfileDTO convertToProfileDTO(User user) {
        long articleCount = articleRepository.countByAuthor(user);

        return UserProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .createdAt(user.getCreatedAt())
                .articleCount(articleCount)
                .followerCount(0L) // 可根据需求扩展
                .build();
    }

    /**
     * 检查用户是否存在
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * 检查邮箱是否已注册
     */
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}