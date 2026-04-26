package csulzc.My_Personal_Blogger.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "file")
public class FileStorageProperties {

    private String uploadDir = "uploads";

    private long maxFileSize = 5 * 1024 * 1024; // 5MB

    private String[] allowedFileTypes = {"image/jpeg", "image/png", "image/gif", "image/webp"};
}
