package app;

import static app.Main.*;

import static constants.BuildingConstant.*;
import static constants.CommonConstant.*;
import static constants.ElevatorConstant.*;
import static constants.PersonConstant.PERSON_WIDTH;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class Elevator extends StackPane {

  private final String id;
  private final boolean isSynchronized;
  private final BuildingConfig config;

  private ElevatorState state;
  private int direction;
  private int currentFloor;

  private Set<Person> passengers;        // people who stand inside the lift
  private TreeSet<Integer> destinations; // floors the lift will stop
  private TreeSet<Integer> upRequests;   // going up requests associated with the lift
  private TreeSet<Integer> downRequests; // going down requests associated with the lift
  private List<ElevatorButton> buttons;  // elevator buttons in the whole building
  private ElevatorModel model;

  // When lift is going down from a higher floor to a lower one for an UP request,
  // Its direction should change after reaching that lower floor, vice versa.
  private boolean needReverse;

  public Elevator(String id,
                  boolean isSynchronized,
                  BuildingConfig config,
                  List<ElevatorButton> buttons) {
    this.id = id;
    this.isSynchronized = isSynchronized;
    this.config = config;
    this.buttons = buttons;

    this.state = ElevatorState.IDLE;
    this.direction = NO_DIRECTION;
    this.currentFloor = FloorPair.getRandomFloor(config);
    this.needReverse = false;

    this.passengers   = new HashSet<>();
    this.destinations = new TreeSet<>();
    this.upRequests   = new TreeSet<>();
    this.downRequests = new TreeSet<>();
    this.model = new ElevatorModel();
    this.getChildren().add(model);

    new Thread(() -> {
      try {
        while (true) {
          Platform.runLater(() -> operate());
          Thread.sleep(OPERATION_INTERVAL);
        }
      } catch (InterruptedException ex) {}
    }).start();
  }

  /** Listen for any status change and respond properly */
  private void operate() {
    // Do not interfere with the lift once it has decided
    // which floor to go and currently heading to that floor
    if (isMoving()) {
      return;
    }

    // Tell people who are waiting outside of the lift as soon as the lift stops
    // that if this lift is the right one for them because they will walk in only
    // when their heading direction is the same as the lift's
    if (needReverse) {
      needReverse = false;
      direction = direction == DIRECTION_UP ? DIRECTION_DOWN : DIRECTION_UP;
    }

    // Now, the corresponding request on this floor should be satisfied
    removeRequest(currentFloor, direction);

    // Wait for doors to close before making any decision
    if (!isDoorClosed()) {
      return;
    }

    // Now, figure out which floor to go
    if (direction == DIRECTION_UP)
      handleGoingUp();
    else if (direction == DIRECTION_DOWN)
      handleGoingDown();
    else
      handleLiftSuspend();
  }

  /** Find out the optimal floor to stop next while going up */
  private void handleGoingUp() {
    destinations.addAll(
        upRequests.stream()
                  .filter(r -> r > currentFloor)
                  .collect(Collectors.toSet()));
    if (destinations.size() > 0)
      model.startLiftAnimation(destinations.pollFirst());
    else
      handleLiftSuspend();
  }

  /** Find out the optimal floor to stop next while going down */
  private void handleGoingDown() {
    destinations.addAll(
        downRequests.stream()
                    .filter(r -> r < currentFloor)
                    .collect(Collectors.toSet()));
    if (destinations.size() > 0)
      model.startLiftAnimation(destinations.pollLast());
    else
      handleLiftSuspend();
  }

  /** Find out the optimal floor to stop next if uncertain */
  private void handleLiftSuspend() {
    // The direction need to be recalculated
    direction = NO_DIRECTION;

    // Combine all requests together
    TreeSet<Integer> allRequests = new TreeSet<>();
    allRequests.addAll(upRequests);
    allRequests.addAll(downRequests);

    // If no one is trying to use this lift, then staying idle
    if (allRequests.size() < 1) {
      return;
    }

    // First figure out which direction to go
    // new direction is determined by the relative position between
    // the current floor and the nearest floor that has at least one request
    int nearestFloor = allRequests.first();
    for (Integer requestFloor : allRequests) {
      if (Math.abs(currentFloor - requestFloor) <
          Math.abs(currentFloor - nearestFloor)) {
        nearestFloor = requestFloor;
      }
    }

    // Then figure out the exact floor to stop
    // Typical case: the lift is at 10th, ready to move downward and
    // there are requests like 8th UP, 6th UP, 4th UP at the same time,
    // the lift should go down to 4th, change direction, and go up to
    // satisfy these requests together instead of simply go down to 8th
    needReverse = false;
    if (currentFloor >= nearestFloor) {
      direction = DIRECTION_DOWN;

      if (upRequests.contains(nearestFloor) &&
          !downRequests.contains(nearestFloor)) {
        needReverse = true;
        model.startLiftAnimation(upRequests.first());
      } else {
        model.startLiftAnimation(nearestFloor);
      }
    } else {
      direction = DIRECTION_UP;

      if (downRequests.contains(nearestFloor) &&
          !upRequests.contains(nearestFloor)) {
        needReverse = true;
        model.startLiftAnimation(downRequests.last());
      } else {
        model.startLiftAnimation(nearestFloor);
      }
    }
  }

  /** Remind passengers that it maybe the time for them to leave */
  private void askAnyoneWantToGetOut() {
    synchronized (passengers) {
      Set<Person> leavers = passengers.stream()
                                      .filter(p -> p.getEndFloor() == currentFloor)
                                      .collect(Collectors.toSet());
      leavers.stream().forEach(p -> p.walkoutLift());
      passengers.removeAll(leavers);
    }
  }

  /** Add a person into lift's passenger list */
  public void addPassenger(Person newPassenger) {
    synchronized (passengers) {
      passengers.add(newPassenger);
      destinations.add(newPassenger.getEndFloor());
    }
  }

  /** Add a new request to the most appropriate lift's 'todo' list */
  public void addRequest(int requestFloor, int requestDirection, Set<Elevator> allSyncLifts) {
    if (!isSynchronized) {
      turnOnRequestButtonLight(id, requestFloor, requestDirection);
      addRequest(requestFloor, requestDirection);
      return;
    }
    // This request requires the synchronized lifts to cooperate
    // Turn their button lights on but only pick the 'best' one to serve
    config.getSyncLiftIds().forEach(id ->
        turnOnRequestButtonLight(id, requestFloor, requestDirection));
    Elevator bestLift = ElevatorScheduler.
        pickBestSyncLift(requestFloor, requestDirection, allSyncLifts);
    if (bestLift != null) {
      bestLift.addRequest(requestFloor, requestDirection);
    }
  }

  /** Add a request to the lift's 'todo' list */
  public void addRequest(int requestFloor, int requestDirection) {
    if (requestDirection == DIRECTION_UP)
      upRequests.add(requestFloor);
    else if (requestDirection == DIRECTION_DOWN)
      downRequests.add(requestFloor);
  }

  /** Remove a request from the lift's "todo" list */
  private void removeRequest(int requestFloor, int requestDirection) {
    turnOffRequestButtonLight(id, requestFloor, requestDirection);
    if (requestDirection == DIRECTION_UP)
      upRequests.remove(requestFloor);
    else if (requestDirection == DIRECTION_DOWN)
      downRequests.remove(requestFloor);

    // Turn off all associated lights if it is a sync lift
    if (isSynchronized) {
      config.getSyncLiftIds().forEach(id ->
          turnOffRequestButtonLight(id, requestFloor, requestDirection));
    }
  }

  private void turnOnRequestButtonLight(String liftId, int floor, int buttonDirection) {
    ElevatorButton button = findButton(liftId, floor, buttonDirection);
    if (button != null) {
      synchronized (button) {
        button.turnOn();
      }
    }
  }

  private void turnOffRequestButtonLight(String liftId, int floor, int buttonDirection) {
    ElevatorButton button = findButton(liftId, floor, buttonDirection);
    if (button != null) {
      synchronized (button) {
        button.turnOff();
      }
    }
  }

  private ElevatorButton findButton(String liftId, int floor, int buttonDirection) {
    Optional<ElevatorButton> optional =
        buttons.stream()
               .filter(b -> b.getElevatorId().equals(liftId))
               .filter(b -> b.getFloor() == floor)
               .filter(b -> b.getDirection() == buttonDirection)
               .findFirst();
    return optional.isPresent() ? optional.get() : null;
  }

  public String getLiftId() {
    return this.id;
  }

  public boolean isSynchronized() {
    return this.isSynchronized;
  }

  public int getCurrentFloor() {
    return this.currentFloor;
  }

  public int getDirection() {
    return this.direction;
  }

  public boolean isMoving() {
    return this.state == ElevatorState.MOVING;
  }

  /** Indicate if the doors are fully opened */
  public boolean isDoorOpened() {
    return this.state == ElevatorState.WAITING;
  }

  /** Indicate if the doors are fully closed */
  public boolean isDoorClosed() {
    // Doors are also closed while lift is moving, but we don't care here
    return this.state == ElevatorState.IDLE;
  }

  public int getNextStopFloor() {
    return this.model.getNextFloor();
  }

  public double getAbsoluteButtonRightX() {
    return getAbsoluteDoorCenterX() + ELEVATOR_WIDTH * SCREEN_WIDTH_SCALE;
  }

  public double getAbsoluteDoorCenterX() {
    return this.model.getCenterX() + this.getTranslateX();
  }

  public double getAbsoluteLiftLeftMostX() {
    return this.model.getLeftMostX() + this.getTranslateX();
  }

  /** Inner class representing a lift */
  private class ElevatorModel extends Pane {
    private Rectangle leftDoor;
    private Rectangle rightDoor;
    private Text txCurrentFloor;

    private int waiter;          // a waiting counter
    private int nextFloor;       // next floor to reach
    private double distance;     // distance between current and next floor
    private double floorTracker; // use to track the current floor in real time

    private Thread doorAnimation; // handle door's open/wait/close
    private Thread liftAnimation; // handle lift's movement

    public ElevatorModel() {
      Color color = Color.color(Math.random(), Math.random(), Math.random());

      leftDoor = new Rectangle(0, 0, ELEVATOR_WIDTH / 2, ELEVATOR_HEIGHT);
      rightDoor= new Rectangle(ELEVATOR_WIDTH / 2, 0, ELEVATOR_WIDTH / 2, ELEVATOR_HEIGHT);
      leftDoor.setStroke(Color.WHITESMOKE);
      rightDoor.setStroke(Color.WHITESMOKE);
      leftDoor.setFill(color);
      rightDoor.setFill(color);

      txCurrentFloor = new Text(-ELEVATOR_WIDTH / 4, -ELEVATOR_HEIGHT / 7, "" + currentFloor);
      txCurrentFloor.setStroke(color);
      txCurrentFloor.setFont(Font.font(18 * COMBINED_SCALE));
      getChildren().addAll(leftDoor, rightDoor, txCurrentFloor);
    }

    public void startDoorAnimation() {
      state = ElevatorState.OPENING;
      waiter = 0;

      doorAnimation = new Thread(() -> {
        try {
          while (true) {
            Platform.runLater(() -> dispatch());
            Thread.sleep((long) DOOR_TIME);
          }
        } catch (InterruptedException ex) {}
      });
      doorAnimation.start();
    }

    public void startLiftAnimation(int floorToReach) {
      state = ElevatorState.MOVING;
      calculateDistance(floorToReach);

      liftAnimation = new Thread(() -> {
        try {
          while (true) {
            Platform.runLater(() -> move());
            Thread.sleep((long) MOVE_TIME);
          }
        } catch (InterruptedException ex) {}
      });
      liftAnimation.start();
    }

    /** Dispatch different kinds of door actions */
    private void dispatch() {
      if (state == ElevatorState.OPENING)
        open();
      else if (state == ElevatorState.CLOSING)
        close();
      else if (state == ElevatorState.WAITING) {
        if (++waiter < DOOR_WAIT_TIME / DOOR_TIME)
          return;
        else
          state = ElevatorState.CLOSING;
      }
    }

    private void open() {
      if (leftDoor.getWidth() > DOOR_STEP) {
        leftDoor.setWidth(leftDoor.getWidth() - DOOR_STEP);
        rightDoor.setX(rightDoor.getX() + DOOR_STEP);
        rightDoor.setWidth(rightDoor.getWidth() - DOOR_STEP);
      } else {
        leftDoor.setWidth(0.0);
        rightDoor.setX(ELEVATOR_WIDTH);
        rightDoor.setWidth(0.0);
        state = ElevatorState.WAITING;
        askAnyoneWantToGetOut();
      }
    }

    private void close() {
      if (leftDoor.getWidth() < ELEVATOR_WIDTH / 2) {
        leftDoor.setWidth(leftDoor.getWidth() + DOOR_STEP);
        rightDoor.setX(rightDoor.getX() - DOOR_STEP);
        rightDoor.setWidth(rightDoor.getWidth() + DOOR_STEP);
      } else {
        leftDoor.setWidth(ELEVATOR_WIDTH / 2);
        rightDoor.setX(ELEVATOR_WIDTH / 2);
        rightDoor.setWidth(ELEVATOR_WIDTH / 2);
        // Passengers are not visible when doors are closed
        passengers.stream().forEach(p -> p.setVisible(false));
        distributePassengersEvenly();
        state = ElevatorState.IDLE;
        doorAnimation.interrupt();
      }
    }

    private void move() {
      distance     -= MOVE_STEP;
      floorTracker += MOVE_STEP;

      // Recount the tracker after reaching each floor
      if (Math.abs(floorTracker - FLOOR_HEIGHT) < MOVE_STEP) {
        floorTracker = 0;
      }

      if (distance < 0) {           // we are already there
        txCurrentFloor.setText(""); // hide the floor indicator
        currentFloor = nextFloor;   // destination has reached
        passengers.stream().forEach(p -> p.setVisible(true)); // passengers become visible
        startDoorAnimation();       // start the door animation
        liftAnimation.interrupt();  // end the lifting animation
      } else {
        if (direction == DIRECTION_UP) {
          leftDoor.setY(leftDoor.getY() - MOVE_STEP);
          rightDoor.setY(rightDoor.getY() - MOVE_STEP);
          txCurrentFloor.setY(txCurrentFloor.getY() - MOVE_STEP);
          passengers.stream().forEach(p -> p.moveWithLift(-MOVE_STEP));
          if (floorTracker < MOVE_STEP) {
            currentFloor += (currentFloor == -1) ? 2 : 1;
          }
        } else if (direction == DIRECTION_DOWN) {
          leftDoor.setY(leftDoor.getY() + MOVE_STEP);
          rightDoor.setY(rightDoor.getY() + MOVE_STEP);
          txCurrentFloor.setY(txCurrentFloor.getY() + MOVE_STEP);
          passengers.stream().forEach(p -> p.moveWithLift(MOVE_STEP));
          if (floorTracker < MOVE_STEP) {
            currentFloor -= (currentFloor == 1) ? 2 : 1;
          }
        }
        txCurrentFloor.setText("" + currentFloor);
      }
    }

    private void calculateDistance(int floorToReach) {
      nextFloor = floorToReach;
      distance = 0.0f;
      floorTracker = 0.0f;

      switch (direction) {
        case DIRECTION_UP:
          distance = ((FLOOR_HEIGHT + SCREEN_HEIGHT_SCALE) *
              Math.abs((nextFloor > 0) ?
                       (nextFloor - ((currentFloor > 0) ? currentFloor : (currentFloor+1))) :
                       (nextFloor - currentFloor)));
          break;
        case DIRECTION_DOWN:
          distance = ((FLOOR_HEIGHT + SCREEN_HEIGHT_SCALE) *
              Math.abs((nextFloor > 0) ?
                       (nextFloor - currentFloor) :
                       (nextFloor - ((currentFloor > 0) ? (currentFloor-1) : currentFloor))));
          break;
        default:
          break;
      }
    }

    /** Adjust passengers' horizontal positions properly */
    private void distributePassengersEvenly() {
      int size = passengers.size();
      if (size == 1) {
        Person p = passengers.stream().findFirst().get();
        double adjustment = getAbsoluteDoorCenterX() - p.getAbsoluteHeadCenterX();
        p.adjustPositionInLift(adjustment);

      } else if (size > 1) {
        int count = 0;
        double basePosition = getAbsoluteLiftLeftMostX() + PERSON_WIDTH / 2;
        double headCenterGap = (ELEVATOR_WIDTH - PERSON_WIDTH) / (size - 1);

        for (Person p : passengers) {
          double targetPosition = basePosition + headCenterGap * count++;
          double adjustment = targetPosition - p.getAbsoluteHeadCenterX();
          p.adjustPositionInLift(adjustment);
        }
      }
    }

    public int getNextFloor() {
      return this.nextFloor;
    }

    public double getCenterX() {
      return (leftDoor.getX() + rightDoor.getX() + rightDoor.getWidth()) / 2;
    }

    public double getLeftMostX() {
      return leftDoor.getX();
    }
  }
}
