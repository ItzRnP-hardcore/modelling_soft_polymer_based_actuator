import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

public class TraceActuatorCurlingTrajectory {

    public static void main(String[] args) {
        String baseDir = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp";
        String imgDir = baseDir + "\\images";
        String csvOut = baseDir + "\\actuator_free_curling_trajectory.csv";
        String mphOut = baseDir + "\\actuator_free_curling_220mm_solved.mph";

        System.out.println("Tracing free curling trajectory of 220 mm actuator...");
        double L_act = 220.0;
        double R0 = 11.5;
        double Rt = 3.2;
        double tw_0 = 2.2;
        double tw_t = 0.65;
        double depth_0 = 2.8;
        double depth_t = 0.85;

        String[] pStepList = {"0", "20", "40", "60", "80", "100"};

        Model model = ModelUtil.create("Model");
        model.modelPath(baseDir);

        double x_start = 5.0;
        double x_end = L_act - 6.0;

        model.param().set("L_act", L_act + "[mm]");
        model.param().set("R0", R0 + "[mm]");
        model.param().set("Rt", Rt + "[mm]");
        model.param().set("tw_0", tw_0 + "[mm]");
        model.param().set("tw_t", tw_t + "[mm]");
        model.param().set("depth_0", depth_0 + "[mm]");
        model.param().set("depth_t", depth_t + "[mm]");
        model.param().set("x_start", x_start + "[mm]");
        model.param().set("x_end", x_end + "[mm]");
        model.param().set("L_cav", "x_end - x_start");

        model.param().set("r_out0", "R0 - tw_0");
        model.param().set("r_outt", "Rt - tw_t");
        model.param().set("r_in0", "r_out0 - depth_0");
        model.param().set("r_int", "r_outt - depth_t");

        model.param().set("E_mod", "0.40[MPa]");
        model.param().set("nu_val", "0.45");
        model.param().set("mu_mod", "E_mod/(2*(1+nu_val))");
        model.param().set("kappa_mod", "2*mu_mod*(1+nu_val)/(3*(1-2*nu_val))");

        model.param().set("eta_drive", "0.10");
        model.param().set("alpha_balloon", "0.005");

        model.param().set("P_step", "0[kPa]");
        model.param().set("w1", "1");
        model.param().set("w2", "0");
        model.param().set("w3", "0");
        model.param().set("w4", "0");
        model.param().set("P1", "w1*P_step");
        model.param().set("P2", "w2*P_step");
        model.param().set("P3", "w3*P_step");
        model.param().set("P4", "w4*P_step");

        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").lengthUnit("mm");

        // Actuator Outer Body
        model.component("comp1").geom("geom1").create("cone_body", "Cone");
        model.component("comp1").geom("geom1").feature("cone_body").set("axistype", "x");
        model.component("comp1").geom("geom1").feature("cone_body").set("pos", new double[]{0.0, 0.0, 0.0});
        model.component("comp1").geom("geom1").feature("cone_body").set("r", "R0");
        model.component("comp1").geom("geom1").feature("cone_body").set("specifytop", "radius");
        model.component("comp1").geom("geom1").feature("cone_body").set("rtop", "Rt");
        model.component("comp1").geom("geom1").feature("cone_body").set("h", "L_act");

        double rotOffset = 62.0;
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
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("pos", new double[]{x_start, 0.0, 0.0});
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("r", "r_out0");
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("specifytop", "radius");
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("rtop", "r_outt");
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("h", "L_cav");

            model.component("comp1").geom("geom1").create("cone_in_" + k, "Cone");
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("axistype", "x");
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("pos", new double[]{x_start, 0.0, 0.0});
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("r", "r_in0");
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("specifytop", "radius");
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("rtop", "r_int");
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("h", "L_cav");

            model.component("comp1").geom("geom1").create(sTag, "Difference");
            model.component("comp1").geom("geom1").feature(sTag).selection("input").set("cone_out_" + k);
            model.component("comp1").geom("geom1").feature(sTag).selection("input2").set("cone_in_" + k);

            model.component("comp1").geom("geom1").create(b1Tag, "Block");
            model.component("comp1").geom("geom1").feature(b1Tag).set("pos", new double[]{x_start - 0.5, 0.0, -100.0});
            model.component("comp1").geom("geom1").feature(b1Tag).set("size", new double[]{L_act, 100.0, 200.0});

            model.component("comp1").geom("geom1").create(r1Tag, "Rotate");
            model.component("comp1").geom("geom1").feature(r1Tag).set("rot", baseRot + rotOffset);
            model.component("comp1").geom("geom1").feature(r1Tag).set("axistype", "x");
            model.component("comp1").geom("geom1").feature(r1Tag).selection("input").set(b1Tag);

            model.component("comp1").geom("geom1").create(b2Tag, "Block");
            model.component("comp1").geom("geom1").feature(b2Tag).set("pos", new double[]{x_start - 0.5, 0.0, -100.0});
            model.component("comp1").geom("geom1").feature(b2Tag).set("size", new double[]{L_act, 100.0, 200.0});

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
        model.component("comp1").mesh().create("mesh1");
        model.component("comp1").mesh("mesh1").autoMeshSize(5);
        model.component("comp1").mesh("mesh1").run();

        model.result().dataset().create("dset_mesh", "Mesh");
        model.result().dataset("dset_mesh").set("mesh", "mesh1");

        int nBnd = model.component("comp1").geom("geom1").getNBoundaries();
        List<Integer> list_base = new ArrayList<>();
        List<Integer> list_act_outer = new ArrayList<>();
        List<List<Integer>> list_drive = new ArrayList<>();
        List<List<Integer>> list_balloon = new ArrayList<>();
        for (int k = 0; k < 4; k++) {
            list_drive.add(new ArrayList<>());
            list_balloon.add(new ArrayList<>());
        }

        double[][] cavAxisYZ = {{1.0, 0.0}, {0.0, 1.0}, {-1.0, 0.0}, {0.0, -1.0}};
        double r_out0_val = R0 - tw_0;
        double r_outt_val = Rt - tw_t;
        double r_in0_val = r_out0_val - depth_0;
        double r_int_val = r_outt_val - depth_t;
        double L_cav_val = x_end - x_start;

        for (int b = 1; b <= nBnd; b++) {
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

            if (Math.abs(xm) < 0.2) {
                list_base.add(b);
                continue;
            }

            double r_cone_expected = R0 - (R0 - Rt) * (xm / L_act);
            double rm = Math.hypot(ym, zm);
            if (Math.abs(rm - r_cone_expected) < 0.6) {
                list_act_outer.add(b);
                continue;
            }

            if (Math.abs(xm - L_act) < 0.2) {
                list_act_outer.add(b);
                continue;
            }

            int kCav = -1;
            for (int k = 0; k < 4; k++) {
                double dot = ym * cavAxisYZ[k][0] + zm * cavAxisYZ[k][1];
                if (dot > 0.05 * rm) {
                    kCav = k;
                    break;
                }
            }
            if (kCav < 0) {
                list_act_outer.add(b);
                continue;
            }

            if (Math.abs(xm - x_start) < 0.8) {
                list_drive.get(kCav).add(b);
                continue;
            }
            if (Math.abs(xm - x_end) < 0.8) {
                list_drive.get(kCav).add(b);
                continue;
            }

            double cosDeltaPhi = (ym * cavAxisYZ[kCav][0] + zm * cavAxisYZ[kCav][1]) / rm;
            double xi = (xm - x_start) / L_cav_val;
            double r_out_expected = r_out0_val - (r_out0_val - r_outt_val) * xi;
            double r_in_expected = r_in0_val - (r_in0_val - r_int_val) * xi;
            double r_mid_expected = 0.5 * (r_out_expected + r_in_expected);

            if (cosDeltaPhi >= 0.92 && rm < r_mid_expected) {
                list_drive.get(kCav).add(b);
            } else {
                list_balloon.get(kCav).add(b);
            }
        }

        int[] arr_base = list_base.stream().mapToInt(i -> i).toArray();

        // Physics
        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
        model.component("comp1").physics("solid").create("hmm1", "HyperelasticModel", 3);
        model.component("comp1").physics("solid").feature("hmm1").selection().set(new int[]{1});
        model.component("comp1").physics("solid").feature("hmm1").set("MaterialModel", "NeoHookean");
        model.component("comp1").physics("solid").feature("hmm1").set("Compressibility_NeoHookean", "Compressible");
        model.component("comp1").physics("solid").feature("hmm1").set("IsotropicOption", "KG");
        model.component("comp1").physics("solid").feature("hmm1").set("G_mat", "userdef");
        model.component("comp1").physics("solid").feature("hmm1").set("G", "mu_mod");
        model.component("comp1").physics("solid").feature("hmm1").set("K_mat", "userdef");
        model.component("comp1").physics("solid").feature("hmm1").set("K", "kappa_mod");

        model.component("comp1").physics("solid").create("fix_act", "Fixed", 2);
        model.component("comp1").physics("solid").feature("fix_act").selection().set(arr_base);

        for (int k = 1; k <= 4; k++) {
            int[] arr_drv = list_drive.get(k-1).stream().mapToInt(i -> i).toArray();
            int[] arr_bal = list_balloon.get(k-1).stream().mapToInt(i -> i).toArray();

            String bDrvTag = "bndl_drv" + k;
            model.component("comp1").physics("solid").create(bDrvTag, "BoundaryLoad", 2);
            model.component("comp1").physics("solid").feature(bDrvTag).selection().set(arr_drv);
            model.component("comp1").physics("solid").feature(bDrvTag).set("forceType", "FollowerPressure");
            model.component("comp1").physics("solid").feature(bDrvTag).set("pressure", "eta_drive*P" + k + "*(0.20 + 1.80*(X/L_act)^1.5)");

            String bBalTag = "bndl_bal" + k;
            model.component("comp1").physics("solid").create(bBalTag, "BoundaryLoad", 2);
            model.component("comp1").physics("solid").feature(bBalTag).selection().set(arr_bal);
            model.component("comp1").physics("solid").feature(bBalTag).set("forceType", "FollowerPressure");
            model.component("comp1").physics("solid").feature(bBalTag).set("pressure", "alpha_balloon*P" + k);
        }

        // Study
        model.study().create("std1");
        model.study("std1").create("stat", "Stationary");
        model.study("std1").feature("stat").set("geometricNonlinearity", true);
        model.study("std1").feature("stat").set("useparam", true);
        model.study("std1").feature("stat").set("pname", new String[]{"P_step"});
        model.study("std1").feature("stat").set("plistarr", new String[]{String.join(" ", pStepList)});
        model.study("std1").feature("stat").set("punit", new String[]{"kPa"});
        model.study("std1").showAutoSequences("all");

        System.out.println("Solving stationary parametric free bending study...");
        long t0 = System.currentTimeMillis();
        model.study("std1").run();
        long dt = (System.currentTimeMillis() - t0) / 1000;
        System.out.println("Solve completed in " + dt + " seconds.");

        // Extract centerline points along X from 0 to 220 mm (every 5 mm = 45 points)
        int nPts = 45;
        double[] xCoords = new double[nPts];
        double[] yCoords = new double[nPts];
        double[] zCoords = new double[nPts];
        for (int i = 0; i < nPts; i++) {
            xCoords[i] = (i / (double)(nPts - 1)) * L_act;
            yCoords[i] = 0.0;
            zCoords[i] = 0.0;
        }

        model.result().dataset().create("cpt_spine", "CutPoint3D");
        model.result().dataset("cpt_spine").set("data", "dset1");
        model.result().dataset("cpt_spine").set("pointx", xCoords);
        model.result().dataset("cpt_spine").set("pointy", yCoords);
        model.result().dataset("cpt_spine").set("pointz", zCoords);

        model.result().numerical().create("pev_spine_x", "EvalPoint");
        model.result().numerical("pev_spine_x").set("data", "cpt_spine");
        model.result().numerical("pev_spine_x").set("expr", new String[]{"x+u"});

        model.result().numerical().create("pev_spine_y", "EvalPoint");
        model.result().numerical("pev_spine_y").set("data", "cpt_spine");
        model.result().numerical("pev_spine_y").set("expr", new String[]{"y+v"});

        double[][] valX = model.result().numerical("pev_spine_x").getReal();
        double[][] valY = model.result().numerical("pev_spine_y").getReal();

        try (PrintWriter pw = new PrintWriter(new FileWriter(csvOut))) {
            pw.println("P_kPa,pt_idx,s_orig_mm,x_def_mm,y_def_mm");
            for (int col = 0; col < pStepList.length; col++) {
                double pVal = Double.parseDouble(pStepList[col]);
                for (int i = 0; i < nPts; i++) {
                    double defX = valX[i][col];
                    double defY = valY[i][col];
                    pw.printf(Locale.US, "%.1f,%d,%.2f,%.4f,%.4f%n", pVal, i, xCoords[i], defX, defY);
                }
                // Print tip position
                System.out.printf(Locale.US, ">> FREE_BEND: P=%.0f kPa -> Tip at (X=%.2f, Y=%.2f) mm%n",
                                  pVal, valX[nPts-1][col], valY[nPts-1][col]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Saved centerline trajectory CSV: " + csvOut);

        // Export 3D displacement render at peak pressure (P = 100 kPa)
        try {
            String pgD = "pg_disp";
            model.result().create(pgD, "PlotGroup3D");
            model.result(pgD).set("data", "dset1");
            model.result(pgD).setIndex("looplevel", pStepList.length - 1, 0); // P = 100 kPa
            model.result(pgD).create("surf1", "Surface");
            model.result(pgD).feature("surf1").set("expr", "solid.disp");
            model.result(pgD).feature("surf1").create("def1", "Deform");
            model.result(pgD).feature("surf1").feature("def1").set("expr", new String[]{"u", "v", "w"});
            model.result(pgD).feature("surf1").feature("def1").set("scaleactive", true);
            model.result(pgD).feature("surf1").feature("def1").set("scale", 1.0);

            String expD = "exp_disp";
            model.result().export().create(expD, "Image3D");
            model.result().export(expD).set("sourceobject", pgD);
            model.result().export(expD).set("size", "manualweb");
            model.result().export(expD).set("unit", "px");
            model.result().export(expD).set("width", 1400);
            model.result().export(expD).set("height", 900);
            String pDisp = imgDir + "\\actuator_free_curling_220mm_disp_P100.png";
            model.result().export(expD).set("pngfilename", pDisp);
            model.result().export(expD).run();
            System.out.println("Exported render: " + pDisp);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Save solved model
        try {
            model.save(mphOut);
            System.out.println("Saved model to: " + mphOut);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ModelUtil.remove("Model");
        System.out.println("Done!");
    }
}
