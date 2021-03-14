package model;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;

public class FileMessage extends Message {

    private final String name;
    private final byte[] data;
    private final int kb;

    public FileMessage(String name, byte[] data, int kb, String username) throws IOException {
        this.name = name;
        this.data = data;
        this.kb = kb;
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }

    public int getKb() { return kb; }

    @Override
    public String toString() {
        return "model.FileMessage{" +
                "name='" + name + '\'' +
                ", data=" + Arrays.toString(data) +
                ", kb=" + kb +
                '}';
    }
}
