import com.comsol.model.*;
import com.comsol.model.util.*;

public class TestWorkPlane {
    public static void main(String[] args) {
        try {
            Model model = ModelUtil.create("Model");
            model.component().create("comp1", true);
            model.component("comp1").geom().create("geom1", 3);

            model.component("comp1").geom("geom1").create("wp1", "WorkPlane");
            model.component("comp1").geom("geom1").feature("wp1").set("quickplane", "yz");
            model.component("comp1").geom("geom1").feature("wp1").set("quickx", 4.0);

            // In 2D plane: create circle sector or polygon
            GeomSequence g2 = model.component("comp1").geom("geom1").feature("wp1").geom();
            g2.create("sec1", "Circle");
            g2.feature("sec1").set("r", 10.0);
            g2.feature("sec1").set("sectorangle", 80.0);
            g2.feature("sec1").set("rot", -40.0);

            model.component("comp1").geom("geom1").run();
            System.out.println("WorkPlane + Circle Sector SUCCESS!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

