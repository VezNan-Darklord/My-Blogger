package csulzc.My_Personal_Blogger.api.dto.category;

import lombok.Data;
import lombok.Builder;


@Data
@Builder
public class CategoryStatDTO {
    private String categoryName;
    private Long articleCount;
    private Double percentage;
}
