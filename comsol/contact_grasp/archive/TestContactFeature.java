import com.comsol.model.*;
import com.comsol.model.util.*;
import java.util.*;

public class TestContactFeature {
    public static void main(String[] args) throws Exception {
        Model model = ModelUtil.create("Model");
        model.modelPath("c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp");

        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").lengthUnit("mm");

        model.component("comp1").geom("geom1").create("b1", "Block");
        model.component("comp1").geom("geom1").feature("b1").set("size", new double[]{10, 10, 10});

        model.component("comp1").geom("geom1").create("b2", "Block");
        model.component("comp1").geom("geom1").feature("b2").set("pos", new double[]{20, 0, 0});
        model.component("comp1").geom("geom1").feature("b2").set("size", new double[]{10, 10, 10});

        model.component("comp1").geom("geom1").feature("fin").set("action", "assembly");
        model.component("comp1").geom("geom1").run();

        // Create contact pair
        model.pair().create("p1", "Contact", "geom1");
        model.pair("p1").source().set(new int[]{6}); // face of b1
        model.pair("p1").destination().set(new int[]{7}); // face of b2

        // Physics
        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
        model.component("comp1").physics("solid").create("cnt1", "Contact", 2);

        System.out.println("Available param names in cnt1:");
        for (String p : model.component("comp1").physics("solid").feature("cnt1").param()) {
            System.out.println("  Param: " + p + " = " + model.component("comp1").physics("solid").feature("cnt1").getString(p));
        }

        // Check pair selection in cnt1
        try {
            model.component("comp1").physics("solid").feature("cnt1").set("pairSelection", "all");
            System.out.println("pairSelection=all works!");
        } catch (Exception e) {
            System.out.println("Error pairSelection all: " + e.getMessage());
        }

        try {
            model.component("comp1").physics("solid").feature("cnt1").set("pairSelection", "manual");
            System.out.println("pairSelection=manual works!");
        } catch (Exception e) {
            System.out.println("Error pairSelection manual: " + e.getMessage());
        }

        ModelUtil.remove("Model");
    }
}
