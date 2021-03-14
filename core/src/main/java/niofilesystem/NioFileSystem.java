package niofilesystem;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import model.FileMessage;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.stream.Collectors;


public class NioFileSystem {

    private String rootPath;

    private Path filePath;

    public NioFileSystem(String rootPath){
        this.rootPath = rootPath;
        filePath = Paths.get(rootPath);
        if(!Files.exists(filePath)) {
            try {
                Files.createDirectory(filePath);
            } catch (IOException e) {}
        }
    }

    public NFSResponse success(String msg) {
        NFSResponse ls = ls();
        return new NFSResponse(msg, ls.getFiles());
    }

    public NFSResponse error(String msg) {
        return new NFSResponse("Ошибка: " + msg);
    }

    public NFSResponse ls() {
        try {
            String files = Files.list(filePath)
                .sorted(this::sortDirsFirstThenByName)
                .map(this::getFileName)
                .collect(Collectors.joining(", "));
            if(!filePath.toString().equals(Paths.get(rootPath).toString())) files = "..," + files;
            return new NFSResponse("Список файлов в директории " + filePath.toString(), files);
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
        if(targetPath.equals("..")) {
            filePath = filePath.getParent();
        } else {
            filePath = filePath.resolve(trimBrackets(targetPath));
        }
        if (!Files.isDirectory(filePath)) {
            filePath = fileDirBefore;
            return error(targetPath + " не директория");
        } else if (!Files.exists(filePath)) {
            filePath = fileDirBefore;
            return error("Директория " + targetPath + " не существует");
        }
        return success("Поменяли путь на: " + filePath);
    }

    public NFSResponse cat(String targetPath) {
        try {
            Path path = filePath.resolve(targetPath);
            Files.createDirectory(path);
            return success("Директория " + targetPath + " успешно создана");
        } catch(FileAlreadyExistsException e){
            return error("Файл или директория не существует");
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
            return success(msg.getName() + " - " + msg.getKb() + "kb переслали");
        } catch (IOException e) {
            return error(e.getMessage());
        }
    }

    public NFSResponse transfer(String targetPath, FMCallback callback) {
        return transfer(targetPath, callback, "");
    }

    public NFSResponse transfer(String targetPath, FMCallback callback, String username) {

        try {
            Path path = filePath.resolve(targetPath);
            RandomAccessFile aFile = new RandomAccessFile(path.toString(), "r");

            FileChannel inChannel = aFile.getChannel();

            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int i = 0;
            while (inChannel.read(buffer) > 0) {
                i++;
                buffer.flip();
                FileMessage fm = new FileMessage(targetPath, buffer.array(), i, username);
                callback.call(fm);
                buffer.clear();
            }

            inChannel.close();
            aFile.close();

            return success("Переслали файл " + targetPath);
        } catch (IOException e) {
            return error(e.getMessage());
        }
    }

    public NFSResponse mkDir(String targetPath) {
        try {
            Path path = filePath.resolve(targetPath);
            Files.createDirectory(path);
            return success("Директория " + targetPath + " создана успешно");
        } catch(FileAlreadyExistsException e){
            return error("Файл или директория уже существуют");
        } catch (IOException e) {
            return error(e.getMessage());
        }
    }


    public NFSResponse touch(String targetPath) {
        try {
            Path path = filePath.resolve(targetPath);
            Files.createFile(path);
            return success("Файл " + targetPath + " создан успешно");
        } catch(FileAlreadyExistsException e){
            return error("Файл или директория уже существуют");
        } catch (IOException e) {
            return error(e.getMessage());
        }
    }

    public NFSResponse rm(String targetPath) {
        try {
            Path path = filePath.resolve(targetPath);
            Files.delete(path);
            return success("Файл " + targetPath + " успешно удален");
        } catch(DirectoryNotEmptyException e){
            return error("Директорию нельзя удалить, т.к. в ней есть файлы");
        } catch(NoSuchFileException e){
            return error("Файл или директория не существует");
        } catch (IOException e) {
            return error(e.getMessage());
        }
    }

    public NFSResponse rn(String srcPath, String targetPath) {
        try {
            Path pathFrom = filePath.resolve(srcPath);
            Path pathTo = filePath.resolve(targetPath);
            Files.move(pathFrom, pathTo);
            return success("Файл " + srcPath + " успешно переименован в " + targetPath);
        } catch(DirectoryNotEmptyException e){
            return error("Директорию нельзя удалить, т.к. в ней есть файлы");
        } catch(FileAlreadyExistsException e){
            return error("Файл с таким именем уже существует");

        } catch (IOException|UnsupportedOperationException e) {
            return error(e.getMessage());
        }
    }

    public boolean isDir(String targetPath) {
        Path path = filePath.resolve(trimBrackets(targetPath));
        return Files.isDirectory(path);
    }
}
