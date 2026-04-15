package chess;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ChessApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        ChessBoard board = new ChessBoard();
        Scene scene = new Scene(board.getRoot(), 640, 680);
        scene.getStylesheets().add(getClass().getResource("/chess/chess.css").toExternalForm());
        primaryStage.setTitle("Chess");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
