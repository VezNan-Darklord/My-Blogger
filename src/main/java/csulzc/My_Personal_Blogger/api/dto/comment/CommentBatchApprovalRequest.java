package csulzc.My_Personal_Blogger.api.dto.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "评论批量审核请求")
public class CommentBatchApprovalRequest {

    @Schema(description = "评论ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;

    @Schema(description = "审核状态：true=通过，false=不通过", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean approved;
}
