package csulzc.My_Personal_Blogger.api.dto.category;

import lombok.Data;
import lombok.Builder;

import java.util.List;

/**
 * 分类树DTO（用于前端下拉树）
 */
@Data
@Builder
public class CategoryTreeDTO {
    private Long id;
    private String name;
    private String description;
    private Integer articleCount;
    private List<CategoryTreeDTO> children;
}