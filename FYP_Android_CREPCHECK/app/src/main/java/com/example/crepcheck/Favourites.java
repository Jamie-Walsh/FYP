package com.example.crepcheck;

public class Favourites
{
    private String UID;
    private String favourite_id;
    private String shoe_image_url;
    private String shoe_name;
    private String shoe_price;
    private String shoe_image_name;
    private String shoe_img_fileName;

    // Constructor
    public Favourites()
    {

    }

    public Favourites(String UID, String favourite_id, String shoe_image_url, String shoe_img_fileName, String shoe_name, String shoe_price, String shoe_image_name)
    {
        this.UID = UID;
        this.favourite_id = favourite_id;
        this.shoe_image_url = shoe_image_url;
        this.shoe_name = shoe_name;
        this.shoe_price = shoe_price;
        this.shoe_image_name = shoe_image_name;
        this.shoe_img_fileName = shoe_img_fileName;
    }

    public String getUid() {
        return UID;
    }

    public void setUid(String UID) {
        this.UID = UID;
    }

    public String getFavourite_id() {
        return favourite_id;
    }

    public void setFavourite_id(String favourite_id) {
        this.favourite_id = favourite_id;
    }

    public String getShoe_image_url() {
        return shoe_image_url;
    }

    public void setShoe_image_url(String shoe_image_url) {
        this.shoe_image_url = shoe_image_url;
    }

    public String getShoe_name() {
        return shoe_name;
    }

    public void setShoe_name(String shoe_name) {
        this.shoe_name = shoe_name;
    }

    public String getShoe_price() {
        return shoe_price;
    }

    public void setShoe_price(String shoe_price) {
        this.shoe_price = shoe_price;
    }

    public String getShoe_image_name() {
        return shoe_image_name;
    }

    public void setShoe_image_name(String shoe_image_name) {
        this.shoe_image_name = shoe_image_name;
    }

    public String getShoe_img_fileName() {
        return shoe_img_fileName;
    }

    public void setShoe_img_fileName(String shoe_img_fileName) {
        this.shoe_img_fileName = shoe_img_fileName;
    }
}
