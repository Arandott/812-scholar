package com.example.model;

public class SearchResult {
    private String title;
    private String authors;
    private String date;
    private String affiliation;
    private String address;
    private String pdfPath;

    // Getters and setters

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
 
    public String getAffiliation() {
        return affiliation;
    }
 
    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }
 
    public String getAddress() {
        return address;
    }
 
    public void setAddress(String address) {
        this.address = address;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }
}
