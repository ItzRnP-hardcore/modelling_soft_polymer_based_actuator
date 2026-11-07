import com.comsol.model.*;
import com.comsol.model.util.*;

public class PrintCoords {
    public static void main(String[] args) {
        try {
            Model model = ModelUtil.load("Model", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp\\loop_caging_optimization\\actuator_LoopCaging_R8mm_Pos1_Xc48_Yc-42_solved.mph");
            model.result().numerical().create("pev_test", "EvalPoint");
            model.result().numerical("pev_test").set("data", "cpt_cyl");
            model.result().numerical("pev_test").set("expr", new String[]{"x", "y", "X", "Y", "u", "v"});
            double[][] res = model.result().numerical("pev_test").getReal();
            String[] names = {"x", "y", "X", "Y", "u", "v"};
            for (int i = 0; i < res.length; i++) {
                System.out.println(names[i] + ": " + res[i][0]);
            }
            ModelUtil.remove("Model");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

