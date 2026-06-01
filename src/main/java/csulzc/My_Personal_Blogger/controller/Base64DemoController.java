package csulzc.My_Personal_Blogger.controller;

import csulzc.My_Personal_Blogger.api.response.Result;
import csulzc.My_Personal_Blogger.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Base64功能演示控制器
 * 提供简单的Base64编码解码示例
 */
@RestController
@RequestMapping("/api/demo/base64")
@RequiredArgsConstructor
public class Base64DemoController {

    private final FileStorageService fileStorageService;

    /**
     * 简单Base64编码示例
     */
    @GetMapping("/encode")
    public ResponseEntity<Result<String>> encodeExample(@RequestParam String text) {
        try {
            byte[] bytes = text.getBytes();
            String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
            return ResponseEntity.ok(Result.success(base64, "编码成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "编码失败: " + e.getMessage()));
        }
    }

    /**
     * 简单Base64解码示例
     */
    @GetMapping("/decode")
    public ResponseEntity<Result<String>> decodeExample(@RequestParam String base64) {
        try {
            byte[] bytes = java.util.Base64.getDecoder().decode(base64);
            String text = new String(bytes);
            return ResponseEntity.ok(Result.success(text, "解码成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "解码失败: " + e.getMessage()));
        }
    }
}
