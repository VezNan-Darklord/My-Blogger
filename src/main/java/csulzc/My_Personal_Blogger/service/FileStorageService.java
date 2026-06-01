package csulzc.My_Personal_Blogger.service;

import csulzc.My_Personal_Blogger.config.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileStorageProperties fileStorageProperties;

    private Path fileStorageLocation;

    @PostConstruct
    public void init() {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("无法创建文件存储目录", ex);
        }
    }

    /**
     * 上传文件
     */
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        validateFile(file);

        try {
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null) {
                throw new IllegalArgumentException("文件名无效");
            }

            String fileExtension = getFileExtension(originalFileName);
            String newFileName = UUID.randomUUID().toString() + fileExtension;

            Path targetLocation = this.fileStorageLocation.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return newFileName;
        } catch (IOException ex) {
            throw new RuntimeException("文件上传失败: " + file.getOriginalFilename(), ex);
        }
    }

    /**
     * 加载文件资源
     */
    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("文件不存在: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("文件路径错误: " + fileName, ex);
        }
    }

    /**
     * 删除文件
     */
    public void deleteFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("删除文件失败: " + fileName, ex);
        }
    }

    /**
     * 获取文件的完整访问路径
     */
    public String getFileUrl(String fileName) {
        return "/api/files/" + fileName;
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        long fileSize = file.getSize();
        long maxFileSize = fileStorageProperties.getMaxFileSize();

        if (fileSize > maxFileSize) {
            throw new RuntimeException("文件大小超过限制，最大允许: " + (maxFileSize / 1024 / 1024) + "MB");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("无法识别文件类型");
        }

        String[] allowedTypes = fileStorageProperties.getAllowedFileTypes();
        if (!Arrays.asList(allowedTypes).contains(contentType)) {
            throw new RuntimeException("不支持的文件类型: " + contentType +
                    "，仅支持: " + String.join(", ", allowedTypes));
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex).toLowerCase();
    }

    /**
     * 将文件转换为Base64编码
     */
    public String encodeFileToBase64(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            byte[] fileContent = Files.readAllBytes(filePath);
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException ex) {
            throw new RuntimeException("文件转Base64失败: " + fileName, ex);
        }
    }

    /**
     * 将Base64字符串解码并保存为文件
     */
    public String decodeBase64ToFile(String base64Data, String originalFileName) {
        try {
            // 移除可能的data URI前缀（如：data:image/jpeg;base64,）
            String base64Content = base64Data;
            if (base64Data.contains(",")) {
                base64Content = base64Data.split(",", 2)[1];
            }

            // 解码Base64数据
            byte[] decodedBytes = Base64.getDecoder().decode(base64Content);

            // 生成新文件名
            String fileExtension = getFileExtension(originalFileName);
            if (fileExtension.isEmpty()) {
                // 如果没有扩展名，尝试从MIME类型推断
                fileExtension = getFileExtensionFromMimeType(base64Data);
            }
            String newFileName = UUID.randomUUID().toString() + fileExtension;

            // 保存文件
            Path targetLocation = this.fileStorageLocation.resolve(newFileName);
            Files.write(targetLocation, decodedBytes);

            return newFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Base64转文件失败", ex);
        }
    }

    /**
     * 从MIME类型获取文件扩展名
     */
    private String getFileExtensionFromMimeType(String mimeType) {
        if (mimeType.contains("image/jpeg")) {
            return ".jpg";
        } else if (mimeType.contains("image/png")) {
            return ".png";
        } else if (mimeType.contains("image/gif")) {
            return ".gif";
        } else if (mimeType.contains("image/webp")) {
            return ".webp";
        }
        return ""; // 默认无扩展名
    }
}
