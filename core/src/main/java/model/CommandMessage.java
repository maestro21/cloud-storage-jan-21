package model;

import java.io.Serializable;

public class CommandMessage extends Message {

    private final String content;

    public CommandMessage(String content, String username) {
        this.content = content;
        this.username = username;
    }

    public String getContent() {
        return content;
    }


    @Override
    public String toString() {
        return content;
    }
}
