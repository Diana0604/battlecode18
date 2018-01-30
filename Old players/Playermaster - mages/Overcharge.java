import java.util.ArrayList;

import bc.UnitType;

/**
 * Created by Ivan on 1/23/2018.
 */
public class Overcharge {


    static boolean getOvercharged(AuxUnit unit){
        try {
            if (!Units.attack()) return false;
            AuxUnit[] healers = Wrapper.senseUnits(unit.getMapLocation(), 30, true);
            AuxUnit bestHealer = null;
            for (int i = 0; i < healers.length; ++i) {
                if (healers[i].getType() != UnitType.Healer) continue;
                if (healers[i].isInSpace()) continue;
                if (healers[i].isInGarrison()) continue;
                if (!healers[i].canUseAbility()) continue;
                if (bestHealer == null || unit.target == null || unit.target.distanceBFSTo(bestHealer.getMapLocation()) < unit.target.distanceBFSTo(healers[i].getMapLocation())) {
                    bestHealer = healers[i];
                }
            }
            if (bestHealer != null) {
                Wrapper.overcharge(bestHealer, unit);
                return true;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
