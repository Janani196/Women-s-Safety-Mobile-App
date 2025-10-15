package com.example.my;

public class FileItem {
    private String name;
    private String url;

    public FileItem(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}
