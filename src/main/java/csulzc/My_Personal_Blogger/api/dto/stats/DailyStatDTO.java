package csulzc.My_Personal_Blogger.api.dto.stats;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;

@Data
@Builder
public class DailyStatDTO {
    private LocalDate date;
    private Long articleCount;
    private Long commentCount;
    private Long userCount;
}
