package netty;

import java.util.HashMap;
import java.util.Map;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import model.CommandMessage;
import model.FileMessage;
import model.Message;
import niofilesystem.NFSResponse;
import niofilesystem.NioFileSystem;

public class FileRequestHandler extends SimpleChannelInboundHandler<Message> {

    private static final Map<String,ChannelHandlerContext> clients = new HashMap<String,ChannelHandlerContext>();
    private static final Map<String,NioFileSystem> fss = new HashMap<String, NioFileSystem>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        NFSResponse response = null;
        String username = msg.getUsername();
        NioFileSystem fs = this.getFileSystem(username);
        clients.put(username, ctx);
        if (msg instanceof FileMessage) {
            response = fs.put((FileMessage) msg);
        } else if (msg instanceof CommandMessage) {
            response = handleCommandMessage((CommandMessage) msg, fs);
        }

        if(response != null) {
            response.setUsername(msg.getUsername());
            ctx.writeAndFlush(response);
        }
    }

    private NioFileSystem getFileSystem(String username) {
        NioFileSystem fs = fss.get(username);
        if(fs == null) {
            fs = new NioFileSystem("filesServer/" + username);
            fss.put(username, fs);
        }
        return fs;
    }

    private void writeToChannel(FileMessage msg) {
        ChannelHandlerContext ctx = clients.get(msg.getUsername());
        if(ctx != null) {
            ctx.writeAndFlush(msg);
        }
    }


    private NFSResponse handleCommandMessage(CommandMessage msg, NioFileSystem fs) {
        String command = msg.getContent();
        String[] args = command.split(" ");
        NFSResponse response = new NFSResponse(command);

        if (command.equals("ls")) {
            response = fs.ls();
        }

        if (command.startsWith("cd")) {
            if (args.length != 2) {
                response = fs.error("Должно быть только 2 аргумента");
            } else {
                response = fs.cd(args[1]);
            }
        }

        if (command.startsWith("cat")) {
            if (args.length != 2) {
                response = fs.error("Должно быть только 2 аргумента");
            } else {
                response = fs.cat(args[1]);
            }
        }

        if (command.startsWith("touch")) {
            if (args.length != 2) {
                response = fs.error("Должно быть только 2 аргумента");
            } else {
                response = fs.touch(args[1]);
            }
        }

        if (command.startsWith("open")) {
            if (args.length != 2) {
                response = fs.error("Должно быть только 2 аргумента");
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
