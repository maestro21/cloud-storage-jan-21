package model;

import java.io.Serializable;

public class Message implements Serializable {

    protected String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
