package niofilesystem;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class NioFileSystem {

    private String rootPath;

    private Path filePath;

    public NioFileSystem(String rootPath) {
        this.rootPath = rootPath;
        filePath = Paths.get(rootPath);
    }

    public NFSResponse success(String msg) {
        NFSResponse ls = ls();
        return new NFSResponse(msg, ls.getFiles());
    }

    public NFSResponse error(String msg) {
        return new NFSResponse("Error occured: " + msg);
    }

    public NFSResponse ls() {
        try {
            String files = Files.list(filePath)
                .map(path -> path.getFileName().toString())
                .collect(Collectors.joining(", "));
            return new NFSResponse("Listing files in directory " + filePath.toString(), files);
        } catch (IOException e) {
            return error(e.getMessage());
        }
    }

    public NFSResponse cd(String targetPath) {
        Path fileDirBefore = filePath;
        filePath = filePath.resolve(targetPath);
        if (!Files.isDirectory(filePath)) {
            filePath = fileDirBefore;
            return error(targetPath + " is not a directory");
        }  else if(!Files.exists(filePath)) {
            filePath = fileDirBefore;
            return error("Directory " + targetPath + " don`t exist");
        }
        return success("Successfully changed server path to: " + filePath);
    }

    public NFSResponse cat(String targetPath) {
        try {
            Path path = filePath.resolve(targetPath);
            Files.createDirectory(path);
            return success("Directory " + targetPath + " created successfully");
        } catch(FileAlreadyExistsException e){
            return error("File or directory already exists");
        } catch (IOException e) {
            return error(e.getMessage());
        }
    }


    public NFSResponse touch(String targetPath) {
        try {
            Path path = filePath.resolve(targetPath);
            Files.createFile(path);
            return success("File " + targetPath + " created successfully");
        } catch(FileAlreadyExistsException e){
            return error("File or directory already exists");
        } catch (IOException e) {
            return error(e.getMessage());
        }
    }

    public boolean isDir(String targetPath) {
        Path path = filePath.resolve(targetPath);
        return Files.isDirectory(path);
    }
}
