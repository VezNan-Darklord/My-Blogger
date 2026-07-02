package csulzc.My_Personal_Blogger.api.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "批量操作ID请求")
public class BatchIdRequest {

    @NotNull(message = "ID列表不能为空")
    @NotEmpty(message = "ID列表不能为空")
    @Schema(description = "ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;
}
