import com.comsol.model.*;
import com.comsol.model.util.*;
import com.comsol.model.physics.*;

public class InspectProps2 {
    public static Model run() {
        Model model = ModelUtil.create("Model");
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").create("blk1", "Block");
        model.component("comp1").geom("geom1").run();
        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
        PhysicsFeature hmm = model.component("comp1").physics("solid").create("hmm1", "HyperelasticModel", 3);
        String[] hprops = new String[]{"MaterialModel", "Compressibility_NeoHookean", "VolumetricEnergyUncoupled", "MixedFormulation", "MixedFormulationIncompressible", "IsotropicOption", "muLame_mat", "K_mat", "C10_mat"};
        for (String p : hprops) {
            System.out.println("hmm." + p + " = " + hmm.getString(p));
            String[] allowed = hmm.getAllowedPropertyValues(p);
            if (allowed != null) {
                System.out.print("   allowed: ");
                for (String a : allowed) System.out.print(a + ", ");
                System.out.println();
            }
        }
        PhysicsFeature bndl = model.component("comp1").physics("solid").create("bndl1", "BoundaryLoad", 2);
        String[] bprops = new String[]{"forceType", "pressure", "treatasdeadload"};
        for (String p : bprops) {
            System.out.println("bndl." + p + " = " + bndl.getString(p));
            String[] allowed = bndl.getAllowedPropertyValues(p);
            if (allowed != null) {
                System.out.print("   allowed: ");
                for (String a : allowed) System.out.print(a + ", ");
                System.out.println();
            }
        }
        return model;
    }
    public static void main(String[] args) { run(); }
}
