package constants;

import static constants.ElevatorConstant.ELEVATOR_HEIGHT;

public class BuildingConstant {
  // Numbers of total floors
  public static final int MIN_FLOORS = 3;
  public static final int MAX_FLOORS = 13;

  // Numbers of underground floors
  public static final int MIN_UNDERGROUNDS = 0;
  public static final int MAX_UNDERGROUNDS = 3;

  // Numbers of elevators
  public static final int MIN_ELEVATORS = 1;
  public static final int MAX_ELEVATORS = 6;

  // The height of each floor depends on the height of elevator
  public static final double FLOOR_HEIGHT = ELEVATOR_HEIGHT;

  // Traffic control interval
  public static final long CONTROLLER_INTERVAL = 500L;
}
