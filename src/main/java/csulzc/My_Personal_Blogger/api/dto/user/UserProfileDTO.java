package csulzc.My_Personal_Blogger.api.dto.user;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
public class UserProfileDTO {
    private Long id;
    private String username;
    private String displayName;
    private String avatar;  // 头像URL
    private String bio;      // 个人简介
    private LocalDateTime createdAt;
    private Long articleCount;
    private Long followerCount;
}