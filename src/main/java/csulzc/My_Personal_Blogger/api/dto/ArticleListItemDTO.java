package csulzc.My_Personal_Blogger.api.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章列表项DTO（用于列表页）
 */
@Data
@Builder
public class ArticleListItemDTO extends BaseDTO {
    private Long id;
    private String title;
    private String summary;
    private String coverImage;
    private UserProfileDTO author;
    private List<CategoryDTO> categories;
    private LocalDateTime createdAt;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
}