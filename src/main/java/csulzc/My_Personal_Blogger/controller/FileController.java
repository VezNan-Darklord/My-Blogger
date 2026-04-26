package csulzc.My_Personal_Blogger.controller;

import csulzc.My_Personal_Blogger.api.dto.common.FileUploadResponse;
import csulzc.My_Personal_Blogger.api.response.Result;
import csulzc.My_Personal_Blogger.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * 上传图片文件
     */
    @PostMapping("/upload")
    public ResponseEntity<Result<FileUploadResponse>> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileName = fileStorageService.storeFile(file);

        FileUploadResponse response = FileUploadResponse.builder()
                .fileName(fileName)
                .fileUrl(fileStorageService.getFileUrl(fileName))
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .build();

        return ResponseEntity.ok(Result.success(response, "文件上传成功"));
    }

    /**
     * 下载/访问文件
     */
    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        Resource resource = fileStorageService.loadFileAsResource(fileName);

        String contentType = "application/octet-stream";
        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        contentType = switch (fileExtension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> contentType;
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/{fileName}")
    public ResponseEntity<Result<Void>> deleteFile(@PathVariable String fileName) {
        fileStorageService.deleteFile(fileName);
        return ResponseEntity.ok(Result.success(null, "文件删除成功"));
    }
}
