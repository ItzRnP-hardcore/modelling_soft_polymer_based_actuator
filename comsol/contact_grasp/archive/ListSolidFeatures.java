import com.comsol.model.*;
import com.comsol.model.util.*;
import java.lang.reflect.*;
import java.util.*;

public class ListSolidFeatures {
    public static void main(String[] args) throws Exception {
        Model model = ModelUtil.create("Model");
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").create("blk", "Block");
        model.component("comp1").geom("geom1").run();

        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
        Object solidObj = model.component("comp1").physics("solid");

        System.out.println("Methods in Physics containing 'type' or 'feature':");
        for (Method m : solidObj.getClass().getMethods()) {
            String name = m.getName().toLowerCase();
            if (name.contains("type") || name.contains("feature")) {
                System.out.println("  " + m.getReturnType().getSimpleName() + " " + m.getName() + "(" + Arrays.toString(m.getParameterTypes()) + ")");
            }
        }

        // Test common feature names:
        String[] candidates = {
            "SpringFoundation", "Spring", "ElasticFoundation", "FlexibleBushing",
            "ThinElasticLayer", "RigidConnector", "RigidDomain", "PrescribedDisplacement",
            "Free", "Symmetry", "Roller", "AddedMass", "Damping", "BodyLoad",
            "SpringFoundationBoundary", "SpringFoundationDomain", "SpringBoundary", "SpringDomain"
        };

        for (String c : candidates) {
            for (int dim = 0; dim <= 3; dim++) {
                try {
                    model.component("comp1").physics("solid").create("t_" + c + "_" + dim, c, dim);
                    System.out.println(">>> FOUND VALID FEATURE: " + c + " at dim=" + dim);
                    model.component("comp1").physics("solid").feature().remove("t_" + c + "_" + dim);
                } catch (Exception ignored) {}
            }
        }

        ModelUtil.remove("Model");
    }
}
