import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

public class TestRibbedVisuals {
    public static void main(String[] args) {
        try {
            Model model = ModelUtil.create("Model");
            String baseDir = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered_4cavity";
            model.modelPath(baseDir);

            model.param().set("L_act", "100[mm]");
            model.param().set("R0", "10[mm]");
            model.param().set("Rt", "7[mm]");
            model.param().set("x_start", "5[mm]");
            model.param().set("x_end", "92[mm]");
            model.param().set("L_cav", "x_end - x_start");

            model.param().set("tw_0", "1.5[mm]");
            model.param().set("tw_t", "1.0[mm]");
            model.param().set("depth_cav", "3.0[mm]");

            model.param().set("r_out0", "R0 - tw_0");
            model.param().set("r_outt", "Rt - tw_t");
            model.param().set("r_in0", "r_out0 - depth_cav");
            model.param().set("r_int", "r_outt - depth_cav");

            model.param().set("E_mod", "0.6[MPa]");
            model.param().set("nu_val", "0.45");
            model.param().set("P_step", "0[kPa]");

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

            // 80 deg arc angle per cavity -> rotOffset = 90 - 80/2 = 50 deg
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
                model.component("comp1").geom("geom1").feature("cone_out_" + k).set("pos", new double[]{5.0, 0.0, 0.0});
                model.component("comp1").geom("geom1").feature("cone_out_" + k).set("r", "r_out0");
                model.component("comp1").geom("geom1").feature("cone_out_" + k).set("specifytop", "radius");
                model.component("comp1").geom("geom1").feature("cone_out_" + k).set("rtop", "r_outt");
                model.component("comp1").geom("geom1").feature("cone_out_" + k).set("h", "L_cav");

                model.component("comp1").geom("geom1").create("cone_in_" + k, "Cone");
                model.component("comp1").geom("geom1").feature("cone_in_" + k).set("axistype", "x");
                model.component("comp1").geom("geom1").feature("cone_in_" + k).set("pos", new double[]{5.0, 0.0, 0.0});
                model.component("comp1").geom("geom1").feature("cone_in_" + k).set("r", "r_in0");
                model.component("comp1").geom("geom1").feature("cone_in_" + k).set("specifytop", "radius");
                model.component("comp1").geom("geom1").feature("cone_in_" + k).set("rtop", "r_int");
                model.component("comp1").geom("geom1").feature("cone_in_" + k).set("h", "L_cav");

                model.component("comp1").geom("geom1").create(sTag, "Difference");
                model.component("comp1").geom("geom1").feature(sTag).selection("input").set("cone_out_" + k);
                model.component("comp1").geom("geom1").feature(sTag).selection("input2").set("cone_in_" + k);

                model.component("comp1").geom("geom1").create(b1Tag, "Block");
                model.component("comp1").geom("geom1").feature(b1Tag).set("pos", new double[]{4.5, 0.0, -50.0});
                model.component("comp1").geom("geom1").feature(b1Tag).set("size", new double[]{90.0, 50.0, 100.0});

                model.component("comp1").geom("geom1").create(r1Tag, "Rotate");
                model.component("comp1").geom("geom1").feature(r1Tag).set("rot", baseRot + rotOffset);
                model.component("comp1").geom("geom1").feature(r1Tag).set("axistype", "x");
                model.component("comp1").geom("geom1").feature(r1Tag).selection("input").set(b1Tag);

                model.component("comp1").geom("geom1").create(b2Tag, "Block");
                model.component("comp1").geom("geom1").feature(b2Tag).set("pos", new double[]{4.5, 0.0, -50.0});
                model.component("comp1").geom("geom1").feature(b2Tag).set("size", new double[]{90.0, 50.0, 100.0});

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

            int nBnd = model.component("comp1").geom("geom1").getNBoundaries();
            model.component("comp1").mesh().create("mesh1");
            model.component("comp1").mesh("mesh1").autoMeshSize(5); // Faster mesh
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
                if (angDeg >= -45.0 && angDeg <= 45.0) list_c1.add(b);
            }
            int[] arr_c1 = list_c1.stream().mapToInt(i -> i).toArray();

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

            model.study().create("std1");
            model.study("std1").create("stat", "Stationary");
            model.study("std1").feature("stat").set("geometricNonlinearity", true);
            model.study("std1").feature("stat").set("useparam", true);
            model.study("std1").feature("stat").set("pname", new String[]{"P_step"});
            model.study("std1").feature("stat").set("plistarr", new String[]{"0 10 20 30 40 50"});
            model.study("std1").feature("stat").set("punit", new String[]{"kPa"});

            try {
                model.study("std1").run();
            } catch (Exception e) {
                System.out.println("Solver note: " + e.getMessage());
            }

            model.result().dataset().create("cpt_tip", "CutPoint3D");
            model.result().dataset("cpt_tip").set("data", "dset1");
            model.result().dataset("cpt_tip").set("pointx", "100");
            model.result().dataset("cpt_tip").set("pointy", "0");
            model.result().dataset("cpt_tip").set("pointz", "0");

            model.result().numerical().create("pev_tip", "EvalPoint");
            model.result().numerical("pev_tip").set("data", "cpt_tip");
            model.result().numerical("pev_tip").set("expr", new String[]{"P_step", "u", "v", "w", "solid.disp"});
            model.result().numerical("pev_tip").set("unit", new String[]{"kPa", "mm", "mm", "mm", "mm"});

            double[][] res = model.result().numerical("pev_tip").getReal();
            System.out.println("P_kPa, u_mm, v_mm, w_mm, disp_mm");
            for (int s = 0; s < res[0].length; s++) {
                System.out.printf(Locale.US, "%.1f, %.3f, %.3f, %.3f, %.3f\n",
                    res[0][s], res[1][s], res[2][s], res[3][s], res[4][s]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

