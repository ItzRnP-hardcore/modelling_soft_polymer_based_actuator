import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

public class TestLargeDeflection {
    public static Model run() {
        Model model = ModelUtil.create("Model");
        String baseDir = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\large_deflection_4cavity";
        model.modelPath(baseDir);

        // Slender tapered geometry for large bending (theta >= 180 deg)
        model.param().set("L_act", "160[mm]", "Actuator total length");
        model.param().set("R0", "8.0[mm]", "Base outer radius");
        model.param().set("Rt", "4.0[mm]", "Tip outer radius");
        model.param().set("x_start", "4[mm]", "Cavity start position");
        model.param().set("x_end", "154[mm]", "Cavity end position");
        model.param().set("L_cav", "x_end - x_start", "Cavity length");

        model.param().set("tw_0", "1.0[mm]", "Outer wall thickness at base");
        model.param().set("tw_t", "0.7[mm]", "Outer wall thickness at tip");
        model.param().set("r_core0", "1.6[mm]", "Inner core radius at base");
        model.param().set("r_coret", "1.0[mm]", "Inner core radius at tip");

        model.param().set("r_out0", "R0 - tw_0", "Cavity outer radius at base");
        model.param().set("r_outt", "Rt - tw_t", "Cavity outer radius at tip");
        model.param().set("r_in0", "r_core0", "Cavity inner radius at base");
        model.param().set("r_int", "r_coret", "Cavity inner radius at tip");

        // Silicone Elastomer (Soft Ecoflex 00-30 / DragonSkin grade)
        model.param().set("E_mod", "0.25[MPa]", "Small-strain Young's modulus");
        model.param().set("nu_val", "0.45", "Poisson's ratio");

        model.param().set("P_step", "0[kPa]", "Continuation load parameter");

        // Geometry: 4 Arc-shaped cavities (80 deg each), 4 solid radial ribs (10 deg each)
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").lengthUnit("mm");

        model.component("comp1").geom("geom1").create("cone_body", "Cone");
        model.component("comp1").geom("geom1").feature("cone_body").set("axistype", "x");
        model.component("comp1").geom("geom1").feature("cone_body").set("pos", new double[]{0.0, 0.0, 0.0});
        model.component("comp1").geom("geom1").feature("cone_body").set("r", "R0");
        model.component("comp1").geom("geom1").feature("cone_body").set("specifytop", "radius");
        model.component("comp1").geom("geom1").feature("cone_body").set("rtop", "Rt");
        model.component("comp1").geom("geom1").feature("cone_body").set("h", "L_act");

        double rotOffset = 50.0;
        double[] rotAngles = {0.0, 90.0, 180.0, -90.0};
        String[] cavTags = new String[4];

        for (int k = 1; k <= 4; k++) {
            double baseRot = rotAngles[k-1];
            String b1Tag = "blk1_" + k;
            String b2Tag = "blk2_" + k;
            String r1Tag = "rot1_" + k;
            String r2Tag = "rot2_" + k;
            String wTag = "wedge_" + k;
            String sTag = "shell_" + k;
            String cTag = "cav_" + k;
            cavTags[k-1] = cTag;

            model.component("comp1").geom("geom1").create("cone_out_" + k, "Cone");
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("axistype", "x");
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("pos", new double[]{4.0, 0.0, 0.0});
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("r", "r_out0");
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("specifytop", "radius");
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("rtop", "r_outt");
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("h", "L_cav");

            model.component("comp1").geom("geom1").create("cone_in_" + k, "Cone");
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("axistype", "x");
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("pos", new double[]{4.0, 0.0, 0.0});
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("r", "r_in0");
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("specifytop", "radius");
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("rtop", "r_int");
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("h", "L_cav");

            model.component("comp1").geom("geom1").create(sTag, "Difference");
            model.component("comp1").geom("geom1").feature(sTag).selection("input").set("cone_out_" + k);
            model.component("comp1").geom("geom1").feature(sTag).selection("input2").set("cone_in_" + k);

            model.component("comp1").geom("geom1").create(b1Tag, "Block");
            model.component("comp1").geom("geom1").feature(b1Tag).set("pos", new double[]{3.5, 0.0, -50.0});
            model.component("comp1").geom("geom1").feature(b1Tag).set("size", new double[]{152.0, 50.0, 100.0});

            model.component("comp1").geom("geom1").create(r1Tag, "Rotate");
            model.component("comp1").geom("geom1").feature(r1Tag).set("rot", baseRot + rotOffset);
            model.component("comp1").geom("geom1").feature(r1Tag).set("axistype", "x");
            model.component("comp1").geom("geom1").feature(r1Tag).selection("input").set(b1Tag);

            model.component("comp1").geom("geom1").create(b2Tag, "Block");
            model.component("comp1").geom("geom1").feature(b2Tag).set("pos", new double[]{3.5, 0.0, -50.0});
            model.component("comp1").geom("geom1").feature(b2Tag).set("size", new double[]{152.0, 50.0, 100.0});

            model.component("comp1").geom("geom1").create(r2Tag, "Rotate");
            model.component("comp1").geom("geom1").feature(r2Tag).set("rot", baseRot - rotOffset);
            model.component("comp1").geom("geom1").feature(r2Tag).set("axistype", "x");
            model.component("comp1").geom("geom1").feature(r2Tag).selection("input").set(b2Tag);

            model.component("comp1").geom("geom1").create(wTag, "Intersection");
            model.component("comp1").geom("geom1").feature(wTag).selection("input").set(r1Tag, r2Tag);

            model.component("comp1").geom("geom1").create(cTag, "Intersection");
            model.component("comp1").geom("geom1").feature(cTag).selection("input").set(sTag, wTag);
        }

        model.component("comp1").geom("geom1").create("dif_actuator", "Difference");
        model.component("comp1").geom("geom1").feature("dif_actuator").selection("input").set("cone_body");
        model.component("comp1").geom("geom1").feature("dif_actuator").selection("input2").set(cavTags);

        model.component("comp1").geom("geom1").run();

        // Mesh
        int nBnd = model.component("comp1").geom("geom1").getNBoundaries();
        model.component("comp1").mesh().create("mesh1");
        model.component("comp1").mesh("mesh1").autoMeshSize(5);
        model.component("comp1").mesh("mesh1").run();

        model.result().dataset().create("dset_mesh", "Mesh");
        model.result().dataset("dset_mesh").set("mesh", "mesh1");

        List<Integer> list_c1 = new ArrayList<>();
        for (int b = 1; b <= nBnd; b++) {
            if (b == 1 || b == nBnd || (b >= 2 && b <= 5)) continue;
            model.result().numerical().create("int_b", "IntSurface");
            model.result().numerical("int_b").set("data", "dset_mesh");
            model.result().numerical("int_b").selection().set(new int[]{b});
            model.result().numerical("int_b").set("expr", new String[]{"X*1000", "Y*1000", "Z*1000", "1"});
            double[][] res = model.result().numerical("int_b").getReal();
            double ym = res[1][0] / res[3][0];
            double zm = res[2][0] / res[3][0];
            model.result().numerical().remove("int_b");

            double angDeg = Math.atan2(zm, ym) * 180.0 / Math.PI;
            if (angDeg >= -45.0 && angDeg <= 45.0) {
                list_c1.add(b); // Cavity 1: Top (+Y)
            }
        }
        int[] arr_c1 = list_c1.stream().mapToInt(i -> i).toArray();
        System.out.println("Cavity 1 boundaries: " + Arrays.toString(arr_c1));

        // Physics
        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
        model.component("comp1").physics("solid").feature("lemm1").set("E_mat", "userdef");
        model.component("comp1").physics("solid").feature("lemm1").set("E", "E_mod");
        model.component("comp1").physics("solid").feature("lemm1").set("nu_mat", "userdef");
        model.component("comp1").physics("solid").feature("lemm1").set("nu", "nu_val");

        model.component("comp1").physics("solid").create("fix1", "Fixed", 2);
        model.component("comp1").physics("solid").feature("fix1").selection().set(new int[]{1});

        model.component("comp1").physics("solid").create("bndl1", "BoundaryLoad", 2);
        model.component("comp1").physics("solid").feature("bndl1").selection().set(arr_c1);
        model.component("comp1").physics("solid").feature("bndl1").set("forceType", "FollowerPressure");
        model.component("comp1").physics("solid").feature("bndl1").set("pressure", "P_step");

        // Study: sweep pressure
        model.study().create("std1");
        model.study("std1").create("stat", "Stationary");
        model.study("std1").feature("stat").set("geometricNonlinearity", true);
        model.study("std1").feature("stat").set("useparam", true);
        model.study("std1").feature("stat").set("pname", new String[]{"P_step"});
        model.study("std1").feature("stat").set("plistarr", new String[]{"0 5 10 15 20 25 30 35 40 45 50"});
        model.study("std1").feature("stat").set("punit", new String[]{"kPa"});

        try {
            model.study("std1").run();
        } catch (Exception ex) {
            System.out.println("Solver note: " + ex.getMessage());
        }

        model.result().dataset().create("cpt_tip", "CutPoint3D");
        model.result().dataset("cpt_tip").set("data", "dset1");
        model.result().dataset("cpt_tip").set("pointx", "160");
        model.result().dataset("cpt_tip").set("pointy", "0");
        model.result().dataset("cpt_tip").set("pointz", "0");

        model.result().numerical().create("pev_tip", "EvalPoint");
        model.result().numerical("pev_tip").set("data", "cpt_tip");
        model.result().numerical("pev_tip").set("expr", new String[]{"P_step", "u", "v", "w", "solid.disp"});
        model.result().numerical("pev_tip").set("unit", new String[]{"kPa", "mm", "mm", "mm", "mm"});

        double[][] uvw = model.result().numerical("pev_tip").getReal();
        System.out.println("=== LARGE DEFLECTION TEST RESULTS ===");
        System.out.println("P_kPa, u_tip, v_tip, w_tip, delta_3d, theta_deg, deformed_x, deformed_y");
        for (int s = 0; s < uvw[0].length; s++) {
            double p = uvw[0][s];
            double u = uvw[1][s];
            double v = uvw[2][s];
            double w = uvw[3][s];
            double d3 = uvw[4][s];
            double defX = 160.0 + u;
            double defY = v;
            // Arc tangent approximation for curvature / tip orientation:
            double theta = Math.atan2(Math.abs(v), defX) * 180.0 / Math.PI;
            System.out.printf(Locale.US, "%.1f, %.3f, %.3f, %.3f, %.3f, %.2f, %.2f, %.2f\n",
                p, u, v, w, d3, theta, defX, defY);
        }

        return model;
    }

    public static void main(String[] args) {
        run();
    }
}

