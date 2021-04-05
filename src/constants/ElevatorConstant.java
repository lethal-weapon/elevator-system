package constants;

import static app.Main.SCREEN_WIDTH_SCALE;
import static app.Main.SCREEN_HEIGHT_SCALE;

import static constants.PersonConstant.PERSON_WIDTH;
import static constants.PersonConstant.PERSON_HEIGHT;

/**
 * All values directly related to drawing need to multiply the scales
 * from the Main class where the real screen size is obtained.
 *
 * Multiply the width scale if relating to horizontal direction.
 * Multiply the height scale if relating to vertical direction.
 * Multiply the combined scale if relating to both.
 */
public class ElevatorConstant {
  // Size of a lift
  public static final double ELEVATOR_HEIGHT = 1.15 * PERSON_HEIGHT;
  public static final double ELEVATOR_WIDTH  = 3.00 * PERSON_WIDTH;

  // Distance between each lift
  public static final double ELEVATOR_GAP = 4.50 * PERSON_WIDTH;

  // Door's opening & closing control
  public static final double DOOR_STEP = 1.00f;
  public static final double DOOR_TIME = 30.0 / SCREEN_WIDTH_SCALE;

  // Door's waiting duration between fully opened and start closing
  public static final double DOOR_WAIT_TIME = 2000.0f;

  // Lift's vertical movement control
  public static final double MOVE_STEP = 1.00f;
  public static final double MOVE_TIME = 15.0 / SCREEN_HEIGHT_SCALE;

  // Lift's status checking interval
  public static final long OPERATION_INTERVAL = 500L;

  /** Lift must be in one of these states in any given time */
  public static enum ElevatorState {
    IDLE, OPENING, WAITING, CLOSING, MOVING
  }
}
