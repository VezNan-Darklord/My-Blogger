package csulzc.My_Personal_Blogger.api.dto;

import lombok.Data;
import lombok.Builder;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 评论DTO
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class CommentDTO extends BaseDTO {
    private String content;
    private UserProfileDTO commenter;
    private Long articleId;
    private Long parentCommentId;
    private Integer replyCount;
    private List<CommentDTO> replies;  // 回复列表（懒加载）
    private Integer likeCount;
    private Boolean isLiked;
}