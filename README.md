# MarsReg - 高性能RAG系统

MarsReg是一个高性能的检索增强生成（RAG）系统，支持PB级海量文档的存储、检索和推理。

## 系统架构

### 核心模块
- marsreg-common: 公共模块，包含通用工具类和基础组件
- marsreg-document: 文档管理模块，负责文档的存储和管理
- marsreg-vector: 向量化模块，负责文本向量化和索引
- marsreg-search: 检索模块，提供高性能的语义搜索能力
- marsreg-inference: 推理模块，集成大语言模型进行问答
- marsreg-cache: 缓存模块，提供多级缓存加速
- marsreg-monitor: 监控模块，负责系统监控和告警

### 技术栈
- 基础框架：Spring Boot 3.2.3, Spring Cloud 2023.0.0
- 存储系统：MinIO, Elasticsearch, Redis, Milvus
- 监控系统：Prometheus, Grafana, ELK Stack

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.8+
- Docker & Docker Compose

### 构建项目
```bash
mvn clean install
```

### 运行服务
```bash
# 启动基础设施
docker-compose up -d

# 启动应用服务
java -jar marsreg-document/target/marsreg-document.jar
java -jar marsreg-vector/target/marsreg-vector.jar
java -jar marsreg-search/target/marsreg-search.jar
java -jar marsreg-inference/target/marsreg-inference.jar
```

## 开发指南

### 项目结构
```
marsreg/
├── marsreg-common/        # 公共模块
├── marsreg-document/      # 文档管理模块
├── marsreg-vector/        # 向量化模块
├── marsreg-search/        # 检索模块
├── marsreg-inference/     # 推理模块
├── marsreg-cache/         # 缓存模块
└── marsreg-monitor/       # 监控模块
```

### 开发规范
1. 遵循阿里巴巴Java开发手册
2. 使用统一的代码格式化工具
3. 编写单元测试，保持测试覆盖率
4. 使用Git Flow工作流

## 部署指南

### 系统要求
- CPU: 16核+
- 内存: 32GB+
- 存储: SSD, 1TB+
- 网络: 千兆网络

### 部署步骤
1. 准备环境
2. 配置系统参数
3. 部署基础设施
4. 部署应用服务
5. 配置监控系统

## 监控告警

### 监控指标
- 系统资源使用率
- 服务响应时间
- 错误率统计
- 业务指标监控

### 告警规则
- CPU使用率 > 80%
- 内存使用率 > 85%
- 响应时间 > 1s
- 错误率 > 1%

## 贡献指南

1. Fork 项目
2. 创建特性分支
3. 提交代码
4. 创建 Pull Request

## 许可证

Apache License 2.0 