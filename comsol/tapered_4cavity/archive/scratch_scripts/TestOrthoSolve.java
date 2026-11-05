import com.comsol.model.*;
import com.comsol.model.util.*;

public class TestOrthoSolve {
    public static Model run() {
        Model model = ModelUtil.create("Model");
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").lengthUnit("mm");

        model.component("comp1").geom("geom1").create("cone_body", "Cone");
        model.component("comp1").geom("geom1").feature("cone_body").set("axistype", "x");
        model.component("comp1").geom("geom1").feature("cone_body").set("r", 6.5);
        model.component("comp1").geom("geom1").feature("cone_body").set("specifytop", "radius");
        model.component("comp1").geom("geom1").feature("cone_body").set("rtop", 3.5);
        model.component("comp1").geom("geom1").feature("cone_body").set("h", 160.0);

        model.component("comp1").geom("geom1").create("cyl_string", "Cylinder");
        model.component("comp1").geom("geom1").feature("cyl_string").set("axistype", "x");
        model.component("comp1").geom("geom1").feature("cyl_string").set("r", 0.6);
        model.component("comp1").geom("geom1").feature("cyl_string").set("h", 160.0);
        model.component("comp1").geom("geom1").run();

        // Mesh
        model.component("comp1").mesh().create("mesh1");
        model.component("comp1").mesh("mesh1").autoMeshSize(4);
        model.component("comp1").mesh("mesh1").run();

        // Physics
        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");

        // Silicone body: lemm1 (Default covers all domains, will be overridden on domain 2)
        model.component("comp1").physics("solid").feature("lemm1").set("SolidModel", "Orthotropic");
        model.component("comp1").physics("solid").feature("lemm1").set("Evector_mat", "userdef");
        model.component("comp1").physics("solid").feature("lemm1").set("Evector", new String[]{"0.25[MPa]", "5.0[MPa]", "5.0[MPa]"});
        model.component("comp1").physics("solid").feature("lemm1").set("nuvector_mat", "userdef");
        model.component("comp1").physics("solid").feature("lemm1").set("nuvector", new String[]{"0.25", "0.45", "0.25"});
        model.component("comp1").physics("solid").feature("lemm1").set("Gvector_mat", "userdef");
        model.component("comp1").physics("solid").feature("lemm1").set("Gvector", new String[]{"0.1[MPa]", "1.7[MPa]", "0.1[MPa]"});

        // Kevlar string: Domain 2 overrides lemm1
        model.component("comp1").physics("solid").create("lemm2", "LinearElasticModel", 3);
        model.component("comp1").physics("solid").feature("lemm2").selection().set(new int[]{2});
        model.component("comp1").physics("solid").feature("lemm2").set("E_mat", "userdef");
        model.component("comp1").physics("solid").feature("lemm2").set("E", "2500[MPa]");
        model.component("comp1").physics("solid").feature("lemm2").set("nu_mat", "userdef");
        model.component("comp1").physics("solid").feature("lemm2").set("nu", "0.33");

        // Fix base
        model.component("comp1").physics("solid").create("fix1", "Fixed", 2);
        model.component("comp1").physics("solid").feature("fix1").selection().set(new int[]{1, 6});

        // Simple load
        model.component("comp1").physics("solid").create("bndl1", "BoundaryLoad", 2);
        model.component("comp1").physics("solid").feature("bndl1").selection().set(new int[]{5});
        model.component("comp1").physics("solid").feature("bndl1").set("forceType", "FollowerPressure");
        model.component("comp1").physics("solid").feature("bndl1").set("pressure", "10[kPa]");

        // Study
        model.study().create("std1");
        model.study("std1").create("stat", "Stationary");
        model.study("std1").feature("stat").set("geometricNonlinearity", true);

        System.out.println("Running test solve with Transversely Isotropic silicone + Kevlar core...");
        model.study("std1").run();
        System.out.println("Test solve SUCCESS!");
        return model;
    }
    public static void main(String[] args) { run(); }
}
