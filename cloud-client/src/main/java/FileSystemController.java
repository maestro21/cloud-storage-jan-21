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
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import model.CommandMessage;
import model.FileMessage;
import model.Message;
import niofilesystem.NFSResponse;
import niofilesystem.NioFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSystemController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(FileSystemController.class);

    public Label nickname;
    public ListView<String> listView;
    public ListView<String> clientFileList;
    public ListView<String> serverFileList;
    final private NioFileSystem fs = new NioFileSystem("filesClient");;

    public TextField text;

    private ObjectEncoderOutputStream os;
    private ObjectDecoderInputStream is;

    public void sendMessage(ActionEvent event) {
        String messageContent = text.getText();
        sendMessageToServer(messageContent);
        text.clear();
    }

    private void sendFileToServer(FileMessage fm)  {
        try {
            os.writeObject(fm);
            os.flush();
            addMessage("Uploading " + fm.getName() + ": " + fm.getKb() + "kb transferred");
        } catch (IOException e) {
            addMessage("Error occurred on upload" + fm.getName() + ": " + e.getMessage());
        }
    }

    private void sendMessageToServer(String msg)  {
        try {
            os.writeObject(new CommandMessage(msg));
            os.flush();
            addMessage("Sent command to server: " + msg);
        } catch (IOException e) {
            addMessage("Error occurred: " + e.getMessage());
        }
    }

    public String getNickname() {
        return this.nickname.getText();
    }

    public void setNickname(String nickname) {
        this.nickname.setText(nickname);
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        NFSResponse resp = fs.ls();
        // display client file list
        updateFileList(clientFileList, resp.getFilesList());
        bindMouseClicks();
        bindConnection();
        // retrieve from server root dir file list
        sendMessageToServer("ls");
    }

    private void bindMouseClicks() {
        clientFileList.setOnMouseClicked(a -> {
            if (a.getClickCount() == 2) {
                String fileName = clientFileList.getSelectionModel().getSelectedItem();
                if(fs.isDir(fileName)) {
                    sendMessageToServer("cat " + fs.trimBrackets(fileName));
                } else {
                   NFSResponse resp = fs.transfer(fileName, this::sendFileToServer);
                   addMessage(resp.getMessage());
                }
            }
        });

        serverFileList.setOnMouseClicked(a -> {
            if (a.getClickCount() == 2) {
                String fileName = serverFileList.getSelectionModel().getSelectedItem();
                sendMessageToServer("open " + fileName);
            }
        });
    }

    private void bindConnection() {
        try {
            Socket socket = new Socket("localhost", 8189);
            os = new ObjectEncoderOutputStream(socket.getOutputStream());
            is = new ObjectDecoderInputStream(socket.getInputStream());

            new Thread(() -> {
                while (true) {
                    try {
                        Message response = (Message) is.readObject();
                        handleResponse(response);
                    } catch (Exception e) {
                        LOG.error("e = ", e);
                        break;
                    }
                }
            }).start();
        } catch (Exception e) {
            LOG.error("e = ", e);
        }
    }

    private void handleResponse(Message msg) {
        if (msg instanceof FileMessage) {
            NFSResponse resp = fs.put((FileMessage) msg);
            addMessage("Downloading " + resp.getMessage());
        } else if (msg instanceof NFSResponse) {
            String message = ((NFSResponse) msg).getMessage();
            addMessage(message);
            if(((NFSResponse) msg).getFilesList() != null) {
                updateFileList(serverFileList, ((NFSResponse) msg).getFilesList());
            }
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
