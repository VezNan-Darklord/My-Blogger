package csulzc.My_Personal_Blogger.config;

import csulzc.My_Personal_Blogger.domain.entity.User;
import csulzc.My_Personal_Blogger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@example.com")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .displayName("系统管理员")
                    .role(User.UserRole.SUPER_ADMIN)
                    .status(User.UserStatus.ACTIVE)
                    .build();

            userRepository.save(admin);
            System.out.println("=== 默认管理员账号已创建 ===");
            System.out.println("用户名: admin");
            System.out.println("密码: admin123");
            System.out.println("请登录后立即修改密码！");
        }
    }
}
