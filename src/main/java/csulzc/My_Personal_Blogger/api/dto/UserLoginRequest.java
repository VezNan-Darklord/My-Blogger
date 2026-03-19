package csulzc.My_Personal_Blogger.api.dto;

import lombok.Data;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import csulzc.My_Personal_Blogger.domain.entity.User;
import jakarta.validation.constraints.*;

@Data
@Builder
public class UserLoginRequest {

    @NotBlank(message = "用户名/邮箱不能为空")
    private String loginId;  // 可以是用户名或邮箱

    @NotBlank(message = "密码不能为空")
    private String password;
}
