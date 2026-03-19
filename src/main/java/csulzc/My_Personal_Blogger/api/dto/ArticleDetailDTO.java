package csulzc.My_Personal_Blogger.api.dto;

import lombok.Data;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import csulzc.My_Personal_Blogger.domain.entity.Article;

import java.util.List;

/**
 * 文章详情DTO（用于详情页）
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class ArticleDetailDTO extends BaseDTO {
    private String title;
    private String content;
    private String summary;
    private String coverImage;
    private UserProfileDTO author;
    private List<CategoryDTO> categories;
    private List<String> tags;
    private Article.ArticleStatus status;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    private Boolean isLiked;        // 当前用户是否点赞
    private Boolean isFavorited;    // 当前用户是否收藏
    private List<CommentDTO> topComments;  // 前几条评论
}