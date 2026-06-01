# Base64功能测试脚本 (PowerShell版本)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Base64文件编码功能测试" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

$BASE_URL = "http://localhost:8080"

# 测试1: 简单Base64编码
Write-Host "测试1: 简单Base64编码" -ForegroundColor Yellow
Write-Host "请求: GET ${BASE_URL}/api/demo/base64/encode?text=Hello World"
try {
    $response = Invoke-RestMethod -Uri "${BASE_URL}/api/demo/base64/encode?text=Hello%20World" -Method Get
    $response | ConvertTo-Json -Depth 10
} catch {
    Write-Host "错误: $_" -ForegroundColor Red
}
Write-Host ""
Write-Host "---" -ForegroundColor Gray
Write-Host ""

# 测试2: 简单Base64解码
Write-Host "测试2: 简单Base64解码" -ForegroundColor Yellow
try {
    $encodedResponse = Invoke-RestMethod -Uri "${BASE_URL}/api/demo/base64/encode?text=Hello%20World" -Method Get
    $ENCODED = $encodedResponse.data
    Write-Host "编码结果: ${ENCODED}"
    Write-Host "请求: GET ${BASE_URL}/api/demo/base64/decode?base64=${ENCODED}"
    $decodedResponse = Invoke-RestMethod -Uri "${BASE_URL}/api/demo/base64/decode?base64=${ENCODED}" -Method Get
    $decodedResponse | ConvertTo-Json -Depth 10
} catch {
    Write-Host "错误: $_" -ForegroundColor Red
}
Write-Host ""
Write-Host "---" -ForegroundColor Gray
Write-Host ""

# 测试3: 创建测试文件并上传
Write-Host "测试3: 上传测试文件" -ForegroundColor Yellow
Write-Host "创建测试文件..."
$testContent = "test image content for base64"
$testFile = "$env:TEMP\test-base64.txt"
Set-Content -Path $testFile -Value $testContent -Encoding UTF8

Write-Host "请求: POST ${BASE_URL}/api/files/upload"
try {
    $form = @{
        file = Get-Item $testFile
    }
    $uploadResponse = Invoke-RestMethod -Uri "${BASE_URL}/api/files/upload" -Method Post -Form $form
    $uploadResponse | ConvertTo-Json -Depth 10
} catch {
    Write-Host "错误: $_" -ForegroundColor Red
}
Write-Host ""
Write-Host "---" -ForegroundColor Gray
Write-Host ""

# 清理测试文件
Remove-Item $testFile -ErrorAction SilentlyContinue

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "测试完成！" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "注意:" -ForegroundColor Yellow
Write-Host "1. 确保服务正在运行 (mvn spring-boot:run)"
Write-Host "2. 完整的Base64功能测试需要使用真实的图片文件"
Write-Host "3. 查看 Swagger UI: ${BASE_URL}/swagger-ui.html"
Write-Host ""
