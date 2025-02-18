package password.manager.lib;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        ReadablePasswordField custom1 = new ReadablePasswordField();
        ReadablePasswordField custom2 = new ReadablePasswordField();

        VBox root = new VBox();
        root.getChildren().addAll(custom1, custom2);

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