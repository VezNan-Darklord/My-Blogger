package csulzc.My_Personal_Blogger.api.dto.stats;

import csulzc.My_Personal_Blogger.api.dto.user.UserProfileDTO;
import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
public class PopularArticleDTO {
    private Long id;
    private String title;
    private String summary;
    private UserProfileDTO author;
    private LocalDateTime createdAt;
    private Integer likeCount;
    private Integer commentCount;
    private Double popularityScore;  // 热度分
}