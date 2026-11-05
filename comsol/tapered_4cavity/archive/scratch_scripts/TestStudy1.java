import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

public class TestStudy1 {
    public static void main(String[] args) {
        try {
            Model model = ModelUtil.create("Model");
            String baseDir = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered_4cavity";
            model.modelPath(baseDir);

            // 1. Parameters
            model.param().set("L_act", "100[mm]", "Actuator total length");
            model.param().set("R0", "10[mm]", "Base outer radius");
            model.param().set("Rt", "7[mm]", "Tip outer radius");
            model.param().set("tw_out", "1.3[mm]", "Outer wall thickness");
            model.param().set("depth_cav", "3.0[mm]", "Cavity radial depth");
            model.param().set("x_start", "4[mm]", "Cavity start position");
            model.param().set("x_end", "96[mm]", "Cavity end position");
            model.param().set("L_cav", "x_end - x_start", "Cavity length");

            model.param().set("r_out0", "R0 - tw_out", "Cavity outer radius at base");
            model.param().set("r_outt", "Rt - tw_out", "Cavity outer radius at tip");
            model.param().set("r_in0", "r_out0 - depth_cav", "Cavity inner radius at base");
            model.param().set("r_int", "r_outt - depth_cav", "Cavity inner radius at tip");

            model.param().set("E_mod", "0.6[MPa]", "Young's modulus");
            model.param().set("nu_val", "0.4995", "Poisson ratio");
            model.param().set("mu_mod", "E_mod/(2*(1+nu_val))", "Shear modulus");
            model.param().set("kappa_mod", "2*mu_mod*(1+nu_val)/(3*(1-2*nu_val))", "Bulk modulus");

            model.param().set("w1", "1", "Cavity 1 weight (Top +Y)");
            model.param().set("w2", "0", "Cavity 2 weight (Right +Z)");
            model.param().set("w3", "0", "Cavity 3 weight (Bottom -Y)");
            model.param().set("w4", "0", "Cavity 4 weight (Left -Z)");

            model.param().set("P_step", "0[kPa]", "Continuation pressure parameter");
            model.param().set("P1", "w1*P_step", "Cavity 1 pressure");
            model.param().set("P2", "w2*P_step", "Cavity 2 pressure");
            model.param().set("P3", "w3*P_step", "Cavity 3 pressure");
            model.param().set("P4", "w4*P_step", "Cavity 4 pressure");

            // 2. Geometry
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

            double[] rotAngles = {0.0, -90.0, 180.0, 90.0};
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
                model.component("comp1").geom("geom1").feature(b1Tag).set("pos", new double[]{3.5, 0.0, -15.0});
                model.component("comp1").geom("geom1").feature(b1Tag).set("size", new double[]{93.0, 15.0, 30.0});

                model.component("comp1").geom("geom1").create(r1Tag, "Rotate");
                model.component("comp1").geom("geom1").feature(r1Tag).set("rot", baseRot + 40.0);
                model.component("comp1").geom("geom1").feature(r1Tag).set("axistype", "x");
                model.component("comp1").geom("geom1").feature(r1Tag).selection("input").set(b1Tag);

                model.component("comp1").geom("geom1").create(b2Tag, "Block");
                model.component("comp1").geom("geom1").feature(b2Tag).set("pos", new double[]{3.5, 0.0, -15.0});
                model.component("comp1").geom("geom1").feature(b2Tag).set("size", new double[]{93.0, 15.0, 30.0});

                model.component("comp1").geom("geom1").create(r2Tag, "Rotate");
                model.component("comp1").geom("geom1").feature(r2Tag).set("rot", baseRot - 40.0);
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

            // 3. Automated Selections
            int nBnd = model.component("comp1").geom("geom1").getNBoundaries();

            model.component("comp1").selection().create("sel_base", "Box");
            model.component("comp1").selection("sel_base").set("entitydim", 2);
            model.component("comp1").selection("sel_base").set("condition", "inside");
            model.component("comp1").selection("sel_base").set("xmin", "-0.1");
            model.component("comp1").selection("sel_base").set("xmax", "0.1");
            model.component("comp1").selection("sel_base").set("ymin", "-15");
            model.component("comp1").selection("sel_base").set("ymax", "15");
            model.component("comp1").selection("sel_base").set("zmin", "-15");
            model.component("comp1").selection("sel_base").set("zmax", "15");

            model.component("comp1").selection().create("sel_outer_box", "Cylinder");
            model.component("comp1").selection("sel_outer_box").set("entitydim", 2);
            model.component("comp1").selection("sel_outer_box").set("condition", "allvertices");
            model.component("comp1").selection("sel_outer_box").set("axistype", "x");
            model.component("comp1").selection("sel_outer_box").set("r", "10.1");
            model.component("comp1").selection("sel_outer_box").set("rin", "6.8");
            model.component("comp1").selection("sel_outer_box").set("top", "100.1");
            model.component("comp1").selection("sel_outer_box").set("bottom", "-0.1");

            model.component("comp1").selection().create("sel_outer", "Difference");
            model.component("comp1").selection("sel_outer").set("entitydim", 2);
            model.component("comp1").selection("sel_outer").set("add", new String[]{"sel_outer_box"});
            model.component("comp1").selection("sel_outer").set("subtract", new String[]{"sel_base"});

            model.component("comp1").mesh().create("mesh1");
            model.component("comp1").mesh("mesh1").autoMeshSize(5);
            model.component("comp1").mesh("mesh1").run();

            model.result().dataset().create("dset_mesh", "Mesh");
            model.result().dataset("dset_mesh").set("mesh", "mesh1");

            int[] b_base = model.component("comp1").selection("sel_base").entities(2);
            int[] b_outer = model.component("comp1").selection("sel_outer").entities(2);
            Set<Integer> extBnds = new HashSet<>();
            for (int b : b_base) extBnds.add(b);
            for (int b : b_outer) extBnds.add(b);

            List<Integer> list_c1 = new ArrayList<>();
            List<Integer> list_c2 = new ArrayList<>();
            List<Integer> list_c3 = new ArrayList<>();
            List<Integer> list_c4 = new ArrayList<>();

            for (int b = 1; b <= nBnd; b++) {
                if (extBnds.contains(b)) continue;

                model.result().numerical().create("int_b", "IntSurface");
                model.result().numerical("int_b").set("data", "dset_mesh");
                model.result().numerical("int_b").selection().set(new int[]{b});
                model.result().numerical("int_b").set("expr", new String[]{"X*1000", "Y*1000", "Z*1000", "1"});
                double[][] res = model.result().numerical("int_b").getReal();
                double area = res[3][0];
                double xm = res[0][0] / area;
                double ym = res[1][0] / area;
                double zm = res[2][0] / area;
                model.result().numerical().remove("int_b");

                if (xm > 99.0) continue;

                if (ym > Math.abs(zm)) {
                    list_c1.add(b);
                } else if (zm > Math.abs(ym)) {
                    list_c2.add(b);
                } else if (ym < -Math.abs(zm)) {
                    list_c3.add(b);
                } else if (zm < -Math.abs(ym)) {
                    list_c4.add(b);
                }
            }

            int[] arr_c1 = list_c1.stream().mapToInt(i -> i).toArray();
            int[] arr_c2 = list_c2.stream().mapToInt(i -> i).toArray();
            int[] arr_c3 = list_c3.stream().mapToInt(i -> i).toArray();
            int[] arr_c4 = list_c4.stream().mapToInt(i -> i).toArray();

            System.out.println("Cavity 1: " + Arrays.toString(arr_c1));
            System.out.println("Cavity 2: " + Arrays.toString(arr_c2));
            System.out.println("Cavity 3: " + Arrays.toString(arr_c3));
            System.out.println("Cavity 4: " + Arrays.toString(arr_c4));

            // 4. Solid Mechanics Physics
            model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
            model.component("comp1").physics("solid").create("hmm1", "HyperelasticModel", 3);
            model.component("comp1").physics("solid").feature("hmm1").selection().all();
            model.component("comp1").physics("solid").feature("hmm1").set("MaterialModel", "NeoHookean");
            model.component("comp1").physics("solid").feature("hmm1").set("Compressibility_NeoHookean", "NearlyIncompressible");
            model.component("comp1").physics("solid").feature("hmm1").set("VolumetricEnergyUncoupled", "quadratic");
            model.component("comp1").physics("solid").feature("hmm1").set("MixedFormulation", "pFormulation");
            model.component("comp1").physics("solid").feature("hmm1").set("IsotropicOption", "KG");
            model.component("comp1").physics("solid").feature("hmm1").set("G_mat", "userdef");
            model.component("comp1").physics("solid").feature("hmm1").set("G", "mu_mod");
            model.component("comp1").physics("solid").feature("hmm1").set("K_mat", "userdef");
            model.component("comp1").physics("solid").feature("hmm1").set("K", "kappa_mod");

            model.component("comp1").physics("solid").create("fix1", "Fixed", 2);
            model.component("comp1").physics("solid").feature("fix1").selection().named("sel_base");

            int[][] allCavArrays = {arr_c1, arr_c2, arr_c3, arr_c4};
            for (int k = 1; k <= 4; k++) {
                String bndTag = "bndl" + k;
                model.component("comp1").physics("solid").create(bndTag, "BoundaryLoad", 2);
                model.component("comp1").physics("solid").feature(bndTag).selection().set(allCavArrays[k-1]);
                model.component("comp1").physics("solid").feature(bndTag).set("forceType", "FollowerPressure");
                model.component("comp1").physics("solid").feature(bndTag).set("pressure", "P" + k);
            }

            // 5. Study 1 (Parametric continuation on P_step)
            model.study().create("std1");
            model.study("std1").create("stat", "Stationary");
            model.study("std1").feature("stat").set("geometricNonlinearity", true);
            model.study("std1").feature("stat").set("useparam", true);
            model.study("std1").feature("stat").set("pname", new String[]{"P_step"});
            model.study("std1").feature("stat").set("plistarr", new String[]{"0 10 20 30 40 50"});
            model.study("std1").feature("stat").set("punit", new String[]{"kPa"});

            System.out.println("Starting solve for Study 1...");
            long t0 = System.currentTimeMillis();
            model.study("std1").run();
            long t1 = System.currentTimeMillis();
            System.out.println("Solve finished in " + (t1 - t0) / 1000.0 + " s");

            // Evaluate Tip Deflection across all steps
            model.result().dataset().create("cpt_tip", "CutPoint3D");
            model.result().dataset("cpt_tip").set("data", "dset1");
            model.result().dataset("cpt_tip").set("pointx", "100");
            model.result().dataset("cpt_tip").set("pointy", "0");
            model.result().dataset("cpt_tip").set("pointz", "0");

            model.result().numerical().create("pev_tip", "EvalPoint");
            model.result().numerical("pev_tip").set("data", "cpt_tip");
            model.result().numerical("pev_tip").set("expr", new String[]{"u", "v", "w"});
            model.result().numerical("pev_tip").set("unit", new String[]{"mm", "mm", "mm"});
            double[][] uvw = model.result().numerical("pev_tip").getReal();

            double[] pSteps = {0, 10, 20, 30, 40, 50};
            System.out.println("=== STUDY 1 RESULTS (Cavity 1 Top) ===");
            System.out.println("P_kPa,u_tip_mm,v_tip_mm,w_tip_mm,delta_3d_mm,theta_deg");
            for (int i = 0; i < uvw[0].length; i++) {
                double u_t = uvw[0][i];
                double v_t = uvw[1][i];
                double w_t = uvw[2][i];
                double delta = Math.sqrt(v_t * v_t + w_t * w_t);
                double theta = Math.atan2(delta, 100.0 + u_t) * 180.0 / Math.PI;
                System.out.printf(Locale.US, "%.1f,%.4f,%.4f,%.4f,%.4f,%.2f\n",
                    pSteps[i], u_t, v_t, w_t, delta, theta);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

