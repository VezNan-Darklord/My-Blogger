package csulzc.My_Personal_Blogger.api.dto.comment;

import lombok.Data;
import lombok.Builder;
import jakarta.validation.constraints.*;

/**
 * 评论创建请求DTO
 */
@Data
@Builder
public class CommentCreateRequest {

    @NotBlank(message = "评论内容不能为空")
    @Size(min = 1, max = 500, message = "评论长度必须在1-500之间")
    private String content;

    private Long parentCommentId;  // 如果是回复，填父评论ID
}
