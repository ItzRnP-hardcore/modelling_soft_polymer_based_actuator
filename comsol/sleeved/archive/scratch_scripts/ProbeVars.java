import com.comsol.model.*;
import com.comsol.model.util.*;

public class ProbeVars {
    public static void main(String[] args) throws Exception {
        Model model = ModelUtil.load("Model", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\bare\\actuator_bare_solved_Model.mph");
        String[] exprs = {"X", "Y", "Z", "x", "y", "uTX", "uTY", "uTZ", "vTY", "wTZ", "uTx", "vTy", "solid.FdxY", "vY"};
        model.component("comp1").selection().create("sel_outer_probe", "Cylinder");
        model.component("comp1").selection("sel_outer_probe").set("entitydim", 2);
        model.component("comp1").selection("sel_outer_probe").set("condition", "allvertices");
        model.component("comp1").selection("sel_outer_probe").set("axistype", "x");
        model.component("comp1").selection("sel_outer_probe").set("r", "10.1");
        model.component("comp1").selection("sel_outer_probe").set("rin", "9.9");
        model.component("comp1").selection("sel_outer_probe").set("top", "105.1");
        model.component("comp1").selection("sel_outer_probe").set("bottom", "-0.1");
        model.result().numerical().create("ip", "IntSurface");
        model.result().numerical("ip").set("data", "dset1");
        model.result().numerical("ip").selection().named("sel_outer_probe");
        for (String e : exprs) {
            try {
                model.result().numerical("ip").set("expr", new String[]{e});
                double[][] v = model.result().numerical("ip").getReal();
                System.out.println("OK   " + e + " -> last step " + v[0][v[0].length - 1]);
            } catch (Exception ex) {
                System.out.println("FAIL " + e + " : " + ex.getMessage().split("\n")[0]);
            }
        }
    }
}
