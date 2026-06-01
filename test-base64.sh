#!/bin/bash
# Base64功能测试脚本

echo "========================================="
echo "Base64文件编码功能测试"
echo "========================================="
echo ""

BASE_URL="http://localhost:8080"

# 测试1: 简单Base64编码
echo "测试1: 简单Base64编码"
echo "请求: GET ${BASE_URL}/api/demo/base64/encode?text=Hello World"
curl -s "${BASE_URL}/api/demo/base64/encode?text=Hello%20World" | python3 -m json.tool
echo ""
echo "---"
echo ""

# 测试2: 简单Base64解码
echo "测试2: 简单Base64解码"
ENCODED=$(curl -s "${BASE_URL}/api/demo/base64/encode?text=Hello%20World" | python3 -c "import sys, json; print(json.load(sys.stdin)['data'])")
echo "编码结果: ${ENCODED}"
echo "请求: GET ${BASE_URL}/api/demo/base64/decode?base64=${ENCODED}"
curl -s "${BASE_URL}/api/demo/base64/decode?base64=${ENCODED}" | python3 -m json.tool
echo ""
echo "---"
echo ""

# 测试3: 上传测试文件
echo "测试3: 上传测试文件"
echo "Creating test image..."
# 创建一个简单的测试文件
echo "test image content" > /tmp/test-image.txt
echo "请求: POST ${BASE_URL}/api/files/upload"
curl -s -X POST "${BASE_URL}/api/files/upload" \
  -F "file=@/tmp/test-image.txt" | python3 -m json.tool
echo ""
echo "---"
echo ""

echo "========================================="
echo "测试完成！"
echo "========================================="
echo ""
echo "注意:"
echo "1. 确保服务正在运行 (mvn spring-boot:run)"
echo "2. 完整的Base64功能测试需要使用真实的图片文件"
echo "3. 查看 Swagger UI: ${BASE_URL}/swagger-ui.html"
echo ""
