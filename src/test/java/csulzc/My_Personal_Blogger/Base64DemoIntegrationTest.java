package csulzc.My_Personal_Blogger;

import csulzc.My_Personal_Blogger.service.FileStorageService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Base64功能集成测试")
public class Base64DemoIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FileStorageService fileStorageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @Order(1)
    @DisplayName("测试文件上传后转换为Base64")
    void testFileUploadThenConvertToBase64() throws Exception {
        // 上传文件
        byte[] content = "test image content for base64".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-integration.jpg",
                "image/jpeg",
                content
        );

        String uploadResponse = mockMvc.perform(multipart("/api/files/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 从响应中提取文件名（简化处理）
        String fileName = extractFileNameFromResponse(uploadResponse);

        // 将文件转换为Base64
        String base64Response = mockMvc.perform(get("/api/files/{fileName}/base64", fileName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.base64Data").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 验证Base64数据存在
        assert base64Response.contains("base64Data");
    }

    @Test
    @Order(2)
    @DisplayName("测试Base64上传并保存为文件")
    void testBase64UploadAndSaveAsFile() throws Exception {
        // 准备Base64数据
        String originalContent = "test base64 upload content";
        String base64Data = Base64.getEncoder().encodeToString(originalContent.getBytes());

        String requestBody = String.format(
                "{\"base64Data\":\"%s\",\"fileName\":\"test-upload.txt\"}",
                base64Data
        );

        mockMvc.perform(post("/api/files/upload-base64")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Base64文件上传成功"))
                .andExpect(jsonPath("$.data.fileName").exists());
    }

    @Test
    @Order(3)
    @DisplayName("测试带data URI前缀的Base64上传")
    void testBase64UploadWithDataUriPrefix() throws Exception {
        // 准备带data URI前缀的Base64数据
        String originalContent = "test with data uri";
        String base64Data = Base64.getEncoder().encodeToString(originalContent.getBytes());
        String dataUri = "data:text/plain;base64," + base64Data;

        String requestBody = String.format(
                "{\"base64Data\":\"%s\",\"fileName\":\"test-datauri.txt\"}",
                dataUri
        );

        mockMvc.perform(post("/api/files/upload-base64")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Base64文件上传成功"));
    }

    /**
     * 从上传响应中提取文件名（简化实现）
     */
    private String extractFileNameFromResponse(String response) {
        // 实际项目中应该使用JSON解析库
        int start = response.indexOf("\"fileName\":\"") + 12;
        int end = response.indexOf("\"", start);
        if (start > 11 && end > start) {
            return response.substring(start, end);
        }
        return "test-integration.jpg"; // 默认返回值
    }
}
