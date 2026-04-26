package csulzc.My_Personal_Blogger.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret = "your-secret-key-change-this-in-production-must-be-at-least-256-bits-long";

    private long expiration = 86400000; // 24小时(毫秒)

    private long refreshExpiration = 604800000; // 7天(毫秒)

    private String tokenPrefix = "Bearer ";

    private String header = "Authorization";
}
