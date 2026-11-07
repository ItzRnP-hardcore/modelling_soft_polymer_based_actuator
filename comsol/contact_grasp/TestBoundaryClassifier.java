import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

public class TestBoundaryClassifier {
    public static void main(String[] args) {
        try {
            String baseDir = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp";
            double fixedRcyl = 8.0;
            double X_cyl = 48.0;
            double Y_cyl = -42.0;
            double L_cyl = 50.0;

            Model model = ModelUtil.create("Model");
            model.modelPath(baseDir);

            model.param().set("R_cyl", fixedRcyl + "[mm]");
            model.param().set("X_cyl", X_cyl + "[mm]");
            model.param().set("Y_cyl", Y_cyl + "[mm]");
            model.param().set("L_cyl", L_cyl + "[mm]");

            model.component().create("comp1", true);
            model.component("comp1").geom().create("geom1", 3);
            model.component("comp1").geom("geom1").lengthUnit("mm");

            model.component("comp1").geom("geom1").create("cone_body", "Cone");
            model.component("comp1").geom("geom1").feature("cone_body").set("axistype", "x");
            model.component("comp1").geom("geom1").feature("cone_body").set("r", "11.5[mm]");
            model.component("comp1").geom("geom1").feature("cone_body").set("specifytop", "radius");
            model.component("comp1").geom("geom1").feature("cone_body").set("rtop", "3.2[mm]");
            model.component("comp1").geom("geom1").feature("cone_body").set("h", "220[mm]");

            model.component("comp1").geom("geom1").create("target_cyl", "Cylinder");
            model.component("comp1").geom("geom1").feature("target_cyl").set("axistype", "z");
            model.component("comp1").geom("geom1").feature("target_cyl").set("pos", new double[]{X_cyl, Y_cyl, -L_cyl/2.0});
            model.component("comp1").geom("geom1").feature("target_cyl").set("r", "R_cyl");
            model.component("comp1").geom("geom1").feature("target_cyl").set("h", "L_cyl");

            model.component("comp1").geom("geom1").feature("fin").set("action", "assembly");
            model.component("comp1").geom("geom1").feature("fin").set("createpairs", true);
            model.component("comp1").geom("geom1").run();

            model.component("comp1").mesh().create("mesh1");
            model.component("comp1").mesh("mesh1").autoMeshSize(5);
            model.component("comp1").mesh("mesh1").run();

            model.result().dataset().create("dset_mesh", "Mesh");
            model.result().dataset("dset_mesh").set("mesh", "mesh1");

            int nBnd = model.component("comp1").geom("geom1").getNBoundaries();

            List<Integer> list_cyl = new ArrayList<>();
            List<Integer> list_act = new ArrayList<>();

            for (int b = 1; b <= nBnd; b++) {
                model.result().numerical().create("int_b", "IntSurface");
                model.result().numerical("int_b").set("data", "dset_mesh");
                model.result().numerical("int_b").selection().set(new int[]{b});
                model.result().numerical("int_b").set("expr", new String[]{"X", "Y", "Z", "1"});
                double[][] res = model.result().numerical("int_b").getReal();
                double area = res[3][0];
                double xm_mm = (res[0][0] / area) * 1000.0;
                double ym_mm = (res[1][0] / area) * 1000.0;
                double zm_mm = (res[2][0] / area) * 1000.0;
                model.result().numerical().remove("int_b");

                double distCylAxis = Math.hypot(xm_mm - X_cyl, ym_mm - Y_cyl);
                if (distCylAxis <= fixedRcyl + 0.8 && Math.abs(zm_mm) <= L_cyl/2.0 + 0.8) {
                    list_cyl.add(b);
                    System.out.printf("Boundary %d is CYLINDER: pos=(%.2f, %.2f, %.2f) mm, dist=%.2f mm%n", b, xm_mm, ym_mm, zm_mm, distCylAxis);
                } else {
                    list_act.add(b);
                }
            }

            System.out.println("SUCCESS! Found " + list_cyl.size() + " cylinder boundaries: " + list_cyl);
            System.out.println("SUCCESS! Found " + list_act.size() + " actuator boundaries: " + list_act);
            ModelUtil.remove("Model");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
