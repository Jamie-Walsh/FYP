package com.example.crepcheck;

public class Comments {
    // Attributes
    private String comment_id;
    private String comment;
    private String UID;
    private String username;
    private String comment_time;
    private String comment_date;

    public Comments()
    {
        //Default Constructor
    }

    public Comments(String comment_id, String comment, String UID, String username, String comment_time, String comment_date) {
        this.comment_id = comment_id;
        this.comment = comment;
        this.UID = UID;
        this.username = username;
        this.comment_time = comment_time;
        this.comment_date = comment_date;
    }

    public String getComment_id() {
        return comment_id;
    }

    public void setComment_id(String comment_id) {
        this.comment_id = comment_id;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getUID() {
        return UID;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getComment_time() {
        return comment_time;
    }

    public void setComment_time(String comment_time) {
        this.comment_time = comment_time;
    }

    public String getComment_date() {
        return comment_date;
    }

    public void setComment_date(String comment_date) {
        this.comment_date = comment_date;
    }
}

