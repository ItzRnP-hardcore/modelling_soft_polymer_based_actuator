import com.comsol.model.*;
import com.comsol.model.util.*;
import java.util.*;

public class TestPairSelectionAPI {
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

        model.pair().create("p1", "Contact", "geom1");
        model.pair("p1").source().set(new int[]{6});
        model.pair("p1").destination().set(new int[]{7});

        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
        model.component("comp1").physics("solid").create("cnt1", "Contact", 2);

        // Test pairSelection = "contact"
        try {
            model.component("comp1").physics("solid").feature("cnt1").set("pairSelection", "contact");
            System.out.println("pairSelection=contact succeeded!");
        } catch (Exception e) {
            System.out.println("pairSelection=contact failed: " + e.getMessage());
        }

        // Test pairSelection = "all"
        try {
            model.component("comp1").physics("solid").feature("cnt1").set("pairSelection", "all");
            System.out.println("pairSelection=all succeeded!");
        } catch (Exception e) {
            System.out.println("pairSelection=all failed: " + e.getMessage());
        }

        // Test pairSelection = "list" and selection().named("p1")
        try {
            model.component("comp1").physics("solid").feature("cnt1").set("pairSelection", "list");
            model.component("comp1").physics("solid").feature("cnt1").selection().named("p1");
            System.out.println("pairSelection=list with selection().named('p1') succeeded!");
        } catch (Exception e) {
            System.out.println("selection().named('p1') failed: " + e.getMessage());
        }

        ModelUtil.remove("Model");
    }
}
