package app;

import static app.Main.*;

import static constants.BuildingConstant.*;
import static constants.CommonConstant.*;
import static constants.ElevatorConstant.*;
import static constants.PersonConstant.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class Building extends StackPane {

  private BuildingConfig config;
  private List<Elevator> elevators;
  private List<ElevatorButton> buttons;
  private Set<Person> persons;

  private int peopleCount;
  private Thread trafficController;
  private BuildingModel model;

  public Building(BuildingConfig config) {
    this.config      = config;
    this.elevators   = new ArrayList<>();
    this.buttons     = new ArrayList<>();
    this.persons     = new HashSet<>();
    this.peopleCount = 0;
    this.initButtons();

    for (String id : config.getSyncLiftIds())
      this.elevators.add(new Elevator(id, true, config, buttons));
    for (String id : config.getAsyncLiftIds())
      this.elevators.add(new Elevator(id, false, config, buttons));

    this.model = new BuildingModel();
    this.getChildren().add(model);
    this.startController();
  }

  /** Initialize elevator buttons */
  private void initButtons() {
    int floors = config.getFloors();
    int undergrounds = config.getUndergrounds();

    for (String elevatorId : config.getLiftIds()) {
      for (int j = 0; j < floors; j++) {
        int floor = floors - undergrounds - j;

        ElevatorButton btnUp =
            ElevatorButton.createUpButton(elevatorId, floor > 0 ? floor : floor - 1);
        ElevatorButton btnDown =
            ElevatorButton.createDownButton(elevatorId, floor > 0 ? floor : floor - 1);

        if (j == 0) // highest floor does not have up button
          btnUp.setVisible(false);
        else if (j == floors - 1) // lowest floor does not have down button
          btnDown.setVisible(false);

        buttons.addAll(Arrays.asList(btnUp, btnDown));
      }
    }
  }

  /** Define and start the animation */
  private void startController() {
    this.trafficController = new Thread(() -> {
      try {
        while (true) {
          Platform.runLater(() -> handleTraffic());
          Thread.sleep(CONTROLLER_INTERVAL);
        }
      } catch (InterruptedException ex) {}
    });
    this.trafficController.start();
  }

  /** Define when is the suitable time to create new people */
  private boolean isTrafficClear() {
    // Introduce some randomness to pervert people from showing up too quickly
    return (persons.size() <= elevators.size() * config.getFloors() / 2) &&
           ((int)(Math.random() * 100)) % 5 == 0;
  }

  /** Remove/Add people from/to the system */
  private void handleTraffic() {
    // Remove anyone who has completed his life cycle
    Set<Person> leavers = persons.stream()
                                 .filter(p -> p.isLifeCycleCompleted())
                                 .collect(Collectors.toSet());
    model.removePeopleFromScreen(leavers);
    persons.removeAll(leavers);

    // Add new people only if the traffic isn't busy
    if (isTrafficClear()) {
      int[] pair = FloorPair.getPair(config);
      Person newPerson =
          new Person(++peopleCount % 5 == 0 ? "#" + peopleCount : "", pair[0], pair[1], elevators);
      persons.add(newPerson);
      model.addNewPersonToScreen(newPerson);
    }
  }

  /** Return a pair of elevator buttons which belong to
   *  the same lift and the same floor, "UP" button first */
  private List<ElevatorButton> findButtonPair(String elevatorId, int floor) {
    return buttons.stream()
                  .filter(b -> b.getElevatorId().equals(elevatorId))
                  .filter(b -> b.getFloor() == floor)
                  .sorted((btn1, btn2) -> btn2.getDirection() == DIRECTION_UP ? 1 : -1)
                  .collect(Collectors.toList());
  }

  /** Inner class representing a building */
  private class BuildingModel extends Pane {
    private int floors = config.getFloors();
    private int undergrounds = config.getUndergrounds();

    private double floorWidth = elevators.size() * (ELEVATOR_WIDTH + ELEVATOR_GAP) + ELEVATOR_GAP;
    private double startX = (SCREEN_WIDTH - floorWidth) / 2;
    private double startY = (SCREEN_HEIGHT - floors * FLOOR_HEIGHT) / 2;

    public BuildingModel() {
      drawFloor();
      drawFloorSigns();
      drawElevatorWells();
      drawButtons();
      drawElevators();
    }

    /** Justify the coordinates of a piece of UI and add it to the screen */
    private void addToModel(Pane UI) {
      UI.setTranslateX(startX);
      UI.setTranslateY(startY);
      getChildren().add(UI);
    }

    private void drawFloor() {
      VBox box  = new VBox(FLOOR_HEIGHT);
      box.setAlignment(Pos.CENTER);

      for (int i = 0; i < floors + 1; i++) {
        Line line = new Line(0, 0, floorWidth, 0);
        line.setStroke(
            (i == floors - undergrounds) ? Color.RED : Color.WHITESMOKE);
        box.getChildren().add(line);
      }
      this.addToModel(box);
    }

    private void drawFloorSigns() {
      VBox box  = new VBox(FLOOR_HEIGHT * 0.56);
      box.setAlignment(Pos.CENTER);

      for (int i = 0; i < floors; i++) {
        int floor = floors - undergrounds - i;
        Text txFloor = new Text(0, 0, floor + "F");

        if (floor == 1)
          txFloor.setText("G");
        else if (floor < 1)
          txFloor.setText((Math.abs(floor) + 1) + "UG");

        txFloor.setFont(Font.font(20 * COMBINED_SCALE));
        txFloor.setStroke(Color.WHITESMOKE);
        box.getChildren().add(txFloor);
      }
      this.addToModel(box);
    }

    private void drawElevatorWells() {
      HBox box = new HBox(ELEVATOR_GAP);
      box.setAlignment(Pos.CENTER);
      box.setPadding(new Insets(0, 0, 0, ELEVATOR_GAP));
      double buildingHeight = floors * (FLOOR_HEIGHT + 1.00);

      for (int i = 0; i < elevators.size(); i++) {
        HBox hbox = new HBox(ELEVATOR_WIDTH);
        Line line1 = new Line(0, 0, 0, buildingHeight);
        Line line2 = new Line(0, 0, 0, buildingHeight);
        line1.setStroke(Color.WHITESMOKE);
        line2.setStroke(Color.WHITESMOKE);
        line1.getStrokeDashArray().addAll(10.0, 10.0);
        line2.getStrokeDashArray().addAll(10.0, 10.0);
        hbox.getChildren().addAll(line1, line2);
        box.getChildren().add(hbox);
      }
      this.addToModel(box);
    }

    private void drawButtons() {
      HBox box = new HBox(ELEVATOR_GAP + ELEVATOR_WIDTH * 0.80);
      box.setPadding(new Insets(0, 0, 0, ELEVATOR_GAP + ELEVATOR_WIDTH * 1.20));
      box.setAlignment(Pos.CENTER);

      for (int i = 0; i < elevators.size(); i++) {
        VBox vbox = new VBox(FLOOR_HEIGHT * 0.38);
        vbox.setAlignment(Pos.CENTER);
        String elevatorId = elevators.get(i).getLiftId();

        for (int j = 0; j < floors; j++) {
          VBox vbox2  = new VBox(FLOOR_HEIGHT / 12);
          vbox2.setPadding(new Insets(Math.abs(floors / 2 - j) * SCREEN_HEIGHT_SCALE, 0, 0, 0));
          vbox2.setAlignment(Pos.CENTER);
          int floor = floors - undergrounds - j;

          vbox2.getChildren().addAll(findButtonPair(elevatorId, floor > 0 ? floor : floor - 1));
          vbox.getChildren().add(vbox2);
        }
        box.getChildren().add(vbox);
      }
      this.addToModel(box);
    }

    private void drawElevators() {
      int top = floors - undergrounds;

      for (int i = 1; i <= elevators.size(); i++) {
        Elevator lift = elevators.get(i-1);
        int floor = lift.getCurrentFloor();
        int diff = top - floor;

        double offsetX = i * (ELEVATOR_GAP + 1.00);
        offsetX += i > 1 ? (i - 1) * (ELEVATOR_WIDTH + SCREEN_WIDTH_SCALE) : 0;

        double offsetY = 0.00f;
        if (floor > 0 && floor < top)
          offsetY += getAdjustedFloorHeight(diff) * diff;
        else if (floor < 0)
          offsetY += getAdjustedFloorHeight(diff) * (diff - 1);

        lift.setTranslateX(startX + offsetX);
        lift.setTranslateY(startY + offsetY);
        getChildren().add(lift);
      }
    }

    /** The further an object away from the top floor, the larger its y offset */
    private double getAdjustedFloorHeight(int diff) {
      return FLOOR_HEIGHT + 0.15 * diff;
    }

    public void addNewPersonToScreen(Person person) {
      int side = person.getAppearSide();
      int floor = person.getStartFloor();
      int top = floors - undergrounds;
      int diff = top - floor;

      double offsetX = side == SIDE_LEFT ? 0 : floorWidth;
      double offsetY = FLOOR_HEIGHT - (PERSON_HEIGHT - HEAD_RADIUS - 1.00);

      if (floor > 0 && floor < top)
        offsetY += getAdjustedFloorHeight(diff) * diff;
      else if (floor < 0)
        offsetY += getAdjustedFloorHeight(diff) * (diff - 1);

      person.setTranslateX(startX + offsetX);
      person.setTranslateY(startY + offsetY);
      getChildren().add(person);
    }

    public void removePeopleFromScreen(Set<Person> people) {
      getChildren().removeAll(people);
    }
  }
}
