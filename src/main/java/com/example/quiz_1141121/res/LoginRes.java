package com.example.quiz_1141121.res;

import com.example.quiz_1141121.entity.User;

public class LoginRes extends BasicRes {

    private User user;

    private String token; // 新增：登入成功後回傳給前端的 JWT token

    public LoginRes(int code, String message, User user) {
        super(code, message);
        this.user = user;
    }

    // 新增：登入成功且帶 token 的建構子
    public LoginRes(int code, String message, User user, String token) {
        super(code, message);
        this.user = user;
        this.token = token;
    }

    public LoginRes(int code, String message) {
        super(code, message);
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
