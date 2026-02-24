package mian;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("Trying to load /fxmls/login.fxml");
        System.out.println(getClass().getResource("/fxmls/login.fxml"));

        Parent root = FXMLLoader.load(getClass().getResource("/fxmls/login.fxml"));

        System.out.println(getClass().getResource("/css/textfeild1.css"));
        System.out.println(getClass().getResource("/css/passwordField.css"));
        System.out.println(getClass().getResource("/css/login-button.css"));

        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(

                getClass().getResource("/css/textfeild1.css").toExternalForm(),
              getClass().getResource("/css/passwordField.css").toExternalForm(),
               getClass().getResource("/css/login-button.css").toExternalForm()
        );

        primaryStage.setTitle("Snapfood - Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
