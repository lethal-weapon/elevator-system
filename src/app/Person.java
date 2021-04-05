package app;

import static app.Main.*;

import static constants.CommonConstant.*;
import static constants.PersonConstant.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class Person extends StackPane {

  private final int startFloor;
  private final int endFloor;
  private final int appearSide;

  private PersonState state;
  private List<Elevator> lifts;          // all lifts
  private List<Elevator> requestedLifts; // lifts this person is currently waiting
  private Elevator chosenLift;           // the lift this person chooses in the end
  private PersonModel model;

  public Person(String name,
                int startFloor,
                int endFloor,
                List<Elevator> lifts) {

    this.startFloor = startFloor;
    this.endFloor = endFloor;
    this.appearSide = getRandomAppearSide();

    this.state = PersonState.ENTER;
    this.lifts = lifts;
    this.requestedLifts = new ArrayList<>();
    this.chosenLift = null;

    this.model = new PersonModel(name);
    this.getChildren().add(model);
    this.model.startMoveAnimation();
  }

  private int getRandomAppearSide() {
    return ((int)(Math.random() * 2) == 0) ? SIDE_LEFT : SIDE_RIGHT;
  }

  public int getStartFloor() {
    return this.startFloor;
  }

  public int getEndFloor() {
    return this.endFloor;
  }

  public int getAppearSide() {
    return this.appearSide;
  }

  /** Return this person object */
  public Person getThisPerson() {
    return this;
  }

  /** Return this person's heading direction */
  public int getDirection() {
    return startFloor >= endFloor ? DIRECTION_DOWN : DIRECTION_UP;
  }

  /** Indicate if this person is done with the system */
  public boolean isLifeCycleCompleted() {
    return this.state == PersonState.EXIT;
  }

  /** Move a small distance inside the lift */
  public void adjustPositionInLift(double adjustment) {
    this.model.moveH(adjustment);
  }

  /** Move as much as the containing lift moves */
  public void moveWithLift(double liftStep) {
    this.model.moveV(liftStep);
  }

  /** Exit the containing lift */
  public void walkoutLift() {
    this.state = PersonState.WALK_OUT;
    this.model.startMoveAnimation();
  }

  /** Return all synchronized lifts in a set */
  private Set<Elevator> getAllSyncLifts() {
    return lifts.stream()
                .filter(l -> l.isSynchronized())
                .collect(Collectors.toSet());
  }

  public double getAbsoluteHeadCenterX() {
    return this.model.getHeadCenterX() + this.getTranslateX();
  }

  /** Inner class representing a person */
  private class PersonModel extends Pane {
    private Circle head;
    private Line body;
    private Line leftArm;
    private Line leftLeg;
    private Line rightArm;
    private Line rightLeg;
    private Text txName;

    private Thread moveAnimation;
    private Thread waitAnimation;

    public PersonModel(String name) {
      head = new Circle(0, 0, HEAD_RADIUS);
      body = new Line(head.getCenterX(), head.getCenterY() + head.getRadius(),
                      head.getCenterX(), head.getCenterY() + head.getRadius() + BODY_LENGTH);

      double bodyStartX = body.getStartX();
      double bodyStartY = body.getStartY();
      double bodyEndX   = body.getEndX();
      double bodyEndY   = body.getEndY();

      leftArm = new Line(bodyStartX, (bodyStartY + bodyEndY) / 2,
                         bodyStartX - ARM_LENGTH * Math.cos(ARM_ANGLE),
                         (bodyStartY + bodyEndY) / 2 - ARM_LENGTH * Math.sin(ARM_ANGLE));

      leftLeg = new Line(bodyEndX, bodyEndY,
                         bodyEndX - LEG_LENGTH * Math.sin(LEG_ANGLE),
                         bodyEndY + LEG_LENGTH * Math.cos(LEG_ANGLE));

      rightArm = new Line(bodyStartX, (bodyStartY + bodyEndY) / 2,
                          bodyStartX + ARM_LENGTH * Math.cos(ARM_ANGLE),
                          (bodyStartY + bodyEndY) / 2 - ARM_LENGTH * Math.sin(ARM_ANGLE));

      rightLeg = new Line(bodyEndX, bodyEndY,
                          bodyEndX + LEG_LENGTH * Math.sin(LEG_ANGLE),
                          bodyEndY + LEG_LENGTH * Math.cos(LEG_ANGLE));

      txName = new Text(bodyStartX + 12 * SCREEN_WIDTH_SCALE, bodyStartY, name);

      head.setFill(Color.BLACK);
      head.setStroke(Color.WHITESMOKE);
      body.setStroke(Color.WHITESMOKE);
      leftArm.setStroke(Color.WHITESMOKE);
      leftLeg.setStroke(Color.WHITESMOKE);
      rightArm.setStroke(Color.WHITESMOKE);
      rightLeg.setStroke(Color.WHITESMOKE);
      txName.setStroke(Color.WHITESMOKE);
      txName.setFont(Font.font(12 * COMBINED_SCALE));

      getChildren().addAll(head, body, leftArm, leftLeg, rightArm, rightLeg, txName);
    }

    public void startMoveAnimation() {
      moveAnimation = new Thread(() -> {
        try {
          while (true) {
            Platform.runLater(() -> dispatch());
            Thread.sleep((long) MOVE_TIME);
          }
        } catch (InterruptedException ex) {}
      });
      moveAnimation.start();
    }

    public void startWaitAnimation() {
      waitAnimation = new Thread(() -> {
        try {
          while (true) {
            Platform.runLater(() -> doWait());
            Thread.sleep(WAIT_INTERVAL);
          }
        } catch (InterruptedException ex) {}
      });
      waitAnimation.start();
    }

    /** Dispatch different kinds of moving actions */
    private void dispatch() {
      if (state == PersonState.ENTER)
        enter();
      else if (state == PersonState.WALK_IN)
        walkin();
      else if (state == PersonState.WALK_OUT)
        walkout();
    }

    /** Enter the screen while pressing each button */
    private void enter() {
      moveH(appearSide == SIDE_LEFT ? MOVE_STEP : -MOVE_STEP);

      // Request all available lifts
      for (Elevator lift : lifts) {
        if (!requestedLifts.contains(lift)) {
          boolean isCloseEnough =
              Math.abs(lift.getAbsoluteButtonRightX() -
                            getAbsoluteHeadCenterX()) < head.getRadius();

          if (isCloseEnough) {
            // Press one button is sufficient for synchronized lifts
            requestedLifts.addAll(
                lift.isSynchronized() ? getAllSyncLifts() : Arrays.asList(lift));
            lift.addRequest(startFloor, getDirection(), getAllSyncLifts());
          }
        }
      }
      // Enter the waiting phase when all lifts have been requested
      if (lifts.size() == requestedLifts.size()) {
        state = PersonState.WAITING;
        startWaitAnimation();
        moveAnimation.interrupt();
      }
    }

    /** Check lift status while waiting, pick one in the end */
    private void doWait() {
      boolean keepWaiting = true;

      for (Elevator lift : requestedLifts) {
        if (lift.isDoorOpened() &&
            lift.getCurrentFloor() == startFloor &&
            lift.getDirection() == getDirection()) {

          chosenLift = lift;
          chosenLift.addPassenger(getThisPerson());
          keepWaiting = false;
          break;
        }
      }
      if (!keepWaiting) {
        state = PersonState.WALK_IN;
        startMoveAnimation();
        waitAnimation.interrupt();
      }
    }

    /** Walk in the chosen lift */
    private void walkin() {
      double doorCenterX = chosenLift.getAbsoluteDoorCenterX();
      double headCenterX = getAbsoluteHeadCenterX();
      double diff = headCenterX - doorCenterX ;
      moveH(diff < 0 ? MOVE_STEP : -MOVE_STEP);

      if (Math.abs(diff) < head.getRadius() / 2) {
        state = PersonState.MOVE_WITH_LIFT;
        moveAnimation.interrupt();
      }
    }

    /** Walk out the chosen lift while fading out the screen */
    private void walkout() {
      moveH(appearSide == SIDE_LEFT ? -MOVE_STEP : MOVE_STEP);
      this.setOpacity(this.getOpacity() * 0.95);
      if (this.getOpacity() < 0.1) {
        state = PersonState.EXIT;
        moveAnimation.interrupt();
      }
    }

    /** Move horizontally, step > 0 ? Right : Left */
    public void moveH(double step) {
      head.setCenterX(head.getCenterX() + step);
      body.setTranslateX(body.getTranslateX() + step);
      leftArm.setTranslateX(leftArm.getTranslateX() + step);
      leftLeg.setTranslateX(leftLeg.getTranslateX() + step);
      rightArm.setTranslateX(rightArm.getTranslateX() + step);
      rightLeg.setTranslateX(rightLeg.getTranslateX() + step);
      txName.setTranslateX(txName.getTranslateX() + step);
    }

    /** Move vertically, step > 0 ? Down : Up */
    public void moveV(double step) {
      head.setCenterY(head.getCenterY() + step);
      body.setTranslateY(body.getTranslateY() + step);
      leftArm.setTranslateY(leftArm.getTranslateY() + step);
      leftLeg.setTranslateY(leftLeg.getTranslateY() + step);
      rightArm.setTranslateY(rightArm.getTranslateY() + step);
      rightLeg.setTranslateY(rightLeg.getTranslateY() + step);
      txName.setTranslateY(txName.getTranslateY() + step);
    }

    public double getHeadCenterX() {
      return this.head.getCenterX();
    }
  }
}
