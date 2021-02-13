package niofilesystem;

import model.Message;

public class NFSResponse extends Message {

    private String message;

    private String files;


    public NFSResponse(String message) {
        this.message = message;
    }


    public NFSResponse(String message, String files) {
        this.message = message;
        this.files = files;
    }

    public String getMessage() {
        return message;
    }

    public String getFiles() {
        return files;
    }

    public String[] getFilesList() {
        return files == null ? null : files.split(",");
    }
}
