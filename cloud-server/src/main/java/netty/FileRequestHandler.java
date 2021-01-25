package netty;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
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

        for (ChannelHandlerContext client : clients) {
            client.writeAndFlush(response);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        clients.remove(ctx);
    }
}
