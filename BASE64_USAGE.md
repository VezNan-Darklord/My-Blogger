# Base64 文件编码功能使用说明

## 概述

本项目现已支持Base64文件编码功能，允许将文件转换为Base64字符串以及将Base64字符串保存为文件。

## API 端点

### 1. 将文件转换为Base64编码

**请求：**
```
GET /api/files/{fileName}/base64
```

**响应示例：**
```json
{
  "code": 200,
  "message": "文件转Base64成功",
  "data": {
    "fileName": "abc123.jpg",
    "fileUrl": "/api/files/abc123.jpg",
    "fileType": "image/jpeg",
    "fileSize": 1024,
    "base64Data": "iVBORw0KGgoAAAANSUhEUgAAAAUA..."
  }
}
```

### 2. 上传Base64编码的文件

**请求：**
```
POST /api/files/upload-base64
Content-Type: application/json

{
  "base64Data": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD...",
  "fileName": "example.jpg"
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "Base64文件上传成功",
  "data": {
    "fileName": "xyz789.jpg",
    "fileUrl": "/api/files/xyz789.jpg",
    "fileType": "image/jpeg",
    "fileSize": 2048,
    "base64Data": null
  }
}
```

## 使用场景

1. **前端直接嵌入图片**：将图片转换为Base64后可以直接在HTML/CSS中使用
2. **API数据传输**：在小文件传输场景中避免多次HTTP请求
3. **数据持久化**：将小文件存储在数据库中

## 注意事项

1. Base64编码会使文件大小增加约33%
2. 建议仅对小文件（<1MB）使用Base64编码
3. 大文件建议使用传统的文件上传方式
4. 支持的图片格式：JPEG, PNG, GIF, WebP

## 代码示例

### Java后端调用
```java
// 将文件转换为Base64
String base64Data = fileStorageService.encodeFileToBase64("filename.jpg");

// 将Base64保存为文件
String fileName = fileStorageService.decodeBase64ToFile(base64Data, "newfile.jpg");
```

### JavaScript前端调用
```javascript
// 上传图片并获取Base64
fetch('/api/files/upload', {
  method: 'POST',
  body: formData
})
.then(response => response.json())
.then(data => {
  // 获取文件的Base64编码
  fetch(`/api/files/${data.data.fileName}/base64`)
    .then(res => res.json())
    .then(result => {
      const base64 = result.data.base64Data;
      // 在img标签中使用
      document.getElementById('image').src = `data:${result.data.fileType};base64,${base64}`;
    });
});

// 直接上传Base64数据
fetch('/api/files/upload-base64', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    base64Data: canvas.toDataURL('image/jpeg'),
    fileName: 'canvas-image.jpg'
  })
})
.then(response => response.json())
.then(data => console.log('上传成功:', data));
```
