package password.manager.lib;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        ReadablePasswordField custom1 = new ReadablePasswordField();
        ReadablePasswordFieldWithStr custom2 = new ReadablePasswordFieldWithStr();
        custom2.setPrefSize(548.0, 70.0);

        AnchorPane root = new AnchorPane();
        root.getChildren().addAll(custom1, custom2);
        AnchorPane.setTopAnchor(custom1, 0.0);
        AnchorPane.setTopAnchor(custom2, 200.0);

        stage.setScene(new Scene(root));
        stage.setTitle("Test");
        stage.setWidth(900);
        stage.setHeight(600);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}