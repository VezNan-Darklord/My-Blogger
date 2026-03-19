package csulzc.My_Personal_Blogger.api.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 评论回复DTO（扁平化结构）
 */
@Data
@Builder
public class CommentReplyDTO {
    private Long id;
    private String content;
    private UserProfileDTO commenter;
    private UserProfileDTO replyToUser;  // 回复给谁
    private LocalDateTime createdAt;
}