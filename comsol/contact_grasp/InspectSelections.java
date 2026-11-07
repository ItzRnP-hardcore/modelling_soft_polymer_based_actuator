import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

public class InspectSelections {
    public static void main(String[] args) {
        try {
            String mphFile = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp\\loop_caging_optimization\\actuator_LoopCaging_R8mm_Pos1_Xc48_Yc-42_solved.mph";
            System.out.println("Loading: " + mphFile);
            Model model = ModelUtil.load("Model", mphFile);

            // Check contact pressure at all pressure steps
            model.result().numerical().create("max_cnt_p", "MaxSurface");
            model.result().numerical("max_cnt_p").set("data", "dset1");
            model.result().numerical("max_cnt_p").selection().all();
            model.result().numerical("max_cnt_p").set("expr", new String[]{"solid.cnt1.Tn"});
            double[][] maxTn = model.result().numerical("max_cnt_p").getReal();
            System.out.println("solid.cnt1.Tn max over all surfaces: " + Arrays.toString(maxTn[0]));

            // Cylinder position
            double[][] cylPos = model.result().numerical("pev_cyl").getReal();
            System.out.println("Cylinder disp u: " + Arrays.toString(cylPos[0]));
            System.out.println("Cylinder disp v: " + Arrays.toString(cylPos[1]));

            ModelUtil.remove("Model");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
