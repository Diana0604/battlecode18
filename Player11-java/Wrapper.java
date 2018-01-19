import java.util.ArrayList;
import bc.*;
/**
 * Created by Ivan on 1/18/2018.
 */
public class Wrapper {
    static AuxUnit[] senseUnits(int x, int y, int r, boolean myTeam){
        ArrayList<AuxUnit> ans = new ArrayList<>();
        for (int i = 0; i < Vision.Mx[r].length; ++i){
            AuxUnit unit = Data.getUnit(x+Vision.Mx[r][i], y + Vision.Mx[r][i], myTeam);
            if (unit != null) ans.add(unit);
        }
        return (AuxUnit[])ans.toArray();
    }
}
