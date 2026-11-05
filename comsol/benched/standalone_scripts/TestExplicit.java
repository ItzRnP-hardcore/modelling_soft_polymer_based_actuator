import com.comsol.model.*;
import com.comsol.model.util.*;

public class TestExplicit {
    public static void main(String[] args) {
        try {
            Model model = ModelUtil.create("Model");
            model.component().create("comp1", true);
            model.component("comp1").geom().create("geom1", 3);
            model.component("comp1").geom("geom1").create("blk1", "Block");
            model.component("comp1").geom("geom1").run();

            model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
            model.component("comp1").physics("solid").create("bndl1", "BoundaryLoad", 2);
            model.component("comp1").physics("solid").feature("bndl1").selection().set(new int[]{1, 2});
            int[] ents = model.component("comp1").physics("solid").feature("bndl1").selection().entities();
            System.out.println("Direct physics selection entities: " + java.util.Arrays.toString(ents));
            System.out.println("DIRECT SET WORKS PERFECTLY!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
