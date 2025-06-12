# MarsReg 项目

MarsReg 是一个基于 Spring Boot 的文档管理和搜索系统。

## 技术栈

- Java 17
- Spring Boot 3.2.3
- Spring Data JPA
- Spring Data Elasticsearch
- MySQL 8.0
- Elasticsearch 8.12.1
- Redis 7.2
- Milvus 2.3.4
- Docker & Docker Compose

## 功能特性

### 文档管理 (marsreg-document)
- 文档上传与存储
- 文档元数据管理
- 文档版本控制
- 文档分类与标签
- 文档权限控制
- 文档预览与下载
- 文档批量处理
- 文档生命周期管理

### 搜索服务 (marsreg-search)
- 全文检索
- 语义搜索
- 同义词扩展
- 搜索建议
- 搜索结果高亮
- 搜索统计与分析
- 搜索性能优化
- 自定义搜索规则

### 向量存储 (marsreg-vector)
- 文档向量化
- 向量索引构建
- 相似度计算
- 向量检索
- 向量数据管理
- 向量更新与删除
- 批量向量处理
- 向量存储优化

### 缓存服务 (marsreg-cache)
- 多级缓存
- 缓存预热
- 缓存更新
- 缓存监控
- 缓存统计
- 缓存备份
- 分布式缓存
- 缓存性能优化

### 推理服务 (marsreg-inference)
- 文本分类
- 实体识别
- 关系抽取
- 文本摘要
- 情感分析
- 关键词提取
- 文本相似度
- 自定义模型

## 项目结构

```
marsreg/
├── marsreg-common/        # 公共模块
├── marsreg-document/      # 文档管理模块
├── marsreg-search/        # 搜索模块
├── marsreg-vector/        # 向量存储模块
├── marsreg-cache/         # 缓存模块
└── marsreg-inference/     # 推理模块
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.9+
- Docker & Docker Compose

### 本地开发

1. 克隆项目
```bash
git clone https://github.com/your-username/marsreg.git
cd marsreg
```

2. 使用 Maven 构建
```bash
./mvnw clean install
```

3. 运行测试
```bash
./mvnw test
```

### Docker 部署

1. 构建并启动服务
```bash
docker-compose up -d
```

2. 查看服务状态
```bash
docker-compose ps
```

3. 查看服务日志
```bash
docker-compose logs -f
```

## API 文档

启动服务后，访问以下地址查看 API 文档：

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI 规范: http://localhost:8080/v3/api-docs

## 开发指南

### 添加新功能

1. 在相应的模块中创建新的功能类
2. 编写单元测试
3. 更新 API 文档
4. 提交 Pull Request

### 代码规范

- 遵循 Google Java Style Guide
- 使用 Lombok 简化代码
- 编写完整的单元测试
- 保持代码简洁和可维护性

## 贡献指南

1. Fork 项目
2. 创建特性分支
3. 提交更改
4. 推送到分支
5. 创建 Pull Request

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件 