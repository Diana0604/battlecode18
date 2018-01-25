import bc.Direction;

/**
 * Created by Pau on 23/01/2018.
 */
public class Const {
    final static int INF = 1000000000;
    final static long INFL = 1000000000;

    static final Direction[] allDirs = {Direction.North, Direction.Northeast, Direction.East, Direction.Southeast, Direction.South, Direction.Southwest, Direction.West, Direction.Northwest, Direction.Center};

    static final int maxMapSize = 50;

    final static int[] healingPowers = {10, 12, 17, 17};
    final static int[] buildingPowers = {5, 5, 6, 7, 10};
    final static int[] repairingPowers = {10, 10, 11, 12, 15};
    final static int[] harvestingPowers = {3, 4, 4, 4, 4};
    final static int[] mageDamages = {60, 75, 90, 105, 105};
    final static int[] rocketCapacities = {8, 8, 8, 12};

    final static int rangerDamage = 30;
    final static int knightDamage = 80;

    final static int rangerAttackCooldown = 2;
    final static int knightAttackCooldown = 2;
    final static int mageAttackCooldown = 2;

    final static int rangerMinAttackRange = 10;
    final static int rangerAttackRange = 50;
    final static int knightAttackRange = 2;
    final static int mageAttackRange = 30;

    final static int rangerSafeAttackRange = 50;
    final static int knightSafeAttackRange = 8;
    final static int mageSafeAttackRange = 30;

    final static int rangerLongAttackRange = 68;
    final static int knightLongAttackRange = 30;
    final static int mageLongAttackRange = 65;

    final static int rangerExtraAttackRange = 68;
    final static int knightExtraAttackRange = 8;
    final static int mageExtraAttackRange = 65;

    final static int workerCost = 25;
    final static int knightCost = 20;
    final static int rangerCost = 20;
    final static int mageCost   = 20;
    final static int healerCost = 20;
    final static int factoryCost= 100;
    final static int rocketCost = 75;
    final static int replicateCost = 30;

    final static int workerMaxHealth = 100;
    final static int knightMaxHealth = 250;
    final static int rangerMaxHealth = 200;
    final static int mageMaxHealth   = 80;
    final static int healerMaxHealth = 100;
    final static int factoryMaxHealth= 300;
    final static int rocketMaxHealth = 200;
}
