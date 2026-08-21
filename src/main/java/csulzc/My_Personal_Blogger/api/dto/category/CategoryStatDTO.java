package csulzc.My_Personal_Blogger.api.dto.category;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


@Data
@SuperBuilder
@NoArgsConstructor
public class CategoryStatDTO {
    private String categoryName;
    private Long articleCount;
    private Double percentage;
}
