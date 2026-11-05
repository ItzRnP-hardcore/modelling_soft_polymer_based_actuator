import com.comsol.model.*;
import com.comsol.model.util.*;
import java.util.*;

public class CheckCylProps {
    public static void main(String[] args) {
        Model model = ModelUtil.create("Model");
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").create("blk1", "Block");
        model.component("comp1").geom("geom1").run();
        
        // Pass geom1 before type
        model.component("comp1").coordSystem().create("cyl1", "geom1", "Cylindrical");
        model.component("comp1").coordSystem("cyl1").set("axis", new String[]{"1", "0", "0"});
        model.component("comp1").coordSystem("cyl1").set("radialbasevector", new String[]{"0", "1", "0"});
        
        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
        
        try {
            model.component("comp1").physics("solid").feature("lemm1").set("coordinateSystem", "cyl1");
            System.out.println("SUCCESS setting cyl1 with geom1!");
        } catch (Exception e) {
            System.out.println("cyl1 with geom1 failed: " + e.getMessage());
        }
    }
}
