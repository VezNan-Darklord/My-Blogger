package csulzc.My_Personal_Blogger.service;

import csulzc.My_Personal_Blogger.api.dto.dashboard.DashboardStatsDTO;
import csulzc.My_Personal_Blogger.domain.entity.Article;
import csulzc.My_Personal_Blogger.domain.entity.User;
import csulzc.My_Personal_Blogger.repository.ArticleRepository;
import csulzc.My_Personal_Blogger.repository.CommentRepository;
import csulzc.My_Personal_Blogger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;

    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats() {
        return DashboardStatsDTO.builder()
                .userStats(getUserStats())
                .articleStats(getArticleStats())
                .commentStats(getCommentStats())
                .additionalMetrics(getAdditionalMetrics())
                .build();
    }

    private DashboardStatsDTO.UserStats getUserStats() {
        Long totalUsers = userRepository.count();

        Long activeUsers = userRepository.countByStatus(User.UserStatus.ACTIVE);

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        Long newUsersToday = userRepository.countByCreatedAtAfter(startOfToday);

        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        Long newUsersThisMonth = userRepository.countByCreatedAtAfter(startOfMonth);

        return DashboardStatsDTO.UserStats.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .newUsersToday(newUsersToday)
                .newUsersThisMonth(newUsersThisMonth)
                .build();
    }

    private DashboardStatsDTO.ArticleStats getArticleStats() {
        Long totalArticles = articleRepository.count();

        Long publishedArticles = articleRepository.countByStatus(Article.ArticleStatus.RELEASE);

        Long draftArticles = articleRepository.countByStatus(Article.ArticleStatus.DRAFT);

        Long totalViews = articleRepository.sumViewCount();

        Long totalLikes = articleRepository.sumLikeCount();

        return DashboardStatsDTO.ArticleStats.builder()
                .totalArticles(totalArticles)
                .publishedArticles(publishedArticles)
                .draftArticles(draftArticles)
                .totalViews(totalViews)
                .totalLikes(totalLikes)
                .build();
    }

    private DashboardStatsDTO.CommentStats getCommentStats() {
        Long totalComments = commentRepository.count();

        Long pendingComments = commentRepository.countByIsApproved(false);

        Long approvedComments = commentRepository.countByIsApproved(true);

        return DashboardStatsDTO.CommentStats.builder()
                .totalComments(totalComments)
                .pendingComments(pendingComments)
                .approvedComments(approvedComments)
                .build();
    }

    private Map<String, Object> getAdditionalMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("topCategories", getCategoryStats());
        metrics.put("recentActivity", getRecentActivity());

        return metrics;
    }

    private Object getCategoryStats() {
        return Map.of("message", "分类统计功能待实现");
    }

    private Object getRecentActivity() {
        return Map.of("message", "最近活动功能待实现");
    }

    public boolean isAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return user.getRole() == User.UserRole.ADMIN
                || user.getRole() == User.UserRole.SUPER_ADMIN;
    }

    @Transactional(timeout = 30)
    public void promoteToAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        if (user.getRole() == User.UserRole.ADMIN || user.getRole() == User.UserRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("该用户已经是管理员");
        }

        user.setRole(User.UserRole.ADMIN);
        userRepository.save(user);
    }

    @Transactional(timeout = 30)
    public void demoteFromAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        if (user.getRole() == User.UserRole.USER) {
            throw new IllegalArgumentException("该用户不是管理员");
        }

        if (user.getRole() == User.UserRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("不能降级超级管理员");
        }

        user.setRole(User.UserRole.USER);
        userRepository.save(user);
    }
}
