# 812 Scholar

这是一个信息检索系统课程的大作业，课程链接：[Information Retrieval CUFE](https://pewter-radium-c43.notion.site/Information-Retrieval-CUFE-1ac421a414404044b1219f3262beb5d5)。本项目的功能是根据用户输入，从1000篇PDF文档中检索到相关的文档。

## 技术栈

- Java
- Spring Boot
- Grobid
- Maven

## 安装指南

1. **克隆项目**

   ```bash
   git clone https://github.com/Arandott/812-scholar
   cd 812-scholar
   ```

2. **创建项目**

   确保已安装 Maven 和 JDK。
 
   ```bash
   mvn clean install
   ```
3. **预处理PDF文档**

   使用grobid将论文pdf转化成xml文件格式

   ```bash
   ./process_pdfs.sh

4. **建立索引**

   在第一次运行前，需要对文档集合进行索引：

   ```bash
   java -cp target/你的项目.jar com.example.LuceneIndexer /path/to/documents
   ```

   请将 /path/to/documents 替换为实际的文档目录。

5. **启动应用**

   ```bash
   mvn spring-boot:run
   ```

   应用将运行在 http://localhost:8083/。
 

## 贡献者
- 陈宝文
- 张弛
- 宋明坤
