import com.comsol.model.*;
import com.comsol.model.util.*;

public class InspectCone {
    public static void main(String[] args) {
        try {
            Model model = ModelUtil.create("Model");
            model.component().create("comp1", true);
            model.component("comp1").geom().create("geom1", 3);
            model.component("comp1").geom("geom1").create("cone1", "Cone");
            System.out.println("Cone properties:");
            for (String p : model.component("comp1").geom("geom1").feature("cone1").properties()) {
                System.out.println("  " + p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

