package app;

import static constants.CommonConstant.DIRECTION_UP;
import static constants.CommonConstant.DIRECTION_DOWN;
import static constants.ElevatorConstant.ELEVATOR_HEIGHT;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

/**
 * Elevator buttons are the ones launching the initial requests
 * (outside lifts), not those number buttons inside the lifts.
 */
public class ElevatorButton extends StackPane {
  // Half side of the button triangle
  private static final double HALF_SIDE = ELEVATOR_HEIGHT / 8.0;

  private final Polygon shape;     // its shape, a triangle
  private final String elevatorId; // lift it belongs to
  private final int floor;         // floor it is at
  private final int direction;     // direction it faces
  private boolean isOn;            // its light, on/off

  private ElevatorButton(Polygon shape,
                         String elevatorId,
                         int floor,
                         int direction) {
    this.shape      = shape;
    this.elevatorId = elevatorId;
    this.floor      = floor;
    this.direction  = direction;
    this.turnOff();
    this.getChildren().add(shape);
  }

  public String getElevatorId() {
    return this.elevatorId;
  }

  public int getFloor() {
    return this.floor;
  }

  public int getDirection() {
    return this.direction;
  }

  public boolean isLightOn() {
    return this.isOn;
  }

  public void turnOn() {
    shape.setFill(Color.ORANGE);
    shape.setStroke(Color.ORANGE);
    shape.setOpacity(1.0);
    isOn = true;
  }

  public void turnOff() {
    shape.setFill(Color.BLACK);
    shape.setStroke(Color.WHITESMOKE);
    shape.setOpacity(0.75);
    isOn = false;
  }

  public static ElevatorButton createUpButton(String elevatorId, int floor) {
    Polygon shape = new Polygon();
    double tan60 = Math.tan(Math.toRadians(60));

    shape.getPoints().addAll(
        0.0, 0.0,
        HALF_SIDE, HALF_SIDE * tan60,
        -HALF_SIDE, HALF_SIDE * tan60
    );
    return new ElevatorButton(shape, elevatorId, floor, DIRECTION_UP);
  }

  public static ElevatorButton createDownButton(String elevatorId, int floor) {
    Polygon shape = new Polygon();
    double tan60 = Math.tan(Math.toRadians(60));

    shape.getPoints().addAll(
        0.0, 0.0,
        -HALF_SIDE, -HALF_SIDE * tan60,
        HALF_SIDE, -HALF_SIDE * tan60
    );
    return new ElevatorButton(shape, elevatorId, floor, DIRECTION_DOWN);
  }
}
