# 模型转换工具

这个目录包含了将 Sentence Transformers 模型转换为 ONNX 格式的工具。

## 环境要求

- Python 3.8+
- PyTorch 2.0+
- sentence-transformers 2.2.2+
- transformers 4.30.0+

## 安装依赖

```bash
pip install -r requirements.txt
```

## 使用方法

1. 运行转换脚本：

```bash
python convert_model.py
```

2. 脚本会自动：
   - 下载 all-MiniLM-L6-v2 模型
   - 转换为 ONNX 格式
   - 保存到 models/all-MiniLM-L6-v2 目录
   - 生成模型信息文件

3. 转换完成后，将 models 目录复制到 Java 项目的资源目录中

## 模型信息

- 模型名称：sentence-transformers/all-MiniLM-L6-v2
- 向量维度：384
- 最大序列长度：128
- 支持语言：多语言（包括中文）

## 注意事项

1. 确保有足够的磁盘空间（约 500MB）
2. 首次运行时会下载模型，需要稳定的网络连接
3. 转换过程可能需要几分钟时间 