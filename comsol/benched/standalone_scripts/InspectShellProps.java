import com.comsol.model.*;
import com.comsol.model.util.*;
import com.comsol.model.physics.*;

public class InspectShellProps {
    public static Model run() {
        Model model = ModelUtil.create("Model");
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").create("blk1", "Block");
        model.component("comp1").geom("geom1").run();
        model.component("comp1").physics().create("shell", "Shell", "geom1");
        PhysicsFeature emm = model.component("comp1").physics("shell").feature("emm1");
        System.out.println("=== Shell emm1 properties ===");
        for (String p : emm.properties()) {
            System.out.println("  prop: " + p);
        }
        return model;
    }
    public static void main(String[] args) { run(); }
}
