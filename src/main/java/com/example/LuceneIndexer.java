package com.example;

import org.apache.lucene.document.Document; // Lucene Document
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import java.io.File;
import org.w3c.dom.*;

public class LuceneIndexer {

    private IndexWriter writer;

    // 构造函数，初始化索引写入器
    public LuceneIndexer(String indexDir) throws Exception {
        // 创建索引存储目录
        Directory dir = FSDirectory.open(new File(indexDir).toPath());
        // 使用自定义的 PorterStemAnalyzer 配置索引写入器
        IndexWriterConfig config = new IndexWriterConfig(new PorterStemAnalyzer());
        // 创建索引写入器，用于向索引中写入文档
        writer = new IndexWriter(dir, config);
    }

    // 关闭索引写入器
    public void close() throws Exception {
        writer.close();
    }

    // 索引 XML 文件内容
    public void indexXML(File file) throws Exception {
        // 解析 XML 文件
        org.w3c.dom.Document xmlDocument = XMLParser.parseXML(file);
        Element root = xmlDocument.getDocumentElement();

        // 定义命名空间 URI
        String namespaceURI = "http://www.tei-c.org/ns/1.0";

        // 创建一个新的 Lucene 文档对象
        Document luceneDoc = new Document();

        // 定位到 teiHeader 节点
        NodeList teiHeaderList = root.getElementsByTagNameNS(namespaceURI, "teiHeader");
        if (teiHeaderList.getLength() > 0) {
            Element teiHeader = (Element) teiHeaderList.item(0);

            // 定位到 fileDesc 节点
            NodeList fileDescList = teiHeader.getElementsByTagNameNS(namespaceURI, "fileDesc");
            if (fileDescList.getLength() > 0) {
                Element fileDesc = (Element) fileDescList.item(0);

                // 定位到 titleStmt 节点
                NodeList titleStmtList = fileDesc.getElementsByTagNameNS(namespaceURI, "titleStmt");
                if (titleStmtList.getLength() > 0) {
                    Element titleStmt = (Element) titleStmtList.item(0);

                    // 获取标题并添加到 Lucene 文档
                    String title = XMLParser.getTextFromTag(titleStmt, namespaceURI, "title");
                    if (title != null) {
                        luceneDoc.add(new TextField("title", title, Field.Store.YES));
                    }
                }

                // 定位到 sourceDesc 节点
                NodeList sourceDescList = fileDesc.getElementsByTagNameNS(namespaceURI, "sourceDesc");
                if (sourceDescList.getLength() > 0) {
                    Element sourceDesc = (Element) sourceDescList.item(0);

                    // 定位到 biblStruct 节点
                    NodeList biblStructList = sourceDesc.getElementsByTagNameNS(namespaceURI, "biblStruct");
                    if (biblStructList.getLength() > 0) {
                        Element biblStruct = (Element) biblStructList.item(0);

                        // 定位到 analytic 节点
                        NodeList analyticList = biblStruct.getElementsByTagNameNS(namespaceURI, "analytic");
                        if (analyticList.getLength() > 0) {
                            Element analytic = (Element) analyticList.item(0);

                            // 获取作者信息并添加到 Lucene 文档
                            String authors = XMLParser.getAuthors(analytic, namespaceURI);
                            if (authors != null && !authors.isEmpty()) {
                                luceneDoc.add(new TextField("authors", authors, Field.Store.YES));
                            }

                            // 获取机构信息并添加到 Lucene 文档
                            String affiliation = getAffiliation(analytic, namespaceURI);
                            if (affiliation != null && !affiliation.isEmpty()) {
                                luceneDoc.add(new TextField("affiliation", affiliation, Field.Store.YES));
                            }

                            // 获取地址信息并添加到 Lucene 文档
                            String address = getAddress(analytic, namespaceURI);
                            if (address != null && !address.isEmpty()) {
                                luceneDoc.add(new TextField("address", address, Field.Store.YES));
                            }
                        }

                        // 定位到 monogr 节点
                        NodeList monogrList = biblStruct.getElementsByTagNameNS(namespaceURI, "monogr");
                        if (monogrList.getLength() > 0) {
                            Element monogr = (Element) monogrList.item(0);

                            // 定位到 imprint 节点
                            NodeList imprintList = monogr.getElementsByTagNameNS(namespaceURI, "imprint");
                            if (imprintList.getLength() > 0) {
                                Element imprint = (Element) imprintList.item(0);

                                // 获取日期并添加到 Lucene 文档
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

        // 获取全文内容并添加到 Lucene 文档（假设在 <text> 元素中）
        String fulltext = getFullText(root, namespaceURI);
        if (fulltext != null) {
            luceneDoc.add(new TextField("fulltext", fulltext, Field.Store.YES));
        }
        
        // 获取 PDF 文件路径并添加到 Lucene 文档
        String pdfPath = getPDFPath(file);
        if (pdfPath != null) {
            luceneDoc.add(new StringField("pdfPath", pdfPath, Field.Store.YES));
        }

        // 将文档写入索引
        writer.addDocument(luceneDoc);
    }

    // 获取机构信息
    private String getAffiliation(Element analytic, String namespaceURI) {
        StringBuilder affiliations = new StringBuilder();
        // 获取 author 节点列表
        NodeList authorList = analytic.getElementsByTagNameNS(namespaceURI, "author");
        for (int i = 0; i < authorList.getLength(); i++) {
            Element authorElement = (Element) authorList.item(i);
            // 获取 affiliation 节点列表
            NodeList affiliationList = authorElement.getElementsByTagNameNS(namespaceURI, "affiliation");
            for (int j = 0; j < affiliationList.getLength(); j++) {
                Element affiliationElement = (Element) affiliationList.item(j);
                // 获取机构名称并添加到结果字符串中
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
        // 获取 author 节点列表
        NodeList authorList = analytic.getElementsByTagNameNS(namespaceURI, "author");
        for (int i = 0; i < authorList.getLength(); i++) {
            Element authorElement = (Element) authorList.item(i);
            // 获取 affiliation 节点列表
            NodeList affiliationList = authorElement.getElementsByTagNameNS(namespaceURI, "affiliation");
            for (int j = 0; j < affiliationList.getLength(); j++) {
                Element affiliationElement = (Element) affiliationList.item(j);
                // 获取 address 节点列表
                NodeList addressList = affiliationElement.getElementsByTagNameNS(namespaceURI, "address");
                for (int k = 0; k < addressList.getLength(); k++) {
                    Element addressElement = (Element) addressList.item(k);
                    // 获取地址的各个部分并添加到结果字符串中
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
        // 获取 text 节点列表
        NodeList textList = root.getElementsByTagNameNS(namespaceURI, "text");
        if (textList.getLength() > 0) {
            Element textElement = (Element) textList.item(0);
            // 返回全文内容
            return textElement.getTextContent().trim();
        }
        return null;
    }

    // 获取 PDF 文件路径
    private String getPDFPath(File xmlFile) {
        // 获取 XML 文件的绝对路径并替换 oriXMLs 为 oriPDFs
        String xmlFilePath = xmlFile.getAbsolutePath();
        String pdfFilePath = xmlFilePath.replace("/oriXMLs/", "/oriPDFs/").replace(".xml", ".pdf");
        File pdfFile = new File(pdfFilePath);
        
        if (pdfFile.exists()) {
            return pdfFile.getAbsolutePath();
        } else {
            // 处理找不到 PDF 文件的情况
            System.err.println("对应的 PDF 文件不存在：" + pdfFilePath);
            return null;
        }
    }

    // 主函数，运行索引器
    public static void main(String[] args) throws Exception {
        // 创建 Lucene 索引器对象，指定索引目录
        LuceneIndexer indexer = new LuceneIndexer("index");
        // 获取 XML 文件目录
        File xmlDir = new File("oriXMLs");
        File[] xmlFiles = xmlDir.listFiles((dir, name) -> name.endsWith(".xml"));
        if (xmlFiles != null) {
            // 遍历所有 XML 文件并索引其内容
            for (File file : xmlFiles) {
                System.out.println("Indexing file: " + file.getName());
                indexer.indexXML(file);
            }
        } else {
            System.out.println("No XML files found in oriXMLs directory.");
        }
        // 关闭索引器
        indexer.close();
    }
}


// 自定义分析器，使用标准分词器并添加 PorterStemFilter 进行词干化处理,添加停用词过滤器
class PorterStemAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        // 使用标准分词器
        Tokenizer source = new StandardTokenizer();
        // 使用小写过滤器
        TokenStream filter = new LowerCaseFilter(source);
        // 添加停用词过滤器
        filter = new StopFilter(filter, EnglishAnalyzer.getDefaultStopSet());
        // 使用 PorterStemFilter 进行词干化
        filter = new PorterStemFilter(filter);
        return new TokenStreamComponents(source, filter);
    }
}

// package com.example;

// import org.apache.lucene.document.Document; // Lucene Document
// import org.apache.lucene.document.Field;
// import org.apache.lucene.document.StringField;
// import org.apache.lucene.document.TextField;
// import org.apache.lucene.analysis.standard.StandardAnalyzer;
// import org.apache.lucene.index.*;
// import org.apache.lucene.store.*;
// import java.io.File;
// import org.w3c.dom.*;

// public class LuceneIndexer {

//     private IndexWriter writer;

//     public LuceneIndexer(String indexDir) throws Exception {
//         // 创建索引写入器
//         Directory dir = FSDirectory.open(new File(indexDir).toPath());
//         IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
//         writer = new IndexWriter(dir, config);
//     }

//     public void close() throws Exception {
//         writer.close();
//     }

//     public void indexXML(File file) throws Exception {
//         // 解析 XML 文件
//         org.w3c.dom.Document xmlDocument = XMLParser.parseXML(file);
//         Element root = xmlDocument.getDocumentElement();

//         // 命名空间 URI
//         String namespaceURI = "http://www.tei-c.org/ns/1.0";

//         // 创建 Lucene 文档
//         Document luceneDoc = new Document();

//         // 定位到 teiHeader
//         NodeList teiHeaderList = root.getElementsByTagNameNS(namespaceURI, "teiHeader");
//         if (teiHeaderList.getLength() > 0) {
//             Element teiHeader = (Element) teiHeaderList.item(0);

//             // 定位到 fileDesc
//             NodeList fileDescList = teiHeader.getElementsByTagNameNS(namespaceURI, "fileDesc");
//             if (fileDescList.getLength() > 0) {
//                 Element fileDesc = (Element) fileDescList.item(0);

//                 // 定位到 titleStmt
//                 NodeList titleStmtList = fileDesc.getElementsByTagNameNS(namespaceURI, "titleStmt");
//                 if (titleStmtList.getLength() > 0) {
//                     Element titleStmt = (Element) titleStmtList.item(0);

//                     // 获取标题
//                     String title = XMLParser.getTextFromTag(titleStmt, namespaceURI, "title");
//                     if (title != null) {
//                         luceneDoc.add(new TextField("title", title, Field.Store.YES));
//                     }
//                 }

//                 // 定位到 sourceDesc
//                 NodeList sourceDescList = fileDesc.getElementsByTagNameNS(namespaceURI, "sourceDesc");
//                 if (sourceDescList.getLength() > 0) {
//                     Element sourceDesc = (Element) sourceDescList.item(0);

//                     // 定位到 biblStruct
//                     NodeList biblStructList = sourceDesc.getElementsByTagNameNS(namespaceURI, "biblStruct");
//                     if (biblStructList.getLength() > 0) {
//                         Element biblStruct = (Element) biblStructList.item(0);

//                         // 定位到 analytic
//                         NodeList analyticList = biblStruct.getElementsByTagNameNS(namespaceURI, "analytic");
//                         if (analyticList.getLength() > 0) {
//                             Element analytic = (Element) analyticList.item(0);

//                             // 获取作者
//                             String authors = XMLParser.getAuthors(analytic, namespaceURI);
//                             if (authors != null && !authors.isEmpty()) {
//                                 luceneDoc.add(new TextField("authors", authors, Field.Store.YES));
//                             }

//                             // 获取机构
//                             String affiliation = getAffiliation(analytic, namespaceURI);
//                             if (affiliation != null && !affiliation.isEmpty()) {
//                                 luceneDoc.add(new TextField("affiliation", affiliation, Field.Store.YES));
//                             }

//                             // 获取地址
//                             String address = getAddress(analytic, namespaceURI);
//                             if (address != null && !address.isEmpty()) {
//                                 luceneDoc.add(new TextField("address", address, Field.Store.YES));
//                             }
//                         }

//                         // 定位到 monogr
//                         NodeList monogrList = biblStruct.getElementsByTagNameNS(namespaceURI, "monogr");
//                         if (monogrList.getLength() > 0) {
//                             Element monogr = (Element) monogrList.item(0);

//                             // 定位到 imprint
//                             NodeList imprintList = monogr.getElementsByTagNameNS(namespaceURI, "imprint");
//                             if (imprintList.getLength() > 0) {
//                                 Element imprint = (Element) imprintList.item(0);

//                                 // 获取日期
//                                 String date = XMLParser.getTextFromTag(imprint, namespaceURI, "date");
//                                 if (date != null) {
//                                     luceneDoc.add(new StringField("date", date, Field.Store.YES));
//                                 }
//                             }
//                         }
//                     }
//                 }
//             }
//         }

//         // 获取全文内容（假设在 <text> 元素中）
//         String fulltext = getFullText(root, namespaceURI);
//         if (fulltext != null) {
//             luceneDoc.add(new TextField("fulltext", fulltext, Field.Store.YES));
//         }
        
//         // 获取 PDF 文件路径
//         String pdfPath = getPDFPath(file);
//         if (pdfPath != null) {
//             luceneDoc.add(new StringField("pdfPath", pdfPath, Field.Store.YES));
//         }


//         // 将文档写入索引
//         writer.addDocument(luceneDoc);
//     }

//     // 获取机构信息
//     private String getAffiliation(Element analytic, String namespaceURI) {
//         StringBuilder affiliations = new StringBuilder();
//         NodeList authorList = analytic.getElementsByTagNameNS(namespaceURI, "author");
//         for (int i = 0; i < authorList.getLength(); i++) {
//             Element authorElement = (Element) authorList.item(i);
//             NodeList affiliationList = authorElement.getElementsByTagNameNS(namespaceURI, "affiliation");
//             for (int j = 0; j < affiliationList.getLength(); j++) {
//                 Element affiliationElement = (Element) affiliationList.item(j);
//                 String orgName = XMLParser.getTextFromTag(affiliationElement, namespaceURI, "orgName");
//                 if (orgName != null) {
//                     affiliations.append(orgName).append("; ");
//                 }
//             }
//         }
//         return affiliations.toString().trim();
//     }

//     // 获取地址信息
//     private String getAddress(Element analytic, String namespaceURI) {
//         StringBuilder addresses = new StringBuilder();
//         NodeList authorList = analytic.getElementsByTagNameNS(namespaceURI, "author");
//         for (int i = 0; i < authorList.getLength(); i++) {
//             Element authorElement = (Element) authorList.item(i);
//             NodeList affiliationList = authorElement.getElementsByTagNameNS(namespaceURI, "affiliation");
//             for (int j = 0; j < affiliationList.getLength(); j++) {
//                 Element affiliationElement = (Element) affiliationList.item(j);
//                 NodeList addressList = affiliationElement.getElementsByTagNameNS(namespaceURI, "address");
//                 for (int k = 0; k < addressList.getLength(); k++) {
//                     Element addressElement = (Element) addressList.item(k);
//                     String settlement = XMLParser.getTextFromTag(addressElement, namespaceURI, "settlement");
//                     String region = XMLParser.getTextFromTag(addressElement, namespaceURI, "region");
//                     String country = XMLParser.getTextFromTag(addressElement, namespaceURI, "country");
//                     StringBuilder address = new StringBuilder();
//                     if (settlement != null) {
//                         address.append(settlement).append(", ");
//                     }
//                     if (region != null) {
//                         address.append(region).append(", ");
//                     }
//                     if (country != null) {
//                         address.append(country);
//                     }
//                     addresses.append(address.toString().trim()).append("; ");
//                 }
//             }
//         }
//         return addresses.toString().trim();
//     }


//     // 获取全文内容
//     private String getFullText(Element root, String namespaceURI) {
//         NodeList textList = root.getElementsByTagNameNS(namespaceURI, "text");
//         if (textList.getLength() > 0) {
//             Element textElement = (Element) textList.item(0);
//             return textElement.getTextContent().trim();
//         }
//         return null;
//     }


//     private String getPDFPath(File xmlFile) {
//         // 获取 XML 文件的绝对路径并替换 oriXMLs 为 oriPDFs
//         String xmlFilePath = xmlFile.getAbsolutePath();
//         String pdfFilePath = xmlFilePath.replace("/oriXMLs/", "/oriPDFs/").replace(".xml", ".pdf");
//         File pdfFile = new File(pdfFilePath);
        
//         if (pdfFile.exists()) {
//             return pdfFile.getAbsolutePath();
//         } else {
//             // 处理找不到 PDF 文件的情况
//             System.err.println("对应的 PDF 文件不存在：" + pdfFilePath);
//             return null;
//         }
//     }
    


//     public static void main(String[] args) throws Exception {
//         LuceneIndexer indexer = new LuceneIndexer("index");
//         File xmlDir = new File("oriXMLs");
//         File[] xmlFiles = xmlDir.listFiles((dir, name) -> name.endsWith(".xml"));
//         if (xmlFiles != null) {
//             for (File file : xmlFiles) {
//                 System.out.println("Indexing file: " + file.getName());
//                 indexer.indexXML(file);
//             }
//         } else {
//             System.out.println("No XML files found in oriXMLs directory.");
//         }
//         indexer.close();
//     }
// }
