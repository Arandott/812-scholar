package com.example.controller;

import com.example.model.SearchResult;
import com.example.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SearchApiController {

    @Autowired
    private SearchService searchService;

    @GetMapping("/search")
    public List<SearchResult> search(@RequestParam("field") String field,
                                     @RequestParam("query") String queryStr) throws Exception {
        return searchService.search(field, queryStr);
    }
}
