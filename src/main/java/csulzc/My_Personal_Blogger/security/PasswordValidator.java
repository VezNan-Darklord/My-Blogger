package csulzc.My_Personal_Blogger.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    private static final Pattern HAS_UPPER = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWER = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");
    private static final Pattern HAS_SPECIAL = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?~]");

    public void validate(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        if (password.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("密码长度至少为8个字符");
        }

        if (password.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("密码长度不能超过128个字符");
        }

        if (!HAS_UPPER.matcher(password).find()) {
            throw new IllegalArgumentException("密码必须包含至少一个大写字母");
        }

        if (!HAS_LOWER.matcher(password).find()) {
            throw new IllegalArgumentException("密码必须包含至少一个小写字母");
        }

        if (!HAS_DIGIT.matcher(password).find()) {
            throw new IllegalArgumentException("密码必须包含至少一个数字");
        }

        if (!HAS_SPECIAL.matcher(password).find()) {
            throw new IllegalArgumentException("密码必须包含至少一个特殊字符");
        }

        if (isCommonPassword(password)) {
            throw new IllegalArgumentException("密码过于简单，请使用更复杂的密码");
        }
    }

    private boolean isCommonPassword(String password) {
        String lowerPassword = password.toLowerCase();
        return lowerPassword.equals("12345678") ||
                lowerPassword.equals("admin123") ||
                lowerPassword.equals("password123") ||
                lowerPassword.contains("admin") ||
                lowerPassword.contains("password") ||
                lowerPassword.contains("querty");
    }
}
