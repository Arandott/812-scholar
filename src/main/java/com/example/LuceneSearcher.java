package com.example;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.spell.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class LuceneSearcher {

    private IndexSearcher searcher;
    private PorterStemAnalyzer analyzer;
    private IndexReader reader;
    private Map<String, SpellChecker> spellCheckers;

    public LuceneSearcher(String indexDir) throws IOException {
        try {
            // 打开索引目录
            Directory dir = FSDirectory.open(Paths.get(indexDir));
            reader = DirectoryReader.open(dir);
            searcher = new IndexSearcher(reader);
            analyzer = new PorterStemAnalyzer();

            // 构建每个字段独立的拼写检查器
            spellCheckers = new HashMap<>();
            String[] fields = {"title", "authors", "affiliation", "address", "fulltext", "date"};

            for (String field : fields) {
                Directory spellIndexDir = FSDirectory.open(Paths.get(indexDir + "_" + field + "_spell"));
                SpellChecker spellChecker = new SpellChecker(spellIndexDir);
                spellChecker.indexDictionary(new LuceneDictionary(reader, field), new IndexWriterConfig(new StandardAnalyzer()), true);
                spellCheckers.put(field, spellChecker);
            }
        } catch (IOException e) {
            System.err.println("Error initializing LuceneSearcher: " + e.getMessage());
            throw e;
        }
    }

    public void search(String field, String queryStr) throws InvalidTokenOffsetsException {
        try {
            // 执行初次查询
            QueryParser parser = new QueryParser(field, analyzer);
            Query query = parser.parse(queryStr);
            // 创建高亮器
            SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<span style=\"background-color:yellow;\">", "</span>");
            QueryScorer scorer = new QueryScorer(query);
            Highlighter highlighter = new Highlighter(formatter, scorer);
            // 添加 Fragmenter 设置
            Fragmenter fragmenter = new SimpleFragmenter(150); // 设置片段长度为 150 个字符
            highlighter.setTextFragmenter(fragmenter);

            TopDocs results = searcher.search(query, 10);

            // 如果没有命中，执行拼写检查
            if (results.totalHits.value == 0) {
                System.out.println("No results found for: " + queryStr);

                // 使用对应字段的拼写检查器给出建议
                SpellChecker spellChecker = spellCheckers.get(field);
                if (spellChecker != null) {
                    String[] suggestions = spellChecker.suggestSimilar(queryStr, 10);
                    if (suggestions.length > 0) {
                        System.out.println("Did you mean: ");
                        for (int i = 0; i < suggestions.length; i++) {
                            System.out.println((i + 1) + ". " + suggestions[i]);
                        }
                        System.out.print("Enter the number of the suggestion to use it, or 0 to keep your original query: ");
                        Scanner scanner = new Scanner(System.in);
                        int choice = scanner.nextInt();
                        scanner.nextLine(); // Consume the newline

                        if (choice > 0 && choice <= suggestions.length) {
                            // 使用建议的查询词重新执行查询
                            queryStr = suggestions[choice - 1];
                            query = parser.parse(queryStr);
                            search(field, queryStr);
                        }
                    }
                }
            }

            // 输出最终的查询结果
            System.out.println("Total hits: " + results.totalHits.value);
            for (ScoreDoc scoreDoc : results.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);

                // 获取需要高亮的字段内容
                String text = doc.get(field);

                // 创建 TokenStream
                TokenStream tokenStream = TokenSources.getAnyTokenStream(reader, scoreDoc.doc, field, analyzer);

                // 获取高亮片段
                String highlightedText = highlighter.getBestFragment(tokenStream, text);

                // 如果高亮结果为空，使用原始文本
                if (highlightedText == null) {
                    highlightedText = text;
                }
                System.out.println("----------------------------------------");
                // 输出高亮结果
                System.out.println(field + ": " + highlightedText);
                // 输出其他字段
                System.out.println("Title: " + doc.get("title"));
                System.out.println("Authors: " + doc.get("authors"));
                System.out.println("Date: " + doc.get("date"));
                System.out.println("Affiliation: " + doc.get("affiliation"));
                System.out.println("Address: " + doc.get("address"));
                System.out.println("Filepath: " + doc.get("pdfPath"));
            }
        } catch (ParseException e) {
            System.err.println("Error parsing query: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error executing search: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            LuceneSearcher luceneSearcher = new LuceneSearcher("index");
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("请输入要搜索的字段（title, authors, date, affiliation, address, fulltext）：");
                String field = scanner.nextLine();
                System.out.print("请输入搜索关键词：");
                String queryStr = scanner.nextLine();
                luceneSearcher.search(field, queryStr);
                System.out.print("是否继续搜索？（y/n）：");
                String continueSearch = scanner.nextLine();
                if (!continueSearch.equalsIgnoreCase("y")) {
                    break;
                }
            }
            scanner.close();
        } catch (Exception e) {
            System.err.println("Error initializing LuceneSearcher: " + e.getMessage());
        }
    }
}