package com.example.crepcheck;

public class FriendSearch {
    // Attributes
    private String profile_pic, username, user_bio;

    public FriendSearch()
    {
        //Default Constructor
    }

    public FriendSearch(String username, String profile_pic, String user_bio) {
        this.profile_pic = profile_pic;
        this.username = username;
        this.user_bio = user_bio;
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

    public String getUser_bio() {
        return user_bio;
    }

    public void setUser_bio(String user_bio) {
        this.user_bio = user_bio;
    }
}
