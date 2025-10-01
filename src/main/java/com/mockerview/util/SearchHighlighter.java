package com.mockerview.util;

import org.springframework.stereotype.Component;

@Component("searchHighlighter")
public class SearchHighlighter {
    
    public String highlight(String text, String keyword) {
        if (text == null || keyword == null || keyword.trim().isEmpty()) {
            return text;
        }
        
        String escapedKeyword = keyword.replaceAll("([\\[\\](){}*+?.\\\\^$|])", "\\\\$1");
        
        return text.replaceAll(
            "(?i)(" + escapedKeyword + ")",
            "<span class=\"highlight\">$1</span>"
        );
    }
}