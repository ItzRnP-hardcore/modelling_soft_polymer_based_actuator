import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

public class BuildActuatorTapered4CavitySleeved {
    public static Model run() {
        Model model = ModelUtil.create("Model");
        String baseDir = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered_4cavity";
        model.modelPath(baseDir);

        // 1. Actuator & Cavity Parameters
        model.param().set("L_act", "120[mm]", "Actuator total length");
        model.param().set("R0", "8.0[mm]", "Base outer radius");
        model.param().set("Rt", "4.5[mm]", "Tip outer radius");
        model.param().set("x_start", "4[mm]", "Cavity start position");
        model.param().set("x_end", "114[mm]", "Cavity end position");
        model.param().set("L_cav", "x_end - x_start", "Cavity length");

        model.param().set("tw_0", "1.0[mm]", "Outer wall thickness at base");
        model.param().set("tw_t", "0.7[mm]", "Outer wall thickness at tip");
        model.param().set("r_core0", "1.8[mm]", "Inner core radius at base");
        model.param().set("r_coret", "1.0[mm]", "Inner core radius at tip");

        model.param().set("r_out0", "R0 - tw_0", "Cavity outer radius at base");
        model.param().set("r_outt", "Rt - tw_t", "Cavity outer radius at tip");
        model.param().set("r_in0", "r_core0", "Cavity inner radius at base");
        model.param().set("r_int", "r_coret", "Cavity inner radius at tip");

        // Material Parameters (Soft Silicone - DragonSkin 10 / Ecoflex grade)
        model.param().set("E_mod", "0.30[MPa]", "Small-strain Young's modulus");
        model.param().set("nu_val", "0.45", "Poisson's ratio");
        model.param().set("ks_sleeve", "25*E_mod*R0", "Braided sleeve hoop stiffness");

        // Actuation Pressure Parameter
        model.param().set("P_step", "0[kPa]", "Continuation pressure parameter");
        model.param().set("w1", "1", "Cavity 1 (+Y Top)");
        model.param().set("w2", "0", "Cavity 2 (+Z Right)");
        model.param().set("w3", "0", "Cavity 3 (-Y Bottom)");
        model.param().set("w4", "0", "Cavity 4 (-Z Left)");
        model.param().set("P1", "w1*P_step", "Cavity 1 pressure");
        model.param().set("P2", "w2*P_step", "Cavity 2 pressure");
        model.param().set("P3", "w3*P_step", "Cavity 3 pressure");
        model.param().set("P4", "w4*P_step", "Cavity 4 pressure");

        // 2. Geometry: 3D Tapered Cone with 4 Arc Cavities (80 deg) and 4 Radial Ribs (10 deg)
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

        double rotOffset = 50.0; // 80 deg arc cavity wedge
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
            model.component("comp1").geom("geom1").feature(b1Tag).set("size", new double[]{115.0, 50.0, 100.0});

            model.component("comp1").geom("geom1").create(r1Tag, "Rotate");
            model.component("comp1").geom("geom1").feature(r1Tag).set("rot", baseRot + rotOffset);
            model.component("comp1").geom("geom1").feature(r1Tag).set("axistype", "x");
            model.component("comp1").geom("geom1").feature(r1Tag).selection("input").set(b1Tag);

            model.component("comp1").geom("geom1").create(b2Tag, "Block");
            model.component("comp1").geom("geom1").feature(b2Tag).set("pos", new double[]{3.5, 0.0, -50.0});
            model.component("comp1").geom("geom1").feature(b2Tag).set("size", new double[]{115.0, 50.0, 100.0});

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

        // 3. Mesh & Automated Boundary Classification
        int nBnd = model.component("comp1").geom("geom1").getNBoundaries();
        model.component("comp1").mesh().create("mesh1");
        model.component("comp1").mesh("mesh1").autoMeshSize(4);
        model.component("comp1").mesh("mesh1").run();

        model.result().dataset().create("dset_mesh", "Mesh");
        model.result().dataset("dset_mesh").set("mesh", "mesh1");

        List<Integer> list_c1 = new ArrayList<>();
        List<Integer> list_c2 = new ArrayList<>();
        List<Integer> list_c3 = new ArrayList<>();
        List<Integer> list_c4 = new ArrayList<>();
        List<Integer> list_outer = new ArrayList<>();

        for (int b = 1; b <= nBnd; b++) {
            if (b == 1 || b == nBnd) continue;
            if (b >= 2 && b <= 5) {
                list_outer.add(b);
                continue;
            }

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
            } else if (angDeg > 45.0 && angDeg < 135.0) {
                list_c2.add(b); // Cavity 2: Right (+Z)
            } else if (Math.abs(angDeg) >= 135.0) {
                list_c3.add(b); // Cavity 3: Bottom (-Y)
            } else {
                list_c4.add(b); // Cavity 4: Left (-Z)
            }
        }

        int[] arr_c1 = list_c1.stream().mapToInt(i -> i).toArray();
        int[] arr_c2 = list_c2.stream().mapToInt(i -> i).toArray();
        int[] arr_c3 = list_c3.stream().mapToInt(i -> i).toArray();
        int[] arr_c4 = list_c4.stream().mapToInt(i -> i).toArray();
        int[] arr_outer = list_outer.stream().mapToInt(i -> i).toArray();

        System.out.println("Tapered Sleeved 4-Cavity Setup:");
        System.out.println("  Cavity 1 (+Y Top):   " + arr_c1.length + " boundaries");
        System.out.println("  Cavity 2 (+Z Right): " + arr_c2.length + " boundaries");
        System.out.println("  Cavity 3 (-Y Bot):   " + arr_c3.length + " boundaries");
        System.out.println("  Cavity 4 (-Z Left):  " + arr_c4.length + " boundaries");
        System.out.println("  Outer Sleeve Faces:  " + arr_outer.length + " boundaries: " + Arrays.toString(arr_outer));

        // 4. Solid Mechanics Physics
        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
        model.component("comp1").physics("solid").feature("lemm1").set("E_mat", "userdef");
        model.component("comp1").physics("solid").feature("lemm1").set("E", "E_mod");
        model.component("comp1").physics("solid").feature("lemm1").set("nu_mat", "userdef");
        model.component("comp1").physics("solid").feature("lemm1").set("nu", "nu_val");

        // Fixed base boundary at X = 0
        model.component("comp1").physics("solid").create("fix1", "Fixed", 2);
        model.component("comp1").physics("solid").feature("fix1").selection().set(new int[]{1});

        // 4 Cavity Follower Pressure Boundary Loads
        int[][] allCavs = {arr_c1, arr_c2, arr_c3, arr_c4};
        for (int k = 1; k <= 4; k++) {
            String bTag = "bndl" + k;
            model.component("comp1").physics("solid").create(bTag, "BoundaryLoad", 2);
            model.component("comp1").physics("solid").feature(bTag).selection().set(allCavs[k-1]);
            model.component("comp1").physics("solid").feature(bTag).set("forceType", "FollowerPressure");
            model.component("comp1").physics("solid").feature(bTag).set("pressure", "P" + k);
        }

        // 5. Outer Hoop-Reinforcing Braided Sleeve (Prevents Ballooning, Forces Longitudinal Bending)
        model.component("comp1").variable().create("var_sleeve");
        model.component("comp1").variable("var_sleeve").selection().geom("geom1", 2);
        model.component("comp1").variable("var_sleeve").selection().set(arr_outer);
        model.component("comp1").variable("var_sleeve").set("Rr", "sqrt(Y^2+Z^2)");
        model.component("comp1").variable("var_sleeve").set("eh", "(-Z*vTY + Y*wTZ)/Rr");
        model.component("comp1").physics("solid").create("wf_sleeve", "WeakContribution", 2);
        model.component("comp1").physics("solid").feature("wf_sleeve").selection().set(arr_outer);
        model.component("comp1").physics("solid").feature("wf_sleeve").set("weakExpression", "-ks_sleeve*eh*test(eh)");

        // 6. Parametric Continuation Study (0 to 100 kPa)
        model.study().create("std1");
        model.study("std1").create("stat", "Stationary");
        model.study("std1").feature("stat").set("geometricNonlinearity", true);
        model.study("std1").feature("stat").set("useparam", true);
        model.study("std1").feature("stat").set("pname", new String[]{"P_step"});
        model.study("std1").feature("stat").set("plistarr", new String[]{"0 10 20 30 40 50 60 70 80 90 100"});
        model.study("std1").feature("stat").set("punit", new String[]{"kPa"});

        System.out.println("=== SOLVING TAPERED SLEEVED 4-CAVITY ACTUATOR (0..100 kPa) ===");
        long t0 = System.currentTimeMillis();
        try {
            model.study("std1").run();
            long t1 = System.currentTimeMillis();
            System.out.println("COMSOL solve completed in " + (t1 - t0) / 1000.0 + " s");
        } catch (Exception ex) {
            System.out.println("Solver note: " + ex.getMessage());
        }

        // 7. Post-Processing Datasets & Evaluations
        model.result().dataset().create("cpt_tip", "CutPoint3D");
        model.result().dataset("cpt_tip").set("data", "dset1");
        model.result().dataset("cpt_tip").set("pointx", "120");
        model.result().dataset("cpt_tip").set("pointy", "0");
        model.result().dataset("cpt_tip").set("pointz", "0");

        model.result().numerical().create("pev_tip", "EvalPoint");
        model.result().numerical("pev_tip").set("data", "cpt_tip");
        model.result().numerical("pev_tip").set("expr", new String[]{"P_step", "u", "v", "w", "solid.disp"});
        model.result().numerical("pev_tip").set("unit", new String[]{"kPa", "mm", "mm", "mm", "mm"});

        model.result().dataset().create("cpl_mid", "CutPlane");
        model.result().dataset("cpl_mid").set("data", "dset1");
        model.result().dataset("cpl_mid").set("quickplane", "yz");
        model.result().dataset("cpl_mid").set("quickx", "55[mm]");

        // Plot Group 1: 3D Total Displacement (solid.disp)
        model.result().create("pg_disp", "PlotGroup3D");
        model.result("pg_disp").set("data", "dset1");
        model.result("pg_disp").label("Total Displacement (solid.disp) - Sleeved Tapered");
        model.result("pg_disp").create("surf_d", "Surface");
        model.result("pg_disp").feature("surf_d").set("expr", "solid.disp");
        model.result("pg_disp").feature("surf_d").set("unit", "mm");
        model.result("pg_disp").feature("surf_d").create("def", "Deform");
        model.result("pg_disp").feature("surf_d").feature("def").set("expr", new String[]{"u", "v", "w"});
        model.result("pg_disp").feature("surf_d").feature("def").set("scaleactive", true);
        model.result("pg_disp").feature("surf_d").feature("def").set("scale", 1);

        // Plot Group 2: 3D von Mises Stress (solid.mises)
        model.result().create("pg_stress", "PlotGroup3D");
        model.result("pg_stress").set("data", "dset1");
        model.result("pg_stress").label("3D Stress (solid.mises) - Sleeved Tapered");
        model.result("pg_stress").create("surf_s", "Surface");
        model.result("pg_stress").feature("surf_s").set("expr", "solid.mises");
        model.result("pg_stress").feature("surf_s").set("unit", "kPa");
        model.result("pg_stress").feature("surf_s").create("def", "Deform");
        model.result("pg_stress").feature("surf_s").feature("def").set("expr", new String[]{"u", "v", "w"});
        model.result("pg_stress").feature("surf_s").feature("def").set("scaleactive", true);
        model.result("pg_stress").feature("surf_s").feature("def").set("scale", 1);

        // Plot Group 3: 3D Transverse Slices
        model.result().create("pg_slice3d", "PlotGroup3D");
        model.result("pg_slice3d").set("data", "dset1");
        model.result("pg_slice3d").label("3D Transverse Slices - Cavities & Ribs");
        model.result("pg_slice3d").create("slc1", "Slice");
        model.result("pg_slice3d").feature("slc1").set("expr", "solid.mises");
        model.result("pg_slice3d").feature("slc1").set("unit", "kPa");
        model.result("pg_slice3d").feature("slc1").set("quickplane", "yz");
        model.result("pg_slice3d").feature("slc1").set("quickxnumber", 6);
        model.result("pg_slice3d").feature("slc1").create("def", "Deform");
        model.result("pg_slice3d").feature("slc1").feature("def").set("expr", new String[]{"u", "v", "w"});
        model.result("pg_slice3d").feature("slc1").feature("def").set("scaleactive", true);
        model.result("pg_slice3d").feature("slc1").feature("def").set("scale", 1);

        // Plot Group 4: 2D Mid-Cross Section (X = 55 mm)
        model.result().create("pg_sec2d", "PlotGroup2D");
        model.result("pg_sec2d").set("data", "cpl_mid");
        model.result("pg_sec2d").label("2D Mid-Cross Section (X=55 mm) - 4 Arc Cavities & Ribs");
        model.result("pg_sec2d").create("surf_sec", "Surface");
        model.result("pg_sec2d").feature("surf_sec").set("expr", "solid.mises");
        model.result("pg_sec2d").feature("surf_sec").set("unit", "kPa");
        model.result("pg_sec2d").feature("surf_sec").set("colortable", "RainbowLight");
        model.result("pg_sec2d").feature("surf_sec").create("def", "Deform");
        model.result("pg_sec2d").feature("surf_sec").feature("def").set("expr", new String[]{"v", "w"});
        model.result("pg_sec2d").feature("surf_sec").feature("def").set("scaleactive", true);
        model.result("pg_sec2d").feature("surf_sec").feature("def").set("scale", 1);

        // Evaluate Results Across Steps
        double[][] uvw = model.result().numerical("pev_tip").getReal();
        int nSteps = uvw[0].length;
        System.out.println("=== TAPERED SLEEVED 4-CAVITY RESULTS ===");
        System.out.println("P_kPa,u_tip_mm,v_tip_mm,w_tip_mm,delta_3d_mm,theta_deg");

        List<String> csvLines = new ArrayList<>();
        csvLines.add("P_kPa,u_tip_mm,v_tip_mm,w_tip_mm,delta_3d_mm,theta_deg");

        for (int s = 0; s < nSteps; s++) {
            double p = uvw[0][s];
            double u = uvw[1][s];
            double v = uvw[2][s];
            double w = uvw[3][s];
            double d3 = Math.sqrt(v * v + w * w);
            double theta = Math.atan2(d3, 120.0 + u) * 180.0 / Math.PI;

            String row = String.format(Locale.US, "%.1f,%.4f,%.4f,%.4f,%.4f,%.2f", p, u, v, w, d3, theta);
            System.out.println("STEP: " + row);
            csvLines.add(row);
        }

        try {
            File csvFile = new File(baseDir + "\\comsol_tapered_sleeved_summary.csv");
            PrintWriter pw = new PrintWriter(new FileWriter(csvFile));
            for (String line : csvLines) pw.println(line);
            pw.close();
            System.out.println("Saved summary CSV to: " + csvFile.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("CSV save error: " + e.getMessage());
        }

        // Export Images at Max Converged Step
        try {
            model.result("pg_disp").set("solnum", String.valueOf(nSteps));
            model.result("pg_disp").run();
            model.result().export().create("exp_d", "Image3D");
            model.result().export("exp_d").set("sourceobject", "pg_disp");
            model.result().export("exp_d").set("filename", baseDir + "\\comsol_tapered_sleeved_disp.png");
            model.result().export("exp_d").set("size", "manualweb");
            model.result().export("exp_d").set("unit", "px");
            model.result().export("exp_d").set("width", 1280);
            model.result().export("exp_d").set("height", 720);
            model.result().export("exp_d").set("colortheme", "default");
            model.result().export("exp_d").run();

            model.result("pg_stress").set("solnum", String.valueOf(nSteps));
            model.result("pg_stress").run();
            model.result().export().create("exp_s", "Image3D");
            model.result().export("exp_s").set("sourceobject", "pg_stress");
            model.result().export("exp_s").set("filename", baseDir + "\\comsol_tapered_sleeved_stress.png");
            model.result().export("exp_s").set("size", "manualweb");
            model.result().export("exp_s").set("unit", "px");
            model.result().export("exp_s").set("width", 1280);
            model.result().export("exp_s").set("height", 720);
            model.result().export("exp_s").set("colortheme", "default");
            model.result().export("exp_s").run();

            model.result("pg_slice3d").set("solnum", String.valueOf(nSteps));
            model.result("pg_slice3d").run();
            model.result().export().create("exp_slc", "Image3D");
            model.result().export("exp_slc").set("sourceobject", "pg_slice3d");
            model.result().export("exp_slc").set("filename", baseDir + "\\comsol_tapered_sleeved_slice3d_stress.png");
            model.result().export("exp_slc").set("size", "manualweb");
            model.result().export("exp_slc").set("unit", "px");
            model.result().export("exp_slc").set("width", 1280);
            model.result().export("exp_slc").set("height", 720);
            model.result().export("exp_slc").set("colortheme", "default");
            model.result().export("exp_slc").run();

            model.result("pg_sec2d").set("solnum", String.valueOf(nSteps));
            model.result("pg_sec2d").run();
            model.result().export().create("exp_sec", "Image2D");
            model.result().export("exp_sec").set("sourceobject", "pg_sec2d");
            model.result().export("exp_sec").set("filename", baseDir + "\\comsol_tapered_sleeved_sec2d_stress.png");
            model.result().export("exp_sec").set("size", "manualweb");
            model.result().export("exp_sec").set("unit", "px");
            model.result().export("exp_sec").set("width", 1200);
            model.result().export("exp_sec").set("height", 1000);
            model.result().export("exp_sec").set("colortheme", "default");
            model.result().export("exp_sec").run();
            System.out.println("Exported all 4 COMSOL render images successfully!");
        } catch (Exception e) {
            System.out.println("Image export note: " + e.getMessage());
        }

        // Save Solved Model Files
        try {
            model.save(baseDir + "\\actuator_tapered_4cavity_sleeved_solved.mph");
            System.out.println("Saved model to: actuator_tapered_4cavity_sleeved_solved.mph");
        } catch (Exception e) {
            System.out.println("Save error (sleeved): " + e.getMessage());
        }

        try {
            model.save(baseDir + "\\actuator_tapered_4cavity_solved.mph");
            System.out.println("Saved model to: actuator_tapered_4cavity_solved.mph");
        } catch (Exception e) {
            System.out.println("Save error (solved): " + e.getMessage());
        }

        return model;
    }

    public static void main(String[] args) {
        run();
    }
}

