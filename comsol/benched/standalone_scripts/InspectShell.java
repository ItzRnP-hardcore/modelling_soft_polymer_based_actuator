import com.comsol.model.*;
import com.comsol.model.util.*;
import com.comsol.model.physics.*;

public class InspectShell {
    public static Model run() {
        Model model = ModelUtil.create("Model");
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").create("blk1", "Block");
        model.component("comp1").geom("geom1").run();
        try {
            model.component("comp1").physics().create("shell", "Shell", "geom1");
            System.out.println("Shell physics created successfully");
            for (String f : model.component("comp1").physics("shell").feature().tags()) {
                System.out.println("Shell feature: " + f);
            }
        } catch (Exception e) {
            System.out.println("Shell error: " + e.getMessage());
        }
        try {
            model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
            PhysicsFeature mem = model.component("comp1").physics("solid").create("mem1", "Membrane", 2);
            System.out.println("Membrane feature in solid created!");
        } catch (Exception e) {
            System.out.println("Membrane error: " + e.getMessage());
        }
        return model;
    }
    public static void main(String[] args) { run(); }
}
