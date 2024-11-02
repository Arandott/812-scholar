#!/bin/bash
# 确保目标文件夹存在
mkdir -p ~/documents_for_study/computer_science/Machine_Learning/CUFEIR24fall/oriXMLs

# 获取总的 PDF 文件数量
total_files=$(ls /home/kapibala/documents_for_study/computer_science/Machine_Learning/CUFEIR24fall/oriPDFs/*.pdf | wc -l)
current_file=0

# 遍历所有 PDF 文件并解析它们，保存 XML 文件到指定文件夹
for file in /home/kapibala/documents_for_study/computer_science/Machine_Learning/CUFEIR24fall/oriPDFs/*.pdf; do
    ((current_file++))
    echo "Processing file $current_file of $total_files: $file"
    
    # 将解析结果保存到目标文件夹中
    curl -F input=@$file http://localhost:8070/api/processFulltextDocument > ~/documents_for_study/computer_science/Machine_Learning/CUFEIR24fall/oriXMLs/$(basename ${file%.pdf}.xml)
    
    # 简单的进度显示
    echo "Completed $current_file of $total_files"
done

echo "All files processed!"
