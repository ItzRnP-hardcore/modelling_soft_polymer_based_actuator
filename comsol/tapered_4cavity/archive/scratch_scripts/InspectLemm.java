import com.comsol.model.*;
import com.comsol.model.util.*;
import com.comsol.model.physics.*;
import java.util.*;

public class InspectLemm {
    public static Model run() {
        Model model = ModelUtil.create("Model");
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").create("blk1", "Block");
        model.component("comp1").geom("geom1").run();
        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
        
        PhysicsFeature lemm = model.component("comp1").physics("solid").feature("lemm1");
        System.out.println("=== lemm1 properties ===");
        for (String p : lemm.properties()) {
            try {
                String val = Arrays.toString(lemm.getStringArray(p));
                System.out.println("  prop: " + p + " = " + val);
            } catch (Exception e1) {
                try {
                    String val = lemm.getString(p);
                    System.out.println("  prop: " + p + " = " + val);
                } catch (Exception e2) {
                    System.out.println("  prop: " + p);
                }
            }
        }
        return model;
    }
    public static void main(String[] args) { run(); }
}

