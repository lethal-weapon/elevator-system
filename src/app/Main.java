package app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {
  // The application originally developed with this screen size
  public static final double DEFAULT_SCREEN_WIDTH  = 1366.0f;
  public static final double DEFAULT_SCREEN_HEIGHT = 768.0f;

  public static double SCREEN_WIDTH  = DEFAULT_SCREEN_WIDTH;
  public static double SCREEN_HEIGHT = DEFAULT_SCREEN_HEIGHT;

  public static double SCREEN_WIDTH_SCALE  = 1.00f;
  public static double SCREEN_HEIGHT_SCALE = 1.00f;
  public static double COMBINED_SCALE      = 1.00f;

  @Override
  public void start(Stage primaryStage) throws Exception {
    // Obtain the real screen size
    Rectangle2D screenBounds = Screen.getPrimary().getBounds();
    SCREEN_WIDTH  = screenBounds.getWidth();
    SCREEN_HEIGHT = screenBounds.getHeight();
    // Obtain the scales
    SCREEN_WIDTH_SCALE  = SCREEN_WIDTH / DEFAULT_SCREEN_WIDTH;
    SCREEN_HEIGHT_SCALE = SCREEN_HEIGHT / DEFAULT_SCREEN_HEIGHT;
    COMBINED_SCALE      = (SCREEN_WIDTH_SCALE + SCREEN_HEIGHT_SCALE) / 2;

    // Create the stage which has set stage style transparent
    final Stage stage = new Stage(StageStyle.TRANSPARENT);
    // Create the root node of scene
    Group rootGroup = new Group();

    // Create scene with the width, height and color
    Scene scene = new Scene(rootGroup, SCREEN_WIDTH, SCREEN_HEIGHT, Color.TRANSPARENT);
    scene.setCursor(Cursor.NONE);
    stage.setScene(scene);
    stage.centerOnScreen();
    stage.setFullScreen(true);
    stage.show();

    // Press F/f to toggle full screen
    // Press Q/q to terminate the application
    rootGroup.setOnKeyPressed(keyEvent -> {
      if (keyEvent.getCode() != null) {
        switch (keyEvent.getCode()) {
          case F:
            stage.setFullScreen(!stage.isFullScreen());
            break;
          case Q:
            Platform.exit();
            System.exit(0);
          default:
            break;
        }
      }
    });

    // Create the background with desired size
    Rectangle dragger = new Rectangle(SCREEN_WIDTH, SCREEN_HEIGHT);
    dragger.setFill(Color.BLACK);
    // Create the UI and everything
    Building building = new Building(
        BuildingConfig.newConfig(BuildingConfig.SCENARIO_RESIDENCE_I));

    // Add all nodes to main root group
    rootGroup.getChildren().addAll(dragger, building);
    rootGroup.requestFocus();
  }

	public static void main(String[] args) {
		launch(args);
	}
}
