package csulzc.My_Personal_Blogger.api.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {

    private String fileName;

    private String fileUrl;

    private String fileType;

    private long fileSize;

    private String base64Data; // Base64编码数据（可选）
}
