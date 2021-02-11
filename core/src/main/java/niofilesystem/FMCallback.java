package niofilesystem;

import model.FileMessage;

public interface FMCallback {
    void call(FileMessage fm);
}