#!/bin/bash

# 目录路径，可以根据需要修改
XML_DIR="/home/kapibala/documents_for_study/computer_science/Machine_Learning/CUFEIR24fall/oriXMLs"

# 要匹配的 XML 头部
EXPECTED_HEADER='<?xml version="1.0" encoding="UTF-8"?>'

# 查找所有以 .xml 结尾的文件
find "$XML_DIR" -type f -name "*.xml" | while read -r FILE; do
  # 读取文件的第一行
  HEADER=$(head -n 1 "$FILE")

  # 检查第一行是否与期望的头部匹配
  if [[ "$HEADER" != "$EXPECTED_HEADER" ]]; then
    echo "文件不符合要求: $FILE"
  fi
done
