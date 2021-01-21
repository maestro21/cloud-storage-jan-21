import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NioClientTest {


    private SocketChannel nioClient = null;

    public static void main(String[] args) throws IOException, InterruptedException {
        new NioClientTest();
    }

    public NioClientTest() throws IOException, InterruptedException {
        InetSocketAddress conn = new InetSocketAddress("localhost", 8189);
        this.nioClient = SocketChannel.open(conn);
        msg("ls");
        msg("cat test");
        msg("touch test/test.txt");
        msg("ls");
        msg("cd test");
        msg("touch test2.txt");
        msg("ls");

        this.nioClient.close();
    }

    private void msg(String msg) throws InterruptedException, IOException {
        System.out.println("Sending message: " + msg);
        byte[] message = msg.getBytes();
        ByteBuffer buffer = ByteBuffer.wrap(message);
        nioClient.write(buffer);
        buffer.clear();
        Thread.sleep(1000);
    }
}
