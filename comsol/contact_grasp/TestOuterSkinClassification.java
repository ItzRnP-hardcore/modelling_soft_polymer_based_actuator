import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

public class TestOuterSkinClassification {
    public static void main(String[] args) {
        try {
            String mphFile = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp\\loop_caging_optimization\\actuator_LoopCaging_R8mm_Pos1_Xc48_Yc-42_solved.mph";
            Model model = ModelUtil.load("Model", mphFile);

            double L_act = 220.0;
            double R0 = 11.5;
            double Rt = 3.2;
            double X_cyl = 48.0;
            double Y_cyl = -42.0;
            double rCyl = 8.0;
            double L_cyl = 50.0;

            int nBnd = model.component("comp1").geom("geom1").getNBoundaries();
            System.out.println("Total Boundaries: " + nBnd);

            List<Integer> list_cyl = new ArrayList<>();
            List<Integer> list_base = new ArrayList<>();
            List<Integer> list_outer = new ArrayList<>();

            for (int b = 1; b <= nBnd; b++) {
                model.result().numerical().create("int_b", "IntSurface");
                model.result().numerical("int_b").set("data", "dset_mesh");
                model.result().numerical("int_b").selection().set(new int[]{b});
                model.result().numerical("int_b").set("expr", new String[]{"X", "Y", "Z", "1"});
                double[][] res = model.result().numerical("int_b").getReal();
                double area = res[3][0];
                double xm = (res[0][0] / area) * 1000.0;
                double ym = (res[1][0] / area) * 1000.0;
                double zm = (res[2][0] / area) * 1000.0;
                double rm = Math.hypot(ym, zm);
                model.result().numerical().remove("int_b");

                // Check Cylinder
                double distCylAxis = Math.hypot(xm - X_cyl, ym - Y_cyl);
                if (distCylAxis <= rCyl + 0.8 && Math.abs(zm) <= L_cyl/2.0 + 0.8) {
                    list_cyl.add(b);
                    continue;
                }

                // Check Base
                if (Math.abs(xm) < 0.2) {
                    list_base.add(b);
                    continue;
                }

                // Outer skin: radius is roughly (0.85..1.05) * r_cone_expected, and area is significant
                double r_cone_expected = R0 - (R0 - Rt) * (xm / L_act);
                if (Math.abs(rm - 0.90 * r_cone_expected) < 1.8 && area > 2e-4) {
                    list_outer.add(b);
                    System.out.printf("Boundary %d is ACTUATOR OUTER: xm=%.1f, rm=%.2f, expected_r=%.2f, area=%.6f%n",
                                      b, xm, rm, r_cone_expected, area);
                    continue;
                }

                if (Math.abs(xm - L_act) < 0.2) {
                    list_outer.add(b);
                    System.out.printf("Boundary %d is TIP CAP: xm=%.1f%n", b, xm);
                    continue;
                }
            }

            System.out.println("Cylinder boundaries: " + list_cyl);
            System.out.println("Base boundaries: " + list_base);
            System.out.println("Actuator outer skin boundaries: " + list_outer);

            ModelUtil.remove("Model");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

