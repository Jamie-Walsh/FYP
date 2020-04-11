package com.example.crepcheck;


public class Posts
{
    private String UID;
    private String post_id;
    private String profile_pic;
    private String username;
    private String post_time;
    private String post_date;
    private String post_img_uri;
    private String post_img_fileName;
    private String post_caption;

    public Posts()
    {
        //Default Constructor
    }

    public Posts(String UID, String post_id, String username, String profile_pic, String post_caption, String post_date, String post_img_uri, String post_img_fileName, String post_time) {
        this.UID = UID;
        this.post_id = post_id;
        this.profile_pic = profile_pic;
        this.username = username;
        this.post_time = post_time;
        this.post_date = post_date;
        this.post_img_uri = post_img_uri;
        this.post_img_fileName = post_img_fileName;
        this.post_caption = post_caption;
    }

    public String getUid() {
        return UID;
    }

    public void setUid(String UID) {
        this.UID = UID;
    }

    public String getPost_id() {
        return post_id;
    }

    public void setPost_id(String post_id) {
        this.post_id = post_id;
    }

    public String getProfile_pic() {
        return profile_pic;
    }

    public void setProfile_pic(String profile_pic) {
        this.profile_pic = profile_pic;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPost_time() {
        return post_time;
    }

    public void setPost_time(String post_time) {
        this.post_time = post_time;
    }

    public String getPost_date() {
        return post_date;
    }

    public void setPost_date(String post_date) {
        this.post_date = post_date;
    }

    public String getPost_img_uri() {
        return post_img_uri;
    }

    public void setPost_img_uri(String post_img_uri) {
        this.post_img_uri = post_img_uri;
    }

    public String getPost_img_fileName() {
        return post_img_fileName;
    }

    public void setPost_img_fileName(String post_img_fileName) {
        this.post_img_fileName = post_img_fileName;
    }

    public String getPost_caption() {
        return post_caption;
    }

    public void setPost_caption(String post_caption) {
        this.post_caption = post_caption;
    }
}