package csulzc.My_Personal_Blogger.api.dto;

import lombok.Data;
import lombok.Builder;
import jakarta.validation.constraints.*;

/**
 * 分类创建/更新请求DTO
 */
@Data
@Builder
public class CategoryRequest {

    @NotBlank(message = "分类名称不能为空")
    @Size(min = 2, max = 20, message = "分类名称长度必须在2-20之间")
    private String name;

    private String description;

    private Long parentCategoryId;  // 父分类ID
}