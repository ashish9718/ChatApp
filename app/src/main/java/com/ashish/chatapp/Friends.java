package com.ashish.chatapp;

public class Friends {
    public Friends(){}
    public Friends(String date) {
        this.date = date;
    }

    private String date;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
