package csulzc.My_Personal_Blogger.config;

import csulzc.My_Personal_Blogger.domain.entity.User;
import csulzc.My_Personal_Blogger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            String randomPassword = generateSecurePassword();
            User admin = User.builder()
                    .username("admin")
                    .email("admin@example.com")
                    .passwordHash(passwordEncoder.encode(randomPassword))
                    .displayName("系统管理员")
                    .role(User.UserRole.SUPER_ADMIN)
                    .status(User.UserStatus.ACTIVE)
                    .build();

            userRepository.save(admin);

            log.warn("========================================");
            log.warn("  默认管理员账号已创建");
            log.warn("  用户名: admin");
            log.warn("  密码: {}", randomPassword);
            log.warn("  ⚠️  请立即修改密码！");
            log.warn("========================================");
            System.out.println("=== 默认管理员账号已创建 ===");
            System.out.println("用户名: admin");
            System.out.println("密码: " + randomPassword);
            System.out.println("⚠️  请登录后立即修改密码！");
        }
    }

    private String generateSecurePassword() {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        String password = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return password.substring(0, 16);
    }
}
