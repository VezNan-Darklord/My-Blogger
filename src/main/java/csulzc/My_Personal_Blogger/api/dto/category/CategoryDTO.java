package csulzc.My_Personal_Blogger.api.dto.category;

import csulzc.My_Personal_Blogger.api.dto.common.BaseDTO;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 分类DTO
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CategoryDTO extends BaseDTO {
    private Long id;
    private String name;
    private String description;
    private Long parentCategoryId;
    private String parentCategoryName;
    private Integer articleCount;
    private List<CategoryDTO> subCategories;
}