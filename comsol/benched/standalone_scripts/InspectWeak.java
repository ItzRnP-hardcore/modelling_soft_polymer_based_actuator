import com.comsol.model.*;
import com.comsol.model.util.*;
import com.comsol.model.physics.*;

public class InspectWeak {
    public static Model run() {
        Model model = ModelUtil.create("Model");
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").create("blk1", "Block");
        model.component("comp1").geom("geom1").run();
        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
        try {
            PhysicsFeature wf = model.component("comp1").physics("solid").create("wf1", "WeakContribution", 2);
            System.out.println("WeakContribution on boundary valid!");
            for (String p : wf.properties()) System.out.print(p + " ");
            System.out.println();
        } catch (Exception e) {
            System.out.println("WeakContribution error: " + e.getMessage());
        }
        return model;
    }
    public static void main(String[] args) { run(); }
}
