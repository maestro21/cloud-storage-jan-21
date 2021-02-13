package netty;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import model.CommandMessage;
import model.FileMessage;
import model.Message;
import niofilesystem.NFSResponse;
import niofilesystem.NioFileSystem;

public class FileRequestHandler extends SimpleChannelInboundHandler<Message> {

    private static final ConcurrentLinkedDeque<ChannelHandlerContext> clients = new ConcurrentLinkedDeque<>();

    private NioFileSystem fs = new NioFileSystem("filesServer");

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        clients.add(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        NFSResponse response = null;
        if (msg instanceof FileMessage) {
            response = fs.put((FileMessage) msg);
        } else if (msg instanceof CommandMessage) {
            response = handleCommandMessage((CommandMessage) msg);
        }

        if(response != null) {
            writeToChannel(response);
        }
    }

    private void writeToChannel(NFSResponse msg) {
        for (ChannelHandlerContext client : clients) {
            client.writeAndFlush(msg);
        }
    }

    private void writeToChannel(FileMessage msg) {
        for (ChannelHandlerContext client : clients) {
            client.writeAndFlush(msg);
        }
    }


    private NFSResponse handleCommandMessage(CommandMessage msg) {
        String command = msg.getContent();
        String[] args = command.split(" ");
        NFSResponse response = new NFSResponse(command);

        if (command.equals("ls")) {
            response = fs.ls();
        }

        if (command.startsWith("cd")) {
            if (args.length != 2) {
                response = fs.error("Wrong argument count");
            } else {
                response = fs.cd(args[1]);
            }
        }

        if (command.startsWith("cat")) {
            if (args.length != 2) {
                response = fs.error("Wrong argument count");
            } else {
                response = fs.cat(args[1]);
            }
        }

        if (command.startsWith("touch")) {
            if (args.length != 2) {
                response = fs.error("Wrong argument count");
            } else {
                response = fs.touch(args[1]);
            }
        }

        if (command.startsWith("open")) {
            if (args.length != 2) {
                response = fs.error("Wrong argument count");
            } else {
                String fileName = args[1];

                if (fs.isDir(fileName)) {
                   response = fs.cd(fileName);
                } else {
                    response = fs.transfer(fileName, this::writeToChannel);
                }
            }
        }
        return response;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        clients.remove(ctx);
    }
}
