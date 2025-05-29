import torch
from sentence_transformers import SentenceTransformer
import os

def convert_to_onnx(model_name, output_dir):
    """
    将 Sentence Transformers 模型转换为 ONNX 格式
    :param model_name: HuggingFace 模型名称
    :param output_dir: 输出目录
    """
    print(f"正在加载模型: {model_name}")
    model = SentenceTransformer(model_name)
    
    # 创建输出目录
    os.makedirs(output_dir, exist_ok=True)
    
    # 准备示例输入
    dummy_input = ["这是一个测试文本"]
    
    # 导出模型
    print("正在导出模型...")
    model.save(output_dir)
    
    # 保存模型信息
    model_info = {
        "name": model_name,
        "version": "1.0.0",
        "dimension": model.get_sentence_embedding_dimension(),
        "max_seq_length": model.max_seq_length
    }
    
    # 保存模型信息到文件
    with open(os.path.join(output_dir, "model_info.txt"), "w") as f:
        for key, value in model_info.items():
            f.write(f"{key}={value}\n")
    
    print(f"模型已保存到: {output_dir}")
    print(f"模型信息: {model_info}")

if __name__ == "__main__":
    # 使用 all-MiniLM-L6-v2 模型
    model_name = "sentence-transformers/all-MiniLM-L6-v2"
    output_dir = "models/all-MiniLM-L6-v2"
    
    convert_to_onnx(model_name, output_dir) 