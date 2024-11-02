package com.example.service;

import com.example.model.SearchResult;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {

    private IndexSearcher searcher;
    private StandardAnalyzer analyzer;

    @PostConstruct
    public void init() throws IOException {
        Directory dir = FSDirectory.open(Paths.get("index"));
        IndexReader reader = DirectoryReader.open(dir);
        searcher = new IndexSearcher(reader);
        analyzer = new StandardAnalyzer();
    }

    public List<SearchResult> search(String field, String queryStr) throws Exception {
        QueryParser parser = new QueryParser(field, analyzer);
        Query query = parser.parse(queryStr);

        TopDocs results = searcher.search(query, 100); // 设置最大返回结果数

        List<SearchResult> searchResults = new ArrayList<>();

        for (ScoreDoc scoreDoc : results.scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);
            SearchResult result = new SearchResult();
            result.setTitle(doc.get("title"));
            result.setAuthors(doc.get("authors"));
            result.setDate(doc.get("date"));
            result.setAffiliation(doc.get("affiliation"));
            result.setAddress(doc.get("address"));
            result.setPdfPath(doc.get("pdfPath"));
            searchResults.add(result);
        }

        return searchResults;
    }
}
