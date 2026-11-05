import com.comsol.model.*;
import com.comsol.model.util.*;
import com.comsol.model.physics.*;

public class InspectSolidFeatures {
    public static Model run() {
        Model model = ModelUtil.create("Model");
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").create("blk1", "Block");
        model.component("comp1").geom("geom1").run();
        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
        String[] candidates = new String[]{"ThinElasticLayer", "SpringFoundation", "SpringFoundationSolid", "PeriodicCondition", "BodyLoad", "ExternalStress", "Predeformation"};
        for (String c : candidates) {
            try {
                PhysicsFeature f = model.component("comp1").physics("solid").create("test_" + c, c, 2);
                System.out.println("Feature valid for boundary (dim 2): " + c);
                for (String p : f.properties()) {
                    System.out.print(p + " ");
                }
                System.out.println();
            } catch (Exception e) {
                // not a boundary feature or not recognized
            }
        }
        return model;
    }
    public static void main(String[] args) { run(); }
}
