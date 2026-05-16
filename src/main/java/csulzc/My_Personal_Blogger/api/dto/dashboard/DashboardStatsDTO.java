package csulzc.My_Personal_Blogger.api.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {

    private UserStats userStats;
    private ArticleStats articleStats;
    private CommentStats commentStats;
    private Map<String, Object> additionalMetrics;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserStats {
        private Long totalUsers;
        private Long activeUsers;
        private Long newUsersToday;
        private Long newUsersThisMonth;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ArticleStats {
        private Long totalArticles;
        private Long publishedArticles;
        private Long draftArticles;
        private Long totalViews;
        private Long totalLikes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommentStats {
        private Long totalComments;
        private Long pendingComments;
        private Long approvedComments;
    }
}
