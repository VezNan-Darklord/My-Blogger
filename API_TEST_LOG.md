# 项目API测试日志
## 测试简介
该项目采用APIFox作为接口测试工具，开发环境测试接口为http://localhost:8080。
测试所使用的API文档来自http://localhost:8080/v3/api-docs文档的动态自动生成，无需手动调试。
项目目录下的openapi.yaml文件为前后端契约文档，最新生成api-docs文档后手动更新，用于前后端接口定义对齐。
## 测试进展
- 7月23日：搭建APIFox测试工具链，导入文档，并实施了register和login接口测试。
- 8月15日：补充了APIFox中的Bearer鉴权功能。
- 8月15日：新增并测试通过了用户目录下所有/me接口。
- 8月17日：测试通过了分类（Category）有关接口。