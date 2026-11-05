import com.comsol.model.*;
import com.comsol.model.util.*;
import java.util.*;

public class InspectPhysicsFeatures {
    public static void main(String[] args) throws Exception {
        Model model = ModelUtil.create("Model");
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").create("blk", "Block");
        model.component("comp1").geom("geom1").run();

        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");

        System.out.println("Valid feature types for SolidMechanics:");
        for (int dim = 0; dim <= 3; dim++) {
            System.out.println("--- Dimension " + dim + " ---");
            try {
                // Try dummy feature to get list of valid types in error message
                model.component("comp1").physics("solid").create("test", "DummyFeatureType", dim);
            } catch (Exception e) {
                System.out.println("  dim=" + dim + ": " + e.getMessage());
            }
        }

        ModelUtil.remove("Model");
    }
}

