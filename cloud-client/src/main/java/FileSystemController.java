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

    public Label title;
    public String username;
    public ListView<String> listView;
    public ListView<String> clientFileList;
    public ListView<String> serverFileList;
    public ListView<String> serverSharedFileList;
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
            addMessage("Загружаем " + fm.getName() + ": " + fm.getKb() + "kb загружено");
        } catch (IOException e) {
            addMessage("Возникла ошибка при загрузке: " + fm.getName() + ": " + e.getMessage());
        }
    }

    private void sendMessageToServer(String msg)  {
        try {
            os.writeObject(new CommandMessage(msg, this.username));
            os.flush();
            addMessage("Отправили команду на сервер: " + msg);
        } catch (IOException e) {
            addMessage("Возникла ошибка: " + e.getMessage());
        }
    }

    private void sendSharedRequestToServer(String msg)  {
        try {
            os.writeObject(new CommandMessage(msg, "public"));
            os.flush();
            addMessage("Отправили команду на сервер: " + msg);
        } catch (IOException e) {
            addMessage("Возникла ошибка: " + e.getMessage());
        }
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        NFSResponse resp = fs.ls();
        // отображаем список файлов клиента.
        updateFileList(clientFileList, resp.getFilesList());
        bindMouseClicks();
        bindConnection();
    }

    public void init(String username) {
        this.username = username;
        this.title.setText("Облачное хранилище пользователя " + username);
        // отправляем команду на сервер для получения списка файлов в корневой директории пользователя username
        sendMessageToServer("ls");
        sendSharedRequestToServer("ls");
    }


    private void bindMouseClicks() {
        // обрабатываем щелчки по списку файлов клиента
        clientFileList.setOnMouseClicked(a -> {
            if (a.getClickCount() == 2) {
                String fileName = clientFileList.getSelectionModel().getSelectedItem();
                //  если это директория - открываем ее
                if(fs.isDir(fileName)) {
                    NFSResponse resp = fs.cd(fileName);
                    updateFileList(clientFileList, resp.getFilesList());
                } else {
                   // если это файл - отправляем его на сервер
                   NFSResponse resp = fs.transfer(fileName, this::sendFileToServer, this.username);
                   addMessage(resp.getMessage());
                }
            }
        });

        // обрабатываем щелчки по списку файлов на сервере
        serverFileList.setOnMouseClicked(a -> {
            if (a.getClickCount() == 2) {
                String fileName = serverFileList.getSelectionModel().getSelectedItem();
                sendMessageToServer("open " + fileName);
            }
        });

        serverSharedFileList.setOnMouseClicked(a -> {
            if (a.getClickCount() == 2) {
                String fileName = serverSharedFileList.getSelectionModel().getSelectedItem();
                sendSharedRequestToServer("open " + fileName);
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
            addMessage("Скачиваем " + resp.getMessage());
        } else if (msg instanceof NFSResponse) {
            String message = ((NFSResponse) msg).getMessage();
            addMessage(message);
            if(((NFSResponse) msg).getFilesList() != null) {
                if(msg.getUsername().equals("public")) {
                    updateFileList(serverSharedFileList, ((NFSResponse) msg).getFilesList());
                } else {
                    updateFileList(serverFileList, ((NFSResponse) msg).getFilesList());
                }
            }
            NFSResponse resp = fs.ls();
            updateFileList(clientFileList, resp.getFilesList());
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
        Platform.runLater(() -> {
            listView.getItems().add(msg);
            listView.scrollTo(listView.getItems().size() - 1);
        } );
    }
}
