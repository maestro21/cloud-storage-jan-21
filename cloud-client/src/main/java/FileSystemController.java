import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import model.Message;
import niofilesystem.NFSResponse;
import niofilesystem.NioFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSystemController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(FileSystemController.class);

    public ListView<String> listView;
    public ListView<String> clientFileList;
    public ListView<String> serverFileList;
    private NioFileSystem fileSystem = new NioFileSystem("filesClient");;

    public TextField text;

    private ObjectEncoderOutputStream os;
    private ObjectDecoderInputStream is;

    public void sendMessage(ActionEvent event) throws IOException  {
        String messageContent = text.getText();
        sendMessageToServer(messageContent);
        text.clear();
    }

    private void sendMessageToServer(String msg) throws IOException  {
        os.writeObject(new Message(msg));
        os.flush();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        NFSResponse resp = fileSystem.ls();
        updateFileList(clientFileList, resp.getFilesList());
        try {
            Socket socket = new Socket("localhost", 8189);
            os = new ObjectEncoderOutputStream(socket.getOutputStream());
            is = new ObjectDecoderInputStream(socket.getInputStream());

            new Thread(() -> {
                while (true) {
                    try {
                        NFSResponse response = (NFSResponse) is.readObject();
                        addMessage(response.getMessage());
                        if(response.getFilesList() != null) {
                            updateFileList(serverFileList, response.getFilesList());
                        }
                    } catch (Exception e) {
                        LOG.error("e = ", e);
                        break;
                    }
                }
            }).start();
        } catch (Exception e) {
            LOG.error("e = ", e);
        }

        try {
            sendMessageToServer("ls");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void updateFileList(ListView<String> fileList, String[] files) {
        Platform.runLater(() -> {
            fileList.getItems().clear();
            for (String file : files) {
                fileList.getItems().add(file.trim());
            }
        });
    }


    private void addMessage(String msg) {
        Platform.runLater(() -> listView.getItems().add(msg));
    }
}
