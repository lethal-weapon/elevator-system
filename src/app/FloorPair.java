package app;

//import static constants.BuildingConstant.*;

/**
 * Generate a pair of floor for a new person to use.
 *
 * People behave differently in various building settings.
 * This class aims to provide random but appropriate data.
 */
public class FloorPair {

  private static int floors;
  private static int undergrounds;
  private static int start;
  private static int end;
  private static int percent; // 0 - 99

  public static int[] getPair(BuildingConfig config) {
    floors = config.getFloors();
    undergrounds = config.getUndergrounds();
    percent = (int) (Math.random() * 100);
    return getResidencePair();
  }

  private static int[] getResidencePair() {
    start = 1;
    end = 1;

    if (percent < 50) { // 50% start from ground
      end = higherThanGround();
      if (percent < 5 && undergrounds > 0) // 5% down to carpark
        end = -(int) (1 + Math.random() * undergrounds);

    } else { // 50% going down
      start = higherThanGround();
      if (percent > 94) // 5% not going to ground
        while (end == 1 || start == end)
          end = getRandomFloor();
    }
    return new int[] {start, end};
  }

  private static int higherThanGround() {
    int floor = 0;
    while (floor < 2)
      floor = getRandomFloor();
    return floor;
  }

  private static int getRandomFloor() {
    int random = 0;
    while (random == 0) // there is no floor zero
      random = (int) (Math.random() * (floors + 1)) - undergrounds;
    return random;
  }

  public static int getRandomFloor(BuildingConfig config) {
    int floor = 0;
    while (floor == 0)
      floor = (int) (Math.random() * (config.getFloors() + 1)) - config.getUndergrounds();
    return floor;
  }

//  public static void main(String[] args) {
//    for (int i = MIN_FLOORS; i <= MAX_FLOORS; i++)
//      for (int j = MIN_UNDERGROUNDS; j <= (i-2) && j <= MAX_UNDERGROUNDS; j++)
//        test(new BuildingConfig(i, j, 1, 2));
//    System.out.println("Done !");
//  }

//  private static void test(BuildingConfig config) {
//    int floors = config.getFloors();
//    int undergrounds = config.getUndergrounds();
//    int count = 0;
//    int error = 0;
//    int fromGround = 0;
//    int toGround   = 0;
//    int notGround  = 0;
//
//    while (++count <= 1_000_000) {
//      int[] se = getPair(config);
//
//      if (se[0] == 1)
//        ++fromGround;
//      if (se[1] == 1)
//        ++toGround;
//      if (se[0] != 1 && se[1] != 1)
//        ++notGround;
//
//      if (se[0] == 0 || se[1] == 0 ||
//         (se[0] < (-undergrounds)) ||
//         (se[0] > (floors-undergrounds)) ||
//         (se[1] < (-undergrounds)) ||
//         (se[1] > (floors-undergrounds)) ||
//         (se[0] == se[1])) {
//        ++error;
//      }
//    }
//
//    System.out.println("Floors: " + floors + ", Undergrounds: " +
//            undergrounds + "\t" + ((error == 0)? "PASS" : "ERROR!!"));
//
//    double FGP = 100 * (fromGround / ((double)count));  // EST.50
//    double TGP = 100 * (toGround / ((double)count));    // EST.45
//    double NGP = 100 * (notGround / ((double)count));   // EST.5
//
//    if (FGP < 48 || FGP > 52 || TGP < 43 || TGP > 47 || NGP < 3 || NGP > 7) {
//        System.out.println("From G: " + (int)FGP + "%");
//        System.out.println("To   G: " + (int)TGP + "%");
//        System.out.println("Not  G: " + (int)NGP + "%");
//        //System.out.println("Error : " + error);
//    }
//  }
}
