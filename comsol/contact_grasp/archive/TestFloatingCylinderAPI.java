import com.comsol.model.*;
import com.comsol.model.util.*;
import java.util.*;

public class TestFloatingCylinderAPI {
    public static void main(String[] args) throws Exception {
        Model model = ModelUtil.create("Model");
        model.modelPath("c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp");

        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").lengthUnit("mm");

        model.component("comp1").geom("geom1").create("target_cyl", "Cylinder");
        model.component("comp1").geom("geom1").feature("target_cyl").set("axistype", "z");
        model.component("comp1").geom("geom1").feature("target_cyl").set("pos", new double[]{30.0, -28.0, -20.0});
        model.component("comp1").geom("geom1").feature("target_cyl").set("r", "15.0");
        model.component("comp1").geom("geom1").feature("target_cyl").set("h", "40.0");
        model.component("comp1").geom("geom1").run();

        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");

        // Test creating SpringFoundation on domain 1
        try {
            model.component("comp1").physics("solid").create("spf1", "SpringFoundation", 3);
            model.component("comp1").physics("solid").feature("spf1").selection().set(new int[]{1});
            System.out.println("SpringFoundation on domain 3 succeeded!");
            System.out.println("Available params in spf1:");
            for (String p : model.component("comp1").physics("solid").feature("spf1").param()) {
                System.out.println("  Param: " + p + " = " + model.component("comp1").physics("solid").feature("spf1").getString(p));
            }
        } catch (Exception e) {
            System.out.println("SpringFoundation on domain failed: " + e.getMessage());
        }

        // Test creating SpringFoundation on boundaries (dim 2)
        try {
            model.component("comp1").physics("solid").create("spf2", "SpringFoundation", 2);
            System.out.println("SpringFoundation on boundary 2 succeeded!");
            System.out.println("Available params in spf2:");
            for (String p : model.component("comp1").physics("solid").feature("spf2").param()) {
                System.out.println("  Param: " + p + " = " + model.component("comp1").physics("solid").feature("spf2").getString(p));
            }
        } catch (Exception e) {
            System.out.println("SpringFoundation on boundary failed: " + e.getMessage());
        }

        ModelUtil.remove("Model");
    }
}

