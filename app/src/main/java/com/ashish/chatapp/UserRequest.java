package com.ashish.chatapp;

public class UserRequest {
    private String requset_type;

    UserRequest(){

    }
    public UserRequest(String requset_type) {
        this.requset_type = requset_type;
    }

    public String getRequset_type() {
        return requset_type;
    }

    public void setRequset_type(String requset_type) {
        this.requset_type = requset_type;
    }
}
