import com.comsol.model.*;
import com.comsol.model.util.*;
import java.util.Arrays;

public class TestRigidOptions {
    public static void main(String[] args) {
        Model model = ModelUtil.create("Model");
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").create("blk1", "Block");
        model.component("comp1").geom("geom1").run();

        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
        model.component("comp1").physics("solid").create("rd1", "RigidDomain", 3);
        model.component("comp1").physics("solid").feature("rd1").selection().set(new int[]{1});
        
        // Let's inspect subfeature types for rd1
        try {
            System.out.println("Subfeature types allowed on rd1:");
            // Reflection on feature:
            Class<?> cls = model.component("comp1").physics("solid").feature("rd1").getClass();
            for (java.lang.reflect.Method m : cls.getMethods()) {
                if (m.getName().toLowerCase().contains("subfeature") || m.getName().toLowerCase().contains("create") || m.getName().toLowerCase().contains("type")) {
                    System.out.println("  method: " + m.getName() + " params: " + Arrays.toString(m.getParameterTypes()));
                }
            }
        } catch (Exception e) {}

        // Test SpringFoundation properties
        model.component("comp1").physics("solid").feature("rd1").create("spf1", "SpringFoundation");
        System.out.println("SpringType allowed values:");
        try {
            String[] vals = model.component("comp1").physics("solid").feature("rd1").feature("spf1").getAllowedPropertyValues("SpringType");
            System.out.println("  SpringType: " + Arrays.toString(vals));
        } catch (Exception e) {
            System.out.println("  SpringType allowed: " + e.getMessage());
        }

        try {
            String[] vals = model.component("comp1").physics("solid").feature("rd1").feature("spf1").getAllowedPropertyValues("RotSpringType");
            System.out.println("  RotSpringType: " + Arrays.toString(vals));
        } catch (Exception e) {
            System.out.println("  RotSpringType allowed: " + e.getMessage());
        }

        // Test setting kth
        try {
            model.component("comp1").physics("solid").feature("rd1").feature("spf1").set("kth", new String[]{"0.1[N*m/rad]", "0.1[N*m/rad]", "0.1[N*m/rad]"});
            System.out.println("Successfully set 3-vector kth!");
        } catch (Exception e) {
            System.out.println("Failed 3-vector kth: " + e.getMessage());
            try {
                model.component("comp1").physics("solid").feature("rd1").feature("spf1").set("kth", "0.1[N*m/rad]");
                System.out.println("Successfully set scalar kth!");
            } catch (Exception e2) {
                System.out.println("Failed scalar kth: " + e2.getMessage());
            }
        }

        ModelUtil.remove("Model");
    }
}

