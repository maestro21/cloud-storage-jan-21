package niofilesystem;

import model.FileMessage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.Comparator;
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
                .sorted(this::sortDirsFirstThenByName)
                .map(this::getFileName)
                .collect(Collectors.joining(", "));
            return new NFSResponse("Listing files in directory " + filePath.toString(), files);
        } catch (IOException e) {
            return error(e.getMessage());
        }
    }

    private int sortDirsFirstThenByName(Path p1, Path p2) {
        String p1Fname = p1.getFileName().toString();
        String p2Fname = p2.getFileName().toString();
        boolean isP1Dir = isDir(p1Fname);
        boolean isP2Dir = isDir(p2Fname);

        if(isP1Dir == isP2Dir) {
            return p1Fname.compareTo(p2Fname);
        } else {
            return Boolean.compare(isP2Dir, isP1Dir);
        }
    }

    private String getFileName(Path targetPath) {
        String filename = targetPath.getFileName().toString();
        if (isDir(filename)) {
            filename = "[" + filename + "]";
        }
        return filename;
    }

    public String trimBrackets(String string) {
        return string.replaceAll("^\\[|\\]$", "");
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

    public NFSResponse put(FileMessage msg) {
        try {
            Path path = filePath.resolve(msg.getName());
            Files.write(
                path,
                msg.getData(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return success(msg.getName() + " - " + msg.getKb() + "kb transferred");
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return error(e.getMessage());
        }
    }

    public NFSResponse transfer(String targetPath, FMCallback callback) {

        try {
            Path path = filePath.resolve(targetPath);
            RandomAccessFile aFile = new RandomAccessFile(path.toString(), "r");

            FileChannel inChannel = aFile.getChannel();

            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int i = 0;
            while (inChannel.read(buffer) > 0) {
                i++;
                buffer.flip();
                FileMessage fm = new FileMessage(targetPath, buffer.array(), i);
                callback.call(fm);
                buffer.clear();
            }

            inChannel.close();
            aFile.close();

            return success("Transferring file " + targetPath);
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
        Path path = filePath.resolve(trimBrackets(targetPath));
        return Files.isDirectory(path);
    }
}
