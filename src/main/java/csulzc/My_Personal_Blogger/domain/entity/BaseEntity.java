package csulzc.My_Personal_Blogger.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;  // 注意这个

import java.time.LocalDateTime;
import java.util.Arrays;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder  // 使用 @SuperBuilder 而不是 @Builder
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Builder.Default
    private Long version = 0L;

    @Column(updatable = false)  // 创建后不可更新
    protected LocalDateTime createdAt;

    protected LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}