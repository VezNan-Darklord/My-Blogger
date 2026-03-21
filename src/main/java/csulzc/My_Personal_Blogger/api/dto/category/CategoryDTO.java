package csulzc.My_Personal_Blogger.api.dto.category;

import csulzc.My_Personal_Blogger.api.dto.common.BaseDTO;
import lombok.Data;
import lombok.Builder;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 分类DTO
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class CategoryDTO extends BaseDTO {
    private String name;
    private String description;
    private Long parentCategoryId;
    private String parentCategoryName;
    private Integer articleCount;
    private List<CategoryDTO> subCategories;
}