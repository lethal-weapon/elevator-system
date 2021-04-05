package app;

import static constants.CommonConstant.DIRECTION_UP;
import static constants.CommonConstant.DIRECTION_DOWN;
import static constants.CommonConstant.NO_DIRECTION;

import java.util.Set;

/**
 * Pick the optimal synchronized lift to serve a new request.
 *
 * Optimal here means the best tradeoff between the efficiency
 * of the system and the waiting time of individuals.
 */
public class ElevatorScheduler {

  private static int floor;
  private static Set<Elevator> lifts;

  public static Elevator pickBestSyncLift(int requestFloor,
                                          int requestDirection,
                                          Set<Elevator> allSyncLifts) {
    floor = requestFloor;
    lifts = allSyncLifts;

    if (lifts.size() == 0)
      return null;
    else if (lifts.size() == 1)
      return lifts.stream().findFirst().get();
    else {
      return requestDirection == DIRECTION_UP ?
          pickBestOneForUpRequest() :
          pickBestOneForDownRequest();
    }
  }

  private static Elevator pickBestOneForDownRequest() {
    // Case I: lift's going down too but still above us
    for (Elevator lift : lifts) {
      if (lift.getDirection() == DIRECTION_DOWN &&
          lift.getNextStopFloor() >= floor) {
        return lift;
      }
    }
    // Case II: see if there is any inactive lift to use
    Elevator candidate = pickNearestSuspendedLift();
    if (candidate != null) {
      return candidate;
    }
    // Case III: see if there is a lift that is going up but
    // will change its direction soon, it maybe still pretty good
    Elevator candidate2 = pickHighestOrLowestLift(DIRECTION_UP);
    if (candidate2 != null) {
      return candidate2;
    }
    // Case IV: lift's going down too but we just missed
    for (Elevator lift : lifts) {
      if (lift.getDirection() == DIRECTION_DOWN &&
          lift.getNextStopFloor() < floor) {
        return lift;
      }
    }
    // Pick a random one if no matches
    return lifts.stream().findAny().get();
  }

  private static Elevator pickBestOneForUpRequest() {
    // Case I: lift's going up too but still below us
    for (Elevator lift : lifts) {
      if (lift.getDirection() == DIRECTION_UP &&
          lift.getNextStopFloor() <= floor) {
        return lift;
      }
    }
    // Case II: see if there is any inactive lift to use
    Elevator candidate = pickNearestSuspendedLift();
    if (candidate != null) {
      return candidate;
    }
    // Case III: see if there is a lift that is going down but
    // will change its direction soon, it maybe still pretty good
    Elevator candidate2 = pickHighestOrLowestLift(DIRECTION_DOWN);
    if (candidate2 != null) {
      return candidate2;
    }
    // Case IV: lift's going up too but we just missed
    for (Elevator lift : lifts) {
      if (lift.getDirection() == DIRECTION_UP &&
          lift.getNextStopFloor() > floor) {
        return lift;
      }
    }
    // Pick a random one if no matches
    return lifts.stream().findAny().get();
  }

  private static Elevator pickNearestSuspendedLift() {
    Elevator candidate = null;
    for (Elevator lift : lifts) {
      if (lift.getDirection() == NO_DIRECTION) {
        if (candidate == null ||
             (Math.abs(lift.getCurrentFloor() - floor) <
              Math.abs(candidate.getCurrentFloor() - floor))) {

          candidate = lift;
        }
      }
    }
    return candidate;
  }

  private static Elevator pickHighestOrLowestLift(int liftDirection) {
    Elevator candidate = null;
    for (Elevator lift : lifts) {
      if (lift.getDirection() == liftDirection) {
        if (candidate == null ||
            (liftDirection == DIRECTION_UP &&
             lift.getCurrentFloor() > candidate.getCurrentFloor()) ||
            (liftDirection == DIRECTION_DOWN &&
             lift.getCurrentFloor() < candidate.getCurrentFloor())) {

          candidate = lift;
        }
      }
    }
    return candidate;
  }
}
