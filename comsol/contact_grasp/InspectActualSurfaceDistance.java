import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

public class InspectActualSurfaceDistance {
    public static void main(String[] args) {
        try {
            String mphFile = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp\\loop_caging_optimization\\actuator_LoopCaging_R8mm_Pos1_Xc48_Yc-42_solved.mph";
            System.out.println("Loading: " + mphFile);
            Model model = ModelUtil.load("Model", mphFile);

            // Correct SI units: x and u are in meters! 48 mm = 0.048 m, -42 mm = -0.042 m
            model.result().numerical().create("min_dist_axis", "MinSurface");
            model.result().numerical("min_dist_axis").set("data", "dset1");
            model.result().numerical("min_dist_axis").selection().all();
            model.result().numerical("min_dist_axis").set("expr", new String[]{"sqrt((x+u - 0.048)^2 + (y+v - (-0.042))^2)*1000"});

            double[][] minD = model.result().numerical("min_dist_axis").getReal();
            System.out.println("Min distance from actuator surface to cylinder axis across pressure steps:");
            String[] pList = {"0", "20", "40", "60", "80", "100"};
            for (int s = 0; s < pList.length; s++) {
                double dAxis = minD[0][s];
                double gapToCyl = dAxis - 8.0; // R_cyl = 8.0 mm
                System.out.printf(Locale.US, "P = %s kPa -> Min Dist to Axis = %.3f mm | Gap to Cyl Surface (R=8mm) = %+.3f mm%n",
                                  pList[s], dAxis, gapToCyl);
            }

            ModelUtil.remove("Model");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
