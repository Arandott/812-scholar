package com.example.service;

import com.example.model.SearchResult;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.search.spell.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Service;

// import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class SearchService {

    private IndexSearcher searcher;
    private Analyzer analyzer;
    private IndexReader reader;
    private Map<String, SpellChecker> spellCheckers;

    public SearchService() throws IOException {
        try {
            String indexDir = "index";
            Directory dir = FSDirectory.open(Paths.get(indexDir));
            reader = DirectoryReader.open(dir);
            searcher = new IndexSearcher(reader);
            analyzer = new PorterStemAnalyzer();

            // 构建每个字段独立的拼写检查器
            spellCheckers = new HashMap<>();
            String[] fields = {"title", "authors", "affiliation", "address", "fulltext", "date"};

            for (String field : fields) {
                Path spellIndexPath = Paths.get(indexDir + "_" + field + "_spell");
                Directory spellIndexDir = FSDirectory.open(Paths.get(indexDir + "_" + field + "_spell"));
                SpellChecker spellChecker = new SpellChecker(spellIndexDir);
                // spellChecker.indexDictionary(new LuceneDictionary(reader, field), new IndexWriterConfig(new StandardAnalyzer()), true);
                if (!Files.exists(spellIndexPath) || !DirectoryReader.indexExists(spellIndexDir)) {
                    spellChecker.indexDictionary(new LuceneDictionary(reader, field), new IndexWriterConfig(new StandardAnalyzer()), true);
                }
                spellCheckers.put(field, spellChecker);
            }
        } catch (IOException e) {
            System.err.println("Error initializing SearchService: " + e.getMessage());
            throw e;
        }
    }

    public List<SearchResult> search(String field, String queryStr) {
        List<SearchResult> resultsList = new ArrayList<>();
        try {
            QueryParser parser = new QueryParser(field, analyzer);
            Query query = parser.parse(queryStr);

            SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<span style=\"background-color:yellow;\">", "</span>");
            QueryScorer scorer = new QueryScorer(query);
            Highlighter highlighter = new Highlighter(formatter, scorer);
            Fragmenter fragmenter = new SimpleFragmenter(150);
            highlighter.setTextFragmenter(fragmenter);

            TopDocs results = searcher.search(query, 10);

            if (results.totalHits.value == 0) {
                SpellChecker spellChecker = spellCheckers.get(field);
                if (spellChecker != null) {
                    String[] suggestions = spellChecker.suggestSimilar(queryStr, 5);
                    if (suggestions.length > 0) {
                        for (String suggestion : suggestions) {
                            SearchResult suggestionResult = new SearchResult();
                            suggestionResult.setSuggestion(suggestion);
                            resultsList.add(suggestionResult);
                        }
                    }
                }
            } else {
                for (ScoreDoc scoreDoc : results.scoreDocs) {
                    Document doc = searcher.doc(scoreDoc.doc);
                    SearchResult searchResult = new SearchResult();

                    String text = doc.get(field);
                    TokenStream tokenStream = TokenSources.getAnyTokenStream(reader, scoreDoc.doc, field, analyzer);
                    String highlightedText = highlighter.getBestFragment(tokenStream, text);
                    if (highlightedText == null) {
                        highlightedText = text;
                    }

                    searchResult.setHighlightedField(highlightedText);
                    searchResult.setTitle(doc.get("title"));
                    searchResult.setAuthors(doc.get("authors"));
                    searchResult.setDate(doc.get("date"));
                    searchResult.setAffiliation(doc.get("affiliation"));
                    searchResult.setAddress(doc.get("address"));
                    searchResult.setPdfPath(doc.get("pdfPath"));
                    // searchResult.setFulltext(doc.get("fulltext"));

                    resultsList.add(searchResult);
                }
            }
        } catch (ParseException e) {
            System.err.println("Error parsing query: " + e.getMessage());
        } catch (IOException | InvalidTokenOffsetsException e) {
            System.err.println("Error executing search: " + e.getMessage());
        }
        return resultsList;
    }
}


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