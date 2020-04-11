package com.example.crepcheck;

public class Articles
{
    private String article_id;
    private String article_title;
    private String article_body;
    private String article_img_uri;
    //private String article_img_fileName;
    private String article_time;
    private String article_date;

    public Articles()
    {
        //Default Constructor
    }

    public Articles(String article_id, String article_title, String article_body, String article_img_uri, /*String article_img_fileName,*/ String article_time, String article_date) {
        this.article_id = article_id;
        this.article_title = article_title;
        this.article_body = article_body;
        this.article_img_uri = article_img_uri;
        //this.article_img_fileName = article_img_fileName;
        this.article_time = article_time;
        this.article_date = article_date;
    }

    public String getArticle_id() {
        return article_id;
    }

    public void setArticle_id(String article_id) {
        this.article_id = article_id;
    }

    public String getArticle_title() {
        return article_title;
    }

    public void setArticle_title(String article_title) {
        this.article_title = article_title;
    }

    public String getArticle_body() {
        return article_body;
    }

    public void setArticle_body(String article_body) {
        this.article_body = article_body;
    }

    public String getArticle_img_uri() {
        return article_img_uri;
    }

    public void setArticle_img_uri(String article_img_uri) {
        this.article_img_uri = article_img_uri;
    }

    /*public String getArticle_img_fileName() {
        return article_img_fileName;
    }

    public void setArticle_img_fileName(String article_img_fileName) {
        this.article_img_fileName = article_img_fileName;
    }*/

    public String getArticle_time() {
        return article_time;
    }

    public void setArticle_time(String article_time) {
        this.article_time = article_time;
    }

    public String getArticle_date() {
        return article_date;
    }

    public void setArticle_date(String article_date) {
        this.article_date = article_date;
    }
}
