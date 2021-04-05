package app;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Building configuration.
 *
 * Different scenarios associate with different configurations.
 */
public class BuildingConfig {
  public static final int SCENARIO_RESIDENCE_I  = 1;
  public static final int SCENARIO_RESIDENCE_II = 2;
  public static final int SCENARIO_COMMERCIAL   = 3;
  public static final int SCENARIO_ACADEMIC     = 4;

  private final int floors;       // floors in total
  private final int undergrounds; // floors in underground
  private final int syncLifts;    // number of sync lifts
  private final int asyncLifts;   // number of async lifts
  private List<String> liftIds;

  private BuildingConfig(int floors,
                         int undergrounds,
                         int syncLifts,
                         int asyncLifts) {
    this.floors       = floors;
    this.undergrounds = undergrounds;
    this.syncLifts    = syncLifts;
    this.asyncLifts   = asyncLifts;
    this.liftIds      = new ArrayList<>();

    for (int i = 0; i < syncLifts; i++)
      this.liftIds.add("SYNC#" + (i + 1));
    for (int j = 0; j < asyncLifts; j++)
      this.liftIds.add("ASYNC#" + (j + 1));
  }

  public static BuildingConfig newConfig(int scenario) {
    switch(scenario) {
      case SCENARIO_RESIDENCE_I:
        return new BuildingConfig(10, 2, 2, 1);
      case SCENARIO_RESIDENCE_II:
        return new BuildingConfig(10, 2, 2, 1);
      case SCENARIO_COMMERCIAL:
        return new BuildingConfig(10, 2, 2, 1);
      case SCENARIO_ACADEMIC:
        return new BuildingConfig(10, 2, 2, 1);
      default:
        return new BuildingConfig(10, 2, 2, 1);
    }
  }

  public int getFloors() {
    return this.floors;
  }

  public int getUndergrounds() {
    return this.undergrounds;
  }

  public int getLiftCount() {
    return this.syncLifts + this.asyncLifts;
  }

  public List<String> getLiftIds() {
    return this.liftIds;
  }

  public List<String> getSyncLiftIds() {
    return this.liftIds.stream()
                       .filter(id -> id.startsWith("SYNC"))
                       .sorted()
                       .collect(Collectors.toList());
  }

  public List<String> getAsyncLiftIds() {
    return this.liftIds.stream()
                       .filter(id -> id.startsWith("ASYNC"))
                       .sorted()
                       .collect(Collectors.toList());
  }
}
