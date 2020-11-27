package com.ashish.chatapp;

public class Users {
    public String name;
    public String image;
    public String status;
    public String thumb_img;



    public Users(){

    }

    public Users(String name, String image, String status,String thumb_img) {
        this.name = name;
        this.image = image;
        this.status = status;
        this.thumb_img=thumb_img;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getThumb_img() {
        return thumb_img;
    }

    public void setThumb_img(String thumb_img) {
        this.thumb_img = thumb_img;
    }
}
