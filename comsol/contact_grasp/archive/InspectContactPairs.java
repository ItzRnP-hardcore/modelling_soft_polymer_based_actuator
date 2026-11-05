import com.comsol.model.*;
import com.comsol.model.util.*;
import java.util.*;

public class InspectContactPairs {
    public static void main(String[] args) throws Exception {
        Model model = ModelUtil.create("Model");
        model.modelPath("c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp");

        double L_act = 140.0;
        double R0 = 9.5;
        double Rt = 3.8;
        double rCyl = 15.0;
        double X_cyl = 30.0;
        double Y_cyl = -28.0;
        double L_cyl = 40.0;

        model.param().set("L_act", L_act + "[mm]");
        model.param().set("R0", R0 + "[mm]");
        model.param().set("Rt", Rt + "[mm]");
        model.param().set("R_cyl", rCyl + "[mm]");
        model.param().set("X_cyl", X_cyl + "[mm]");
        model.param().set("Y_cyl", Y_cyl + "[mm]");
        model.param().set("L_cyl", L_cyl + "[mm]");

        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").lengthUnit("mm");

        model.component("comp1").geom("geom1").create("cone_body", "Cone");
        model.component("comp1").geom("geom1").feature("cone_body").set("axistype", "x");
        model.component("comp1").geom("geom1").feature("cone_body").set("pos", new double[]{0.0, 0.0, 0.0});
        model.component("comp1").geom("geom1").feature("cone_body").set("r", "R0");
        model.component("comp1").geom("geom1").feature("cone_body").set("specifytop", "radius");
        model.component("comp1").geom("geom1").feature("cone_body").set("rtop", "Rt");
        model.component("comp1").geom("geom1").feature("cone_body").set("h", "L_act");

        model.component("comp1").geom("geom1").create("target_cyl", "Cylinder");
        model.component("comp1").geom("geom1").feature("target_cyl").set("axistype", "z");
        model.component("comp1").geom("geom1").feature("target_cyl").set("pos", new double[]{X_cyl, Y_cyl, -L_cyl/2.0});
        model.component("comp1").geom("geom1").feature("target_cyl").set("r", "R_cyl");
        model.component("comp1").geom("geom1").feature("target_cyl").set("h", "L_cyl");

        model.component("comp1").geom("geom1").feature("fin").set("action", "assembly");
        model.component("comp1").geom("geom1").feature("fin").set("createpairs", true);
        model.component("comp1").geom("geom1").run();

        System.out.println("Geometry run completed.");
        System.out.println("Domains: " + model.component("comp1").geom("geom1").getNDomains());
        System.out.println("Boundaries: " + model.component("comp1").geom("geom1").getNBoundaries());

        // Check pairs
        String[] pairTags = model.pair().tags();
        System.out.println("Pair tags count: " + pairTags.length);
        for (String pt : pairTags) {
            System.out.println("Pair tag: " + pt);
        }

        // Test creating a manual contact pair
        try {
            model.pair().create("p1", "Contact", "geom1");
            System.out.println("Created pair p1 successfully!");
            // Source: cylinder boundaries (master)
            // Destination: actuator outer boundaries (slave)
            model.pair("p1").source().set(new int[]{4, 5, 6}); // dummy entities
            model.pair("p1").destination().set(new int[]{1, 2, 3});
            System.out.println("Set source/destination successfully!");
        } catch (Exception e) {
            System.out.println("Error on pair create: " + e.getMessage());
            e.printStackTrace();
        }

        ModelUtil.remove("Model");
    }
}
