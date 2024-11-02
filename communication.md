
- 使用 n-gram 分析器处理拼写错误。
- 结合 Porter 词干提取算法处理词形变化。
- 实现“猜你想搜 XXX”功能，提供拼写建议。

---

## **项目目录结构**

```
project-root
├── pom.xml
└── src
    └── main
        ├── java
        │   └── com
        │       └── example
        │           ├── LuceneIndexer.java
        │           ├── LuceneSearcher.java
        │           ├── SpellCheckerIndexer.java
        │           └── XMLParser.java
        └── resources
            └── application.properties
```

---

## **1. `pom.xml`**

项目的 Maven 配置文件，包含所需的依赖项。

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" 
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>lucene-full-project</artifactId>
  <version>1.0-SNAPSHOT</version>

  <properties>
    <java.version>11</java.version>
    <lucene.version>9.12.0</lucene.version>
  </properties>

  <dependencies>
    <!-- Lucene Core -->
    <dependency>
      <groupId>org.apache.lucene</groupId>
      <artifactId>lucene-core</artifactId>
      <version>${lucene.version}</version>
    </dependency>
    <!-- Lucene Analyzers Common -->
    <dependency>
      <groupId>org.apache.lucene</groupId>
      <artifactId>lucene-analyzers-common</artifactId>
      <version>${lucene.version}</version>
    </dependency>
    <!-- Lucene Query Parser -->
    <dependency>
      <groupId>org.apache.lucene</groupId>
      <artifactId>lucene-queryparser</artifactId>
      <version>${lucene.version}</version>
    </dependency>
    <!-- Lucene SpellChecker -->
    <dependency>
      <groupId>org.apache.lucene</groupId>
      <artifactId>lucene-spellchecker</artifactId>
      <version>${lucene.version}</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <!-- Maven Compiler Plugin -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.8.1</version>
        <configuration>
          <source>${java.version}</source>
          <target>${java.version}</target>
        </configuration>
      </plugin>
      <!-- Maven Exec Plugin -->
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <version>3.0.0</version>
      </plugin>
    </plugins>
  </build>
</project>
```

---

## **2. `application.properties`**

如果您有特定的配置，可以在此文件中添加。对于当前项目，该文件可以为空或包含默认配置。

```properties
# Application properties (if any)
```

---

## **3. `LuceneIndexer.java`**

索引器，使用自定义分析器结合 n-gram 和 Porter 词干提取。

```java
package com.example;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import java.io.File;
import java.io.IOException;
import org.w3c.dom.*;

public class LuceneIndexer {

    private IndexWriter writer;

    public LuceneIndexer(String indexDir) throws IOException {
        // 创建索引写入器
        Directory dir = FSDirectory.open(new File(indexDir).toPath());
        Analyzer analyzer = new CustomAnalyzer(); // 使用自定义的分析器
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE); // 每次重新创建索引
        writer = new IndexWriter(dir, config);
    }

    public void close() throws IOException {
        writer.close();
    }

    public void indexXML(File file) throws Exception {
        // 解析 XML 文件
        Document xmlDocument = XMLParser.parseXML(file);
        Element root = xmlDocument.getDocumentElement();

        // 命名空间 URI
        String namespaceURI = "http://www.tei-c.org/ns/1.0";

        // 创建 Lucene 文档
        org.apache.lucene.document.Document luceneDoc = new org.apache.lucene.document.Document();

        // 定位到 teiHeader
        NodeList teiHeaderList = root.getElementsByTagNameNS(namespaceURI, "teiHeader");
        if (teiHeaderList.getLength() > 0) {
            Element teiHeader = (Element) teiHeaderList.item(0);

            // 定位到 fileDesc
            NodeList fileDescList = teiHeader.getElementsByTagNameNS(namespaceURI, "fileDesc");
            if (fileDescList.getLength() > 0) {
                Element fileDesc = (Element) fileDescList.item(0);

                // 定位到 titleStmt
                NodeList titleStmtList = fileDesc.getElementsByTagNameNS(namespaceURI, "titleStmt");
                if (titleStmtList.getLength() > 0) {
                    Element titleStmt = (Element) titleStmtList.item(0);

                    // 获取标题
                    String title = XMLParser.getTextFromTag(titleStmt, namespaceURI, "title");
                    if (title != null) {
                        luceneDoc.add(new TextField("title", title, Field.Store.YES));
                    }
                }

                // 定位到 sourceDesc
                NodeList sourceDescList = fileDesc.getElementsByTagNameNS(namespaceURI, "sourceDesc");
                if (sourceDescList.getLength() > 0) {
                    Element sourceDesc = (Element) sourceDescList.item(0);

                    // 定位到 biblStruct
                    NodeList biblStructList = sourceDesc.getElementsByTagNameNS(namespaceURI, "biblStruct");
                    if (biblStructList.getLength() > 0) {
                        Element biblStruct = (Element) biblStructList.item(0);

                        // 定位到 analytic
                        NodeList analyticList = biblStruct.getElementsByTagNameNS(namespaceURI, "analytic");
                        if (analyticList.getLength() > 0) {
                            Element analytic = (Element) analyticList.item(0);

                            // 获取作者
                            String authors = XMLParser.getAuthors(analytic, namespaceURI);
                            if (authors != null && !authors.isEmpty()) {
                                luceneDoc.add(new TextField("authors", authors, Field.Store.YES));
                            }

                            // 获取机构
                            String affiliation = getAffiliation(analytic, namespaceURI);
                            if (affiliation != null && !affiliation.isEmpty()) {
                                luceneDoc.add(new TextField("affiliation", affiliation, Field.Store.YES));
                            }

                            // 获取地址
                            String address = getAddress(analytic, namespaceURI);
                            if (address != null && !address.isEmpty()) {
                                luceneDoc.add(new TextField("address", address, Field.Store.YES));
                            }
                        }

                        // 定位到 monogr
                        NodeList monogrList = biblStruct.getElementsByTagNameNS(namespaceURI, "monogr");
                        if (monogrList.getLength() > 0) {
                            Element monogr = (Element) monogrList.item(0);

                            // 定位到 imprint
                            NodeList imprintList = monogr.getElementsByTagNameNS(namespaceURI, "imprint");
                            if (imprintList.getLength() > 0) {
                                Element imprint = (Element) imprintList.item(0);

                                // 获取日期
                                String date = XMLParser.getTextFromTag(imprint, namespaceURI, "date");
                                if (date != null) {
                                    luceneDoc.add(new StringField("date", date, Field.Store.YES));
                                }
                            }
                        }
                    }
                }
            }
        }

        // 获取全文内容（假设在 <text> 元素中）
        String fulltext = getFullText(root, namespaceURI);
        if (fulltext != null) {
            luceneDoc.add(new TextField("fulltext", fulltext, Field.Store.YES));
        }

        // 获取 PDF 文件的位置
        String pdfPath = getPDFPath(file);
        if (pdfPath != null) {
            luceneDoc.add(new StoredField("pdfPath", pdfPath));
        }

        // 将文档写入索引
        writer.addDocument(luceneDoc);
    }

    // 获取机构信息
    private String getAffiliation(Element analytic, String namespaceURI) {
        StringBuilder affiliations = new StringBuilder();
        NodeList authorList = analytic.getElementsByTagNameNS(namespaceURI, "author");
        for (int i = 0; i < authorList.getLength(); i++) {
            Element authorElement = (Element) authorList.item(i);
            NodeList affiliationList = authorElement.getElementsByTagNameNS(namespaceURI, "affiliation");
            for (int j = 0; j < affiliationList.getLength(); j++) {
                Element affiliationElement = (Element) affiliationList.item(j);
                String orgName = XMLParser.getTextFromTag(affiliationElement, namespaceURI, "orgName");
                if (orgName != null) {
                    affiliations.append(orgName).append("; ");
                }
            }
        }
        return affiliations.toString().trim();
    }

    // 获取地址信息
    private String getAddress(Element analytic, String namespaceURI) {
        StringBuilder addresses = new StringBuilder();
        NodeList authorList = analytic.getElementsByTagNameNS(namespaceURI, "author");
        for (int i = 0; i < authorList.getLength(); i++) {
            Element authorElement = (Element) authorList.item(i);
            NodeList affiliationList = authorElement.getElementsByTagNameNS(namespaceURI, "affiliation");
            for (int j = 0; j < affiliationList.getLength(); j++) {
                Element affiliationElement = (Element) affiliationList.item(j);
                NodeList addressList = affiliationElement.getElementsByTagNameNS(namespaceURI, "address");
                for (int k = 0; k < addressList.getLength(); k++) {
                    Element addressElement = (Element) addressList.item(k);
                    String settlement = XMLParser.getTextFromTag(addressElement, namespaceURI, "settlement");
                    String region = XMLParser.getTextFromTag(addressElement, namespaceURI, "region");
                    String country = XMLParser.getTextFromTag(addressElement, namespaceURI, "country");
                    StringBuilder address = new StringBuilder();
                    if (settlement != null) {
                        address.append(settlement).append(", ");
                    }
                    if (region != null) {
                        address.append(region).append(", ");
                    }
                    if (country != null) {
                        address.append(country);
                    }
                    addresses.append(address.toString().trim()).append("; ");
                }
            }
        }
        return addresses.toString().trim();
    }

    // 获取全文内容
    private String getFullText(Element root, String namespaceURI) {
        NodeList textList = root.getElementsByTagNameNS(namespaceURI, "text");
        if (textList.getLength() > 0) {
            Element textElement = (Element) textList.item(0);
            return textElement.getTextContent().trim();
        }
        return null;
    }

    // 获取 PDF 文件的位置
    private String getPDFPath(File xmlFile) {
        // 假设 PDF 文件名与 XML 文件名相同，但扩展名为 .pdf
        String pdfFileName = xmlFile.getName().replace(".xml", ".pdf");
        return "/pdfs/" + pdfFileName; // 返回相对于服务器的路径
    }

    // 自定义分析器，结合 Porter Stemming 和 n-gram
    public static class CustomAnalyzer extends Analyzer {
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
            // 使用标准分词器
            StandardTokenizer source = new StandardTokenizer();
            // 转换为小写
            TokenFilter filter = new LowerCaseFilter(source);
            // 应用 Porter Stemmer
            filter = new PorterStemFilter(filter);
            // 应用 n-gram 过滤器
            int minGram = 2;
            int maxGram = 3;
            filter = new NGramTokenFilter(filter, minGram, maxGram);
            return new TokenStreamComponents(source, filter);
        }
    }

    public static void main(String[] args) throws Exception {
        LuceneIndexer indexer = new LuceneIndexer("index");
        File xmlDir = new File("oriXMLs");
        File[] xmlFiles = xmlDir.listFiles((dir, name) -> name.endsWith(".xml"));
        if (xmlFiles != null) {
            for (File file : xmlFiles) {
                System.out.println("Indexing file: " + file.getName());
                indexer.indexXML(file);
            }
        } else {
            System.out.println("No XML files found in oriXMLs directory.");
        }
        indexer.close();
    }
}
```

---

## **4. `LuceneSearcher.java`**

搜索器，使用自定义分析器，并实现拼写建议功能。

```java
package com.example;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.spell.*;
import org.apache.lucene.store.*;
import java.nio.file.Paths;
import java.util.Scanner;
import java.io.IOException;

public class LuceneSearcher {

    private IndexSearcher searcher;
    private Analyzer analyzer;

    public LuceneSearcher(String indexDir) throws Exception {
        Directory dir = FSDirectory.open(Paths.get(indexDir));
        IndexReader reader = DirectoryReader.open(dir);
        searcher = new IndexSearcher(reader);
        analyzer = new LuceneIndexer.CustomAnalyzer(); // 使用自定义的分析器
    }

    public void search(String field, String queryStr) throws Exception {
        QueryParser parser = new QueryParser(field, analyzer);
        Query query = parser.parse(QueryParser.escape(queryStr));

        TopDocs results = searcher.search(query, 10);
        ScoreDoc[] hits = results.scoreDocs;

        if (results.totalHits.value == 0) {
            // 如果没有找到结果，提供拼写建议
            String[] suggestions = getSpellSuggestions(field, queryStr);
            if (suggestions != null && suggestions.length > 0) {
                System.out.println("您要找的是不是：");
                for (String suggestion : suggestions) {
                    System.out.println(" - " + suggestion);
                }
            } else {
                System.out.println("没有找到相关结果，也没有拼写建议。");
            }
        } else {
            System.out.println("Found " + results.totalHits.value + " hits.");
            for (ScoreDoc scoreDoc : hits) {
                Document doc = searcher.doc(scoreDoc.doc);
                String title = doc.get("title");
                String authors = doc.get("authors");
                String date = doc.get("date");
                String affiliation = doc.get("affiliation");
                String address = doc.get("address");
                String pdfPath = doc.get("pdfPath");

                System.out.println("----------------------------------------");
                System.out.println("Title: " + title);
                System.out.println("Authors: " + authors);
                System.out.println("Date: " + date);
                System.out.println("Affiliation: " + affiliation);
                System.out.println("Address: " + address);
                System.out.println("PDF Path: " + pdfPath);
            }
        }
    }

    private String[] getSpellSuggestions(String field, String queryStr) throws IOException {
        // 打开拼写索引目录
        Directory spellIndexDirectory = FSDirectory.open(Paths.get("spellIndex"));
        SpellChecker spellChecker = new SpellChecker(spellIndexDirectory);

        // 设置建议的最大数量和精度
        int suggestionsNumber = 5;
        float accuracy = 0.7f;
        spellChecker.setAccuracy(accuracy);

        // 获取拼写建议
        String[] suggestions = spellChecker.suggestSimilar(queryStr, suggestionsNumber);

        spellChecker.close();
        return suggestions;
    }

    public static void main(String[] args) throws Exception {
        LuceneSearcher searcher = new LuceneSearcher("index");
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter search field (title/authors/date/affiliation/address/fulltext): ");
        String field = scanner.nextLine();

        System.out.println("Enter search query: ");
        String queryStr = scanner.nextLine();

        searcher.search(field, queryStr);
    }
}
```

---

## **5. `SpellCheckerIndexer.java`**

构建拼写索引，用于提供拼写建议。

```java
package com.example;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.search.spell.*;
import org.apache.lucene.store.*;
import java.io.IOException;
import java.nio.file.Paths;

public class SpellCheckerIndexer {

    public static void buildSpellIndex(String indexDir, String spellIndexDir) throws IOException {
        // 打开索引目录
        Directory indexDirectory = FSDirectory.open(Paths.get(indexDir));
        // 打开拼写索引目录
        Directory spellIndexDirectory = FSDirectory.open(Paths.get(spellIndexDir));

        // 创建 SpellChecker 对象
        SpellChecker spellChecker = new SpellChecker(spellIndexDirectory);

        // 使用索引中的字段构建拼写索引，例如 "title"
        IndexReader reader = DirectoryReader.open(indexDirectory);
        // 可以指定多个字段
        String[] fields = { "title", "authors", "affiliation", "address", "fulltext" };

        // 创建分析器，与索引时使用的分析器一致
        Analyzer analyzer = new LuceneIndexer.CustomAnalyzer();

        // 构建拼写索引
        spellChecker.indexDictionary(new LuceneDictionary(reader, fields), new IndexWriterConfig(analyzer), true);

        // 关闭资源
        spellChecker.close();
        reader.close();
    }

    public static void main(String[] args) throws IOException {
        String indexDir = "index";
        String spellIndexDir = "spellIndex";
        buildSpellIndex(indexDir, spellIndexDir);
    }
}
```

---

## **6. `XMLParser.java`**

用于解析 XML 文件的辅助类。

```java
package com.example;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

public class XMLParser {

    public static Document parseXML(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // 处理命名空间
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    public static String getTextFromTag(Element element, String namespaceURI, String tag) {
        NodeList childNodes = element.getElementsByTagNameNS(namespaceURI, tag);
        for (int i = 0; i < childNodes.getLength(); i++) {
            Element childElement = (Element) childNodes.item(i);
            if (childElement.getParentNode().isSameNode(element)) {
                return childElement.getTextContent().trim();
            }
        }
        return null;
    }

    public static String getAuthors(Element analytic, String namespaceURI) {
        StringBuilder authors = new StringBuilder();
        NodeList authorList = analytic.getElementsByTagNameNS(namespaceURI, "author");
        for (int i = 0; i < authorList.getLength(); i++) {
            Element authorElement = (Element) authorList.item(i);
            NodeList persNameList = authorElement.getElementsByTagNameNS(namespaceURI, "persName");
            if (persNameList.getLength() > 0) {
                Element persName = (Element) persNameList.item(0);
                String forename = getTextFromTag(persName, namespaceURI, "forename");
                String surname = getTextFromTag(persName, namespaceURI, "surname");
                if (forename != null && surname != null) {
                    authors.append(forename).append(" ").append(surname).append("; ");
                } else if (surname != null) {
                    authors.append(surname).append("; ");
                }
            }
        }
        return authors.toString().trim();
    }
}
```

---

## **7. 运行和测试**

### **步骤 1：建立索引**

1. **编译项目**

   ```bash
   mvn clean compile
   ```
2. **运行索引器**

   ```bash
   mvn exec:java -Dexec.mainClass="com.example.LuceneIndexer"
   ```

   确保 `oriXMLs` 目录中包含所有需要索引的 XML 文件。

### **步骤 2：构建拼写索引**

1. **运行拼写索引器**

   ```bash
   mvn exec:java -Dexec.mainClass="com.example.SpellCheckerIndexer"
   ```

   这将使用已建立的索引，构建拼写建议所需的拼写索引。

### **步骤 3：运行搜索器**

1. **运行搜索器**

   ```bash
   mvn exec:java -Dexec.mainClass="com.example.LuceneSearcher"
   ```
2. **输入搜索字段和查询**

   示例：

   ```
   Enter search field (title/authors/date/affiliation/address/fulltext): title
   Enter search query: conputer learning
   ```
3. **查看搜索结果和拼写建议**

   如果没有找到结果，程序将提供拼写建议，例如：

   ```
   您要找的是不是：
    - computer learning
    - computer leaning
    - computer leaving
   ```

---

## **注意事项**

- **目录结构**

  - 确保项目的目录结构与上述结构一致。
  - `oriXMLs` 目录应位于项目根目录，包含待索引的 XML 文件。
  - `pdfs` 目录应位于 `src/main/resources/static/`（如果使用 Spring Boot），用于存放 PDF 文件。
- **依赖版本**

  - 确保 Maven 依赖的版本与您的环境兼容。
- **Java 版本**

  - 项目使用 Java 11，如有需要，可在 `pom.xml` 中修改 `<java.version>`。
- **分析器一致性**

  - 在索引和搜索过程中，使用相同的自定义分析器 `CustomAnalyzer`。
- **拼写索引**

  - 在索引数据更新后，需要重新构建拼写索引。
