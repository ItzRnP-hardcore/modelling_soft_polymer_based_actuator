import com.comsol.model.*;
import com.comsol.model.util.*;
import java.util.*;

public class InspectRigidDomain {
    public static void main(String[] args) throws Exception {
        Model model = ModelUtil.create("Model");
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").create("blk", "Block");
        model.component("comp1").geom("geom1").run();

        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
        model.component("comp1").physics("solid").create("rd1", "RigidDomain", 3);
        model.component("comp1").physics("solid").feature("rd1").selection().set(new int[]{1});

        System.out.println("Parameters in RigidDomain:");
        for (String p : model.component("comp1").physics("solid").feature("rd1").param()) {
            System.out.println("  " + p + " = " + model.component("comp1").physics("solid").feature("rd1").getString(p));
        }

        System.out.println("Sub-features available in RigidDomain:");
        for (String sf : new String[]{"SpringFoundation", "Spring", "Fixed", "PrescribedDisplacement", "AppliedForce", "MassAndMomentOfInertia"}) {
            try {
                model.component("comp1").physics("solid").feature("rd1").create("sub_" + sf, sf);
                System.out.println(">>> VALID SUBFEATURE in RigidDomain: " + sf);
                for (String p : model.component("comp1").physics("solid").feature("rd1").feature("sub_" + sf).param()) {
                    System.out.println("    " + p + " = " + model.component("comp1").physics("solid").feature("rd1").feature("sub_" + sf).getString(p));
                }
            } catch (Exception ignored) {}
        }

        ModelUtil.remove("Model");
    }
}

