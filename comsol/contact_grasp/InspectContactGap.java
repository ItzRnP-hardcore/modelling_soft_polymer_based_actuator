import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

public class InspectContactGap {
    public static void main(String[] args) {
        try {
            String mphFile = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp\\loop_caging_optimization\\actuator_LoopCaging_R8mm_Pos1_Xc48_Yc-42_solved.mph";
            System.out.println("Loading: " + mphFile);
            Model model = ModelUtil.load("Model", mphFile);

            model.result().numerical().create("min_gap", "MinSurface");
            model.result().numerical("min_gap").set("data", "dset1");
            model.result().numerical("min_gap").selection().all();
            model.result().numerical("min_gap").set("expr", new String[]{"solid.cnt1.gap"});
            model.result().numerical("min_gap").set("unit", new String[]{"mm"});

            double[][] gaps = model.result().numerical("min_gap").getReal();
            String[] pList = {"0", "20", "40", "60", "80", "100"};
            System.out.println("Minimum Contact Gap (solid.cnt1.gap) across pressure steps:");
            for (int s = 0; s < pList.length; s++) {
                System.out.printf(Locale.US, "P = %s kPa -> Min Gap = %.4f mm%n", pList[s], gaps[0][s]);
            }

            ModelUtil.remove("Model");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

