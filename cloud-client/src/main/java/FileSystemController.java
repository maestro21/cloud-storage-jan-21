import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.ResourceBundle;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
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
    private NioFileSystem fs;

    public TextField text;

    private ObjectEncoderOutputStream os;
    private ObjectDecoderInputStream is;


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
    }

    public void init(String username) {
        this.username = username;
        this.title.setText("Облачное хранилище пользователя " + username);
        fs = new NioFileSystem("filesClient/" + username);
        NFSResponse resp = fs.ls();

        // отображаем список файлов клиента.
        updateFileList(clientFileList, resp.getFilesList());
        bindMouseClicks();
        bindConnection();

        // отправляем команду на сервер для получения списка файлов в корневой директории пользователя username
        sendMessageToServer("ls");
        sendSharedRequestToServer("ls");
    }


    private void bindMouseClicks() {
        // обрабатываем щелчки по списку файлов клиента
        clientFileList.setOnMouseClicked(a -> {
            if (a.getClickCount() == 2) {
                String fileName = fs.trimBrackets(clientFileList.getSelectionModel().getSelectedItem());
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
                String fileName = fs.trimBrackets(serverFileList.getSelectionModel().getSelectedItem());
                sendMessageToServer("open " + fileName);
            }
        });

        serverSharedFileList.setOnMouseClicked(a -> {
            if (a.getClickCount() == 2) {
                String fileName = fs.trimBrackets(serverSharedFileList.getSelectionModel().getSelectedItem());
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

    public void serverDelFile(ActionEvent event) {
        String fileName = fs.trimBrackets(serverFileList.getSelectionModel().getSelectedItem());
        if(fileName.equals("..")) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Удалить файл");
        alert.setHeaderText("Удалить файл");
        alert.setContentText("Вы действительно хотите удалить этот файл?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK){
            sendMessageToServer("rm " + fileName);
        }
    }

    public void serverRenameFile(ActionEvent event) {
        String fileName = fs.trimBrackets(serverFileList.getSelectionModel().getSelectedItem());
        if(fileName.equals("..")) return;

        TextInputDialog dialog = new TextInputDialog(fileName);
        dialog.setTitle("Переименовать файл");
        dialog.setHeaderText("Переименовать файл");
        dialog.setContentText("Новое имя:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()){
            sendMessageToServer("rn " + fileName + ' ' + result.get());
        }
    }

    public void clientDelFile(ActionEvent event) {
        String fileName = fs.trimBrackets(clientFileList.getSelectionModel().getSelectedItem());
        if(fileName.equals("..")) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Удалить файл");
        alert.setHeaderText("Удалить файл");
        alert.setContentText("Вы действительно хотите удалить этот файл?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK){
            NFSResponse resp = fs.rm(fileName);
            addMessage(resp.getMessage());
            updateFileList(clientFileList,fs.ls().getFilesList());
        }
    }

    public void clientRenameFile(ActionEvent event) {
        String fileName = fs.trimBrackets(clientFileList.getSelectionModel().getSelectedItem());
        if(fileName.equals("..")) return;

        TextInputDialog dialog = new TextInputDialog(fileName);
        dialog.setTitle("Переименовать файл");
        dialog.setHeaderText("Переименовать файл");
        dialog.setContentText("Новое имя:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()){
            NFSResponse resp = fs.rn(fileName,result.get());
            addMessage(resp.getMessage());
            updateFileList(clientFileList,fs.ls().getFilesList());
        }
    }


    public void clientMkDir(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Создать директорию");
        dialog.setHeaderText("Создать директорию");
        dialog.setContentText("Имя директории:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()){
            NFSResponse resp = fs.mkDir(result.get());
            addMessage(resp.getMessage());
            updateFileList(clientFileList,fs.ls().getFilesList());
        }
    }


    public void serverMkDir(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Создать директорию");
        dialog.setHeaderText("Создать директорию");
        dialog.setContentText("Имя директории:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()){
            sendMessageToServer("mkdir " + result.get());
        }
    }
}
