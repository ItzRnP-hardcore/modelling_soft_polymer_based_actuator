import com.comsol.model.*;
import com.comsol.model.util.*;

public class CheckCoords {
    public static Model run() {
        try {
            Model model = ModelUtil.load("Model", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\actuator_sleeve_solved_Model.mph");
            int[] b_outer = model.component("comp1").selection("sel_outer").entities(2);
            for (int b : b_outer) {
                System.out.println("Boundary: " + b);
            }
            // Evaluate Y, Z ranges on sel_outer
            model.result().numerical().create("max_y", "EvalGlobal");
            return model;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static void main(String[] args) { run(); }
}
