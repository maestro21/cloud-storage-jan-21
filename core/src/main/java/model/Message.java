package model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {

    private final String content;

    public Message(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }


    @Override
    public String toString() {
        return content;
    }
}
