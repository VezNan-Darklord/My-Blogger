package csulzc.My_Personal_Blogger.api.dto.common;

import lombok.Data;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

/**
 * 基础DTO，包含通用字段
 */
@Data
@SuperBuilder
public abstract class BaseDTO {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}