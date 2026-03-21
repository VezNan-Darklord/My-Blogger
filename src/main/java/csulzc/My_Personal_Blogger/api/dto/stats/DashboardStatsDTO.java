package csulzc.My_Personal_Blogger.api.dto.stats;

import csulzc.My_Personal_Blogger.api.dto.category.CategoryStatDTO;
import lombok.Data;
import lombok.Builder;

import java.util.List;

/**
 * 仪表盘统计DTO
 */
@Data
@Builder
public class DashboardStatsDTO {
    private Long totalArticles;
    private Long totalUsers;
    private Long totalComments;
    private Long todayArticles;
    private Long todayComments;
    private Long activeUsers;
    private List<CategoryStatDTO> categoryStats;
    private List<DailyStatDTO> recentTrend;
}
