package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

public class NioServer {

    private final ServerSocketChannel serverChannel = ServerSocketChannel.open();
    private final Selector selector = Selector.open();
    private final ByteBuffer buffer = ByteBuffer.allocate(5);
    private Path serverPath = Paths.get("serverDir");

    public NioServer() throws IOException {
        serverChannel.bind(new InetSocketAddress("localhost", 8189));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        while (serverChannel.isOpen()) {
            selector.select(); // block
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (key.isAcceptable()) {
                    handleAccept(key);
                }
                if (key.isReadable()) {
                    handleRead(key);
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new NioServer();
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        int read = 0;
        StringBuilder msg = new StringBuilder();
        while ((read = channel.read(buffer)) > 0) {
            buffer.flip();
            while (buffer.hasRemaining()) {
                msg.append((char) buffer.get());
            }
            buffer.clear();
        }
        String command = msg.toString().replaceAll("[\n|\r]", "");
        String[] args = command.split(" ");

        if(!command.isEmpty()) {
            System.out.println("Received command: " + command);
        }

        if (command.equals("ls")) {
            response("Showing files in directory " + serverPath, channel);
            String files = Files.list(serverPath)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.joining(", "));
            files += "\n";
            response(files, channel);
        }

        if (command.startsWith("cd")) {
            if (args.length != 2) {
                response("Wrong argument count", channel);
            } else {
                response("Changing server path to: " + serverPath, channel);
                String targetPath = args[1];
                Path serverDirBefore = serverPath;
                serverPath = serverPath.resolve(targetPath);
                if (!Files.isDirectory(serverPath)) {
                    response(targetPath + " is not a directory", channel);
                    serverPath = serverDirBefore;
                }  else if(!Files.exists(serverPath)) {
                    response("Directory " + targetPath + " don`t exist", channel);
                    serverPath = serverDirBefore;
                } else {
                    response("Successfully changed server path to: " + serverPath, channel);
                }
            }
        }

        if (command.startsWith("cat")) {
            if (args.length != 2) {
                response("Wrong argument count", channel);
            } else {
                String targetPath = args[1];
                response("Creating directory " + targetPath, channel);
                try {
                    Path path = serverPath.resolve(targetPath);
                    Files.createDirectory(path);
                    response("Directory " + targetPath + " created successfully", channel);
                } catch(FileAlreadyExistsException e){
                    response("File or directory already exists", channel);
                } catch (IOException e) {
                    response("Error occurred: " + e.getMessage(), channel);
                }
            }
        }

        if (command.startsWith("touch")) {
            if (args.length != 2) {
                response("Wrong argument count", channel);
            } else {
                String targetPath = args[1];
                response("Creating file " + targetPath, channel);
                try {
                    Path path = serverPath.resolve(targetPath);
                    Files.createFile(path);
                    response("File " + targetPath + " created successfully", channel);
                } catch(FileAlreadyExistsException e){
                    response("File or directory already exists", channel);
                } catch (IOException e) {
                    response("Error occurred: " + e.getMessage(), channel);
                }
            }
        }
    }

    private void response(String str, SocketChannel channel) throws IOException {
        str += "\n";
        System.out.println("Sending response: " + str);
        channel.write(ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8)));
    }

    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
    }
}
