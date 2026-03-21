package csulzc.My_Personal_Blogger.api.dto.user;

import csulzc.My_Personal_Blogger.api.dto.common.BaseDTO;
import lombok.Data;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import csulzc.My_Personal_Blogger.domain.entity.User;

import java.time.LocalDateTime;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class UserDetailDTO extends BaseDTO {
    private String username;
    private String email;
    private String displayName;
    private String avatar;
    private String bio;
    private User.UserStatus status;
    private LocalDateTime lastLoginAt;
    private Long articleCount;
    private Long commentCount;
    private Long favoriteCount;
}