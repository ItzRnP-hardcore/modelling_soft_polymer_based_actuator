import com.comsol.model.*;
import com.comsol.model.util.*;
import java.util.Arrays;

public class InspectRigidSpring {
    public static void main(String[] args) {
        Model model = ModelUtil.create("Model");
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").create("blk1", "Block");
        model.component("comp1").geom("geom1").run();

        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
        model.component("comp1").physics("solid").create("rd1", "RigidDomain", 3);
        model.component("comp1").physics("solid").feature("rd1").selection().set(new int[]{1});
        
        System.out.println("RigidDomain rd1 properties:");
        for (String p : model.component("comp1").physics("solid").feature("rd1").properties()) {
            try {
                System.out.println("  " + p + " = " + Arrays.toString(model.component("comp1").physics("solid").feature("rd1").getStringArray(p)));
            } catch (Exception e) {
                try {
                    System.out.println("  " + p + " = " + model.component("comp1").physics("solid").feature("rd1").getString(p));
                } catch (Exception e2) {
                    System.out.println("  " + p + " (type not string)");
                }
            }
        }

        System.out.println("\nRigidDomain subfeatures available:");
        // let's check subfeature types
        String[] subTypes = {"SpringFoundation", "Fixed", "PrescribedDisplacement", "PrescribedRotation", "AppliedForce"};
        for (String st : subTypes) {
            try {
                model.component("comp1").physics("solid").feature("rd1").create("sub_" + st, st);
                System.out.println("  Can create: " + st);
                for (String p : model.component("comp1").physics("solid").feature("rd1").feature("sub_" + st).properties()) {
                    try {
                        System.out.println("    " + p + " = " + Arrays.toString(model.component("comp1").physics("solid").feature("rd1").feature("sub_" + st).getStringArray(p)));
                    } catch (Exception e) {
                        try {
                            System.out.println("    " + p + " = " + model.component("comp1").physics("solid").feature("rd1").feature("sub_" + st).getString(p));
                        } catch (Exception e2) {
                            System.out.println("    " + p);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("  Cannot create: " + st + " -> " + e.getMessage());
            }
        }

        ModelUtil.remove("Model");
    }
}

