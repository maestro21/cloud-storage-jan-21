import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class FileSystemApplication extends Application {

    private String nickname;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent parent = FXMLLoader.load(getClass().getResource("login.fxml"));
        primaryStage.setScene(new Scene(parent));
        primaryStage.setTitle("Client-Server file system");
        primaryStage.show();
    }
}
