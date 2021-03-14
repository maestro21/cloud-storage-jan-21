import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    public TextField nickname;

    public Label error;

    public void doLogin(ActionEvent event) throws IOException {
        String name = nickname.getText();

        if(name.equals("public") || !name.matches("^[a-zA-Z0-9]+$")) {
            error.setText("Неверный логин: логин может\n состоять только из латинских букв и цифр");
            return;
        }


        FXMLLoader loader = new FXMLLoader(getClass().getResource("fileSystemLayout.fxml"));
        Parent fsl = loader.load();
        FileSystemController fsc = loader.getController();
        fsc.init(name);
        Scene scene = new Scene(fsl);
        Stage appStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        appStage.setScene(scene);
        appStage.show();
    }
}
