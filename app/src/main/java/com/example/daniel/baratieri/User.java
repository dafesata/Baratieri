package com.example.daniel.baratieri;

/**
 * Created by Daniel on 7/17/2017.
 */

public class User {
    private String email;
    private String username;

    public User() {
    }

    public User(String email, String username) {
        this.email = email;
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


}
