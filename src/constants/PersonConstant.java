package constants;

import static app.Main.COMBINED_SCALE;
import static app.Main.SCREEN_WIDTH_SCALE;
import static app.Main.SCREEN_HEIGHT_SCALE;

/**
 * All values directly related to drawing need to multiply the scales
 * from the Main class where the real screen size is obtained.
 *
 * Multiply the width scale if relating to horizontal direction.
 * Multiply the height scale if relating to vertical direction.
 * Multiply the combined scale if relating to both.
 */
public class PersonConstant {
  // Sizes of individual parts of a person
  public static final double HEAD_RADIUS = 8.00 * COMBINED_SCALE;
  public static final double BODY_LENGTH = 18.0 * SCREEN_HEIGHT_SCALE;
  public static final double ARM_LENGTH  = 12.0 * COMBINED_SCALE;
  public static final double ARM_ANGLE   = Math.toRadians(30.0); // 30 degree to horizontal
  public static final double LEG_LENGTH  = 12.0 * COMBINED_SCALE;
  public static final double LEG_ANGLE   = Math.toRadians(30.0); // 30 degree to vertical

  // Size of a person, approximate values
  public static final double PERSON_WIDTH  = 2 * ARM_LENGTH * Math.cos(ARM_ANGLE);
  public static final double PERSON_HEIGHT =
      2 * HEAD_RADIUS + BODY_LENGTH + LEG_LENGTH * Math.cos(LEG_ANGLE);

  // Side left or right a person enters the screen
  public static final int SIDE_LEFT  = 1;
  public static final int SIDE_RIGHT = 2;

  // Person's horizontal movement control
  public static final double MOVE_STEP = 5.0f;
  public static final double MOVE_TIME = 15.0 / SCREEN_WIDTH_SCALE;

  // Check lift status for every this interval while waiting
  public static final long WAIT_INTERVAL = 500L;

  /** Different phases a person interacts with the system */
  public static enum PersonState {
    ENTER, WAITING, WALK_IN, MOVE_WITH_LIFT, WALK_OUT, EXIT
  }
}
