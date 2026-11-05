import com.comsol.model.*;
import com.comsol.model.util.*;
import com.comsol.model.physics.*;

public class InspectProps {
    public static Model run() {
        Model model = ModelUtil.create("Model");
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").create("blk1", "Block");
        model.component("comp1").geom("geom1").run();
        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
        try {
            PhysicsFeature hmm = model.component("comp1").physics("solid").create("hmm1", "HyperelasticModel", 3);
            System.out.println("=== HyperelasticModel properties ===");
            for (String p : hmm.properties()) {
                System.out.println("  prop: " + p);
            }
        } catch (Exception e) {
            System.out.println("HyperelasticModel error: " + e.getMessage());
            for (String f : model.component("comp1").physics("solid").feature().tags()) {
                System.out.println("Existing solid feature: " + f);
                for (String p : model.component("comp1").physics("solid").feature(f).properties()) {
                    System.out.println("    " + f + " prop: " + p);
                }
            }
        }
        PhysicsFeature bndl = model.component("comp1").physics("solid").create("bndl1", "BoundaryLoad", 2);
        System.out.println("=== BoundaryLoad properties ===");
        for (String p : bndl.properties()) {
            System.out.println("  prop: " + p);
        }
        return model;
    }
    public static void main(String[] args) { run(); }
}
