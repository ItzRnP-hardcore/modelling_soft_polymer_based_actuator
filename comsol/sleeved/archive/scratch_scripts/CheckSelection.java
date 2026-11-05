import com.comsol.model.*;
import com.comsol.model.util.*;

public class CheckSelection {
    public static Model run() {
        try {
            Model model = ModelUtil.load("Model", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\actuator_sleeve_solved_Model.mph");
            int[] b_base = model.component("comp1").selection("sel_base").entities(2);
            int[] b_sym = model.component("comp1").selection("sel_sym").entities(2);
            int[] b_cav = model.component("comp1").selection("sel_cav").entities(2);
            int[] b_outer = model.component("comp1").selection("sel_outer").entities(2);
            System.out.println("sel_base count: " + b_base.length);
            System.out.println("sel_sym count: " + b_sym.length);
            System.out.println("sel_cav count: " + b_cav.length);
            System.out.println("sel_outer count: " + b_outer.length);
            for (int b : b_outer) System.out.print(b + " ");
            System.out.println();
            return model;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static void main(String[] args) { run(); }
}
