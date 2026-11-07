import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

/**
 * BuildLoopCagingR8mmSweep.java
 *
 * Dedicated COMSOL 6.4 batch study to achieve TRUE CAGING LOOP grasping:
 * 1. Fixed small cylinder radius R_cyl = 8.0 mm (as instructed: do not vary cylinder radius).
 * 2. Sweeps candidate locations:
 *    - Pos 1: (Xc = 48.0, Yc = -42.0) mm
 *    - Pos 2: (Xc = 48.0, Yc = -44.0) mm
 *    - Pos 3: (Xc = 46.0, Yc = -44.0) mm
 * 3. Sweeps pressure P = 0, 20, 40, 60, 80, 100 kPa.
 * 4. Extracts contact forces (Fn, Ffric), peak contact pressure (MaxTn), peak stress (VmMax),
 *    and floating cylinder displacements.
 * 5. Exports 3D displacement and stress renders at P = 60 kPa and P = 100 kPa.
 */
public class BuildLoopCagingR8mmSweep {

    public static void main(String[] args) {
        String baseDir = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp";
        String imgDir = baseDir + "\\images";

        double fixedRcyl = 8.0;

        String[] caseNames = {
            "LoopCaging_R8mm_Pos1_Xc36_Yc-46",
            "LoopCaging_R8mm_Pos2_Xc34_Yc-48",
            "LoopCaging_R8mm_Pos3_Xc32_Yc-52"
        };

        double[] testXc = {36.0, 34.0, 32.0};
        double[] testYc = {-46.0, -48.0, -52.0};

        String[] pStepList = {"0", "20", "40", "60", "70", "80", "85", "90", "95", "100"};

        System.out.println("================================================================================");
        System.out.println("  COMSOL 6.4 LOOP CAGING R=8mm POSITION & CLAMPING FORCE STUDY");
        System.out.println("  Fixed Cylinder Radius = " + fixedRcyl + " mm");
        System.out.println("================================================================================");

        double L_act = 220.0;
        double R0 = 11.5;
        double Rt = 3.2;
        double tw_0 = 2.2;
        double tw_t = 0.65;
        double depth_0 = 2.8;
        double depth_t = 0.85;

        for (int c = 0; c < caseNames.length; c++) {
            String cName = caseNames[c];
            double xCyl = testXc[c];
            double yCyl = testYc[c];

            System.out.printf(Locale.US, "%n>>> SOLVING CASE %d: %s | R_cyl=%.1f mm | (Xc=%.1f, Yc=%.1f) mm <<<%n",
                              c, cName, fixedRcyl, xCyl, yCyl);

            Model model = null;
            try {
                model = buildModel(baseDir, L_act, R0, Rt, tw_0, tw_t, depth_0, depth_t, fixedRcyl, xCyl, yCyl, pStepList);

                // Configure solver features
                try {
                    model.sol("sol1").feature("s1").feature("fc1").set("maxiter", 50);
                    model.sol("sol1").feature("s1").feature("p1").set("pcontinuation", "P_step");
                    model.sol("sol1").feature("s1").feature("p1").set("prestart", "on");
                } catch (Exception ignored) {}

                long t0 = System.currentTimeMillis();
                try {
                    model.study("std1").run();
                } catch (Exception eRun) {
                    System.out.println("Notice: Study run stopped at: " + eRun.getMessage());
                }
                long dt = (System.currentTimeMillis() - t0) / 1000;
                System.out.println("Solve phase completed in " + dt + " seconds.");

                double[] solvedP = model.sol("sol1").getPVals();
                int nSolved = (solvedP != null) ? solvedP.length : 0;
                System.out.println("Total converged parameter steps: " + nSolved);

                if (nSolved == 0) {
                    System.err.println("No parameters solved for case: " + cName);
                    continue;
                }

                // Evaluators
                double[][] resTn = model.result().numerical("int_Tn_dst").getReal();
                double[][] resMaxTn = model.result().numerical("max_Tn_dst").getReal();
                double[][] resVm = model.result().numerical("max_vm").getReal();
                double[][] resCylU = model.result().numerical("pev_cyl").getReal();
                double[][] resTip = model.result().numerical("pev_tip").getReal();
                double[][] resPy = model.result().numerical("pev_py").getReal();
                double[][] resNy = model.result().numerical("pev_ny").getReal();

                for (int s = 0; s < nSolved; s++) {
                    double p = solvedP[s] / 1000.0; // convert Pa to kPa
                    double uc = resCylU[0][s];
                    double vc = resCylU[1][s];
                    double fn = resTn[0][s];
                    double maxTn = resMaxTn[0][s];
                    double vmMax = resVm[0][s];
                    double dispMag = Math.sqrt(uc * uc + vc * vc);
                    double fFric = 0.50 * fn;

                    double uTip = resTip[0][s];
                    double vTip = resTip[1][s];
                    double xTip = L_act + uTip;
                    double yTip = vTip;

                    double uPy = resPy[0][s];
                    double vPy = resPy[1][s];
                    double uNy = resNy[0][s];
                    double vNy = resNy[1][s];

                    double dxSec = (L_act + uPy) - (L_act + uNy);
                    double dySec = (Rt + vPy) - (-Rt + vNy);
                    double secAngle = Math.atan2(dySec, dxSec) * (180.0 / Math.PI);
                    double tipAngle = 90.0 - secAngle;
                    if (tipAngle < 0.0 && p >= 20.0) tipAngle += 360.0;
                    double wrapAngle = tipAngle;
                    double circumferenceEnclosure = Math.min(100.0, (wrapAngle / 360.0) * 100.0);

                    System.out.printf(Locale.US,
                        ">> LOOP_R8_ROW: Case=%s, R_cyl=%.1f, Xc=%.1f, Yc=%.1f, P=%.1f, Fn=%.5f, Ffric=%.5f, MaxTn=%.2f, VmMax=%.2f, ux=%.3f, uy=%.3f, uMag=%.3f, xTip=%.1f, yTip=%.1f, WrapAngle=%.1f, Enclosure=%.1f%%%n",
                        cName, fixedRcyl, xCyl, yCyl, p, fn, fFric, maxTn, vmMax, uc, vc, dispMag, xTip, yTip, wrapAngle, circumferenceEnclosure);
                }

                int midIdx = Math.max(0, nSolved / 2);
                int lastIdx = nSolved - 1;
                double pLast = solvedP[lastIdx] / 1000.0;
                double pMid = solvedP[midIdx] / 1000.0;

                // 1. Export 3D displacement render at intermediate pressure
                String pgD60 = "pg_disp60_" + c;
                model.result().create(pgD60, "PlotGroup3D");
                model.result(pgD60).set("data", "dset1");
                model.result(pgD60).setIndex("looplevel", midIdx, 0);
                model.result(pgD60).create("surf1", "Surface");
                model.result(pgD60).feature("surf1").set("expr", "solid.disp");
                model.result(pgD60).feature("surf1").create("def1", "Deform");
                model.result(pgD60).feature("surf1").feature("def1").set("expr", new String[]{"u", "v", "w"});
                model.result(pgD60).feature("surf1").feature("def1").set("scaleactive", true);
                model.result(pgD60).feature("surf1").feature("def1").set("scale", 1.0);

                String expD60 = "exp_disp60_" + c;
                model.result().export().create(expD60, "Image3D");
                model.result().export(expD60).set("sourceobject", pgD60);
                model.result().export(expD60).set("size", "manualweb");
                model.result().export(expD60).set("unit", "px");
                model.result().export(expD60).set("width", 1400);
                model.result().export(expD60).set("height", 900);
                String pDisp60 = String.format(Locale.US, "%s\\actuator_%s_disp_P%.0f.png", imgDir, cName, pMid);
                model.result().export(expD60).set("pngfilename", pDisp60);
                model.result().export(expD60).run();
                System.out.println("Exported render: " + pDisp60);

                // 2. Export 3D displacement render at peak converged pressure
                String pgD = "pg_disp_" + c;
                model.result().create(pgD, "PlotGroup3D");
                model.result(pgD).set("data", "dset1");
                model.result(pgD).setIndex("looplevel", lastIdx, 0);
                model.result(pgD).create("surf1", "Surface");
                model.result(pgD).feature("surf1").set("expr", "solid.disp");
                model.result(pgD).feature("surf1").create("def1", "Deform");
                model.result(pgD).feature("surf1").feature("def1").set("expr", new String[]{"u", "v", "w"});
                model.result(pgD).feature("surf1").feature("def1").set("scaleactive", true);
                model.result(pgD).feature("surf1").feature("def1").set("scale", 1.0);

                String expD = "exp_disp_" + c;
                model.result().export().create(expD, "Image3D");
                model.result().export(expD).set("sourceobject", pgD);
                model.result().export(expD).set("size", "manualweb");
                model.result().export(expD).set("unit", "px");
                model.result().export(expD).set("width", 1400);
                model.result().export(expD).set("height", 900);
                String pDisp = String.format(Locale.US, "%s\\actuator_%s_disp_P%.0f.png", imgDir, cName, pLast);
                model.result().export(expD).set("pngfilename", pDisp);
                model.result().export(expD).run();
                System.out.println("Exported render: " + pDisp);

                // 3. Export 3D stress render at peak converged pressure
                String pgS = "pg_stress_" + c;
                model.result().create(pgS, "PlotGroup3D");
                model.result(pgS).set("data", "dset1");
                model.result(pgS).setIndex("looplevel", lastIdx, 0);
                model.result(pgS).create("surf1", "Surface");
                model.result(pgS).feature("surf1").set("expr", "solid.mises");
                model.result(pgS).feature("surf1").create("def1", "Deform");
                model.result(pgS).feature("surf1").feature("def1").set("expr", new String[]{"u", "v", "w"});
                model.result(pgS).feature("surf1").feature("def1").set("scaleactive", true);
                model.result(pgS).feature("surf1").feature("def1").set("scale", 1.0);

                String expS = "exp_stress_" + c;
                model.result().export().create(expS, "Image3D");
                model.result().export(expS).set("sourceobject", pgS);
                model.result().export(expS).set("size", "manualweb");
                model.result().export(expS).set("unit", "px");
                model.result().export(expS).set("width", 1400);
                model.result().export(expS).set("height", 900);
                String pStress = String.format(Locale.US, "%s\\actuator_%s_stress_P%.0f.png", imgDir, cName, pLast);
                model.result().export(expS).set("pngfilename", pStress);
                model.result().export(expS).run();
                System.out.println("Exported render: " + pStress);

                // Save solved model
                String savePath = String.format("%s\\actuator_%s_solved.mph", baseDir, cName);
                System.out.println("Saving model: " + savePath);
                model.save(savePath);
                System.out.println("Saved successfully.");

            } catch (Exception e) {
                System.err.println("ERROR in Case " + cName + ": " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (model != null) {
                    try {
                        ModelUtil.remove("Model");
                    } catch (Exception ignored) {}
                }
            }
        }

        System.out.println("Loop caging R=8mm sweep complete!");
    }

    public static Model buildModel(String baseDir, double L_act, double R0, double Rt,
                                  double tw_0, double tw_t, double depth_0, double depth_t,
                                  double rCyl, double X_cyl, double Y_cyl, String[] pStepList) {
        Model model = ModelUtil.create("Model");
        model.modelPath(baseDir);

        double x_start = 5.0;
        double x_end = L_act - 6.0;
        double L_cyl = 50.0;

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

        model.param().set("R_cyl", rCyl + "[mm]");
        model.param().set("X_cyl", X_cyl + "[mm]");
        model.param().set("Y_cyl", Y_cyl + "[mm]");
        model.param().set("L_cyl", L_cyl + "[mm]");

        model.param().set("E_mod", "0.40[MPa]");
        model.param().set("nu_val", "0.45");
        model.param().set("mu_mod", "E_mod/(2*(1+nu_val))");
        model.param().set("kappa_mod", "2*mu_mod*(1+nu_val)/(3*(1-2*nu_val))");

        model.param().set("eta_drive", "0.10");
        model.param().set("alpha_balloon", "0.005");
        model.param().set("mu_fric", "0.50");

        // Compliant floating spring stiffness
        model.param().set("k_float", "25[N/m]", "Floating object compliance spring stiffness");
        model.param().set("kth_float", "0.01[N*m/rad]", "Floating object rotational compliance stiffness");

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

        // 1. Actuator Outer Body
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

        // 2. Target Cylinder
        model.component("comp1").geom("geom1").create("target_cyl", "Cylinder");
        model.component("comp1").geom("geom1").feature("target_cyl").set("axistype", "z");
        model.component("comp1").geom("geom1").feature("target_cyl").set("pos", new double[]{X_cyl, Y_cyl, -L_cyl/2.0});
        model.component("comp1").geom("geom1").feature("target_cyl").set("r", "R_cyl");
        model.component("comp1").geom("geom1").feature("target_cyl").set("h", "L_cyl");

        model.component("comp1").geom("geom1").feature("fin").set("action", "assembly");
        model.component("comp1").geom("geom1").feature("fin").set("createpairs", true);
        model.component("comp1").geom("geom1").run();

        // 3. Mesh
        model.component("comp1").mesh().create("mesh1");
        model.component("comp1").mesh("mesh1").autoMeshSize(5);
        model.component("comp1").mesh("mesh1").run();

        model.result().dataset().create("dset_mesh", "Mesh");
        model.result().dataset("dset_mesh").set("mesh", "mesh1");

        int nBnd = model.component("comp1").geom("geom1").getNBoundaries();
        List<Integer> list_base = new ArrayList<>();
        List<Integer> list_act_outer = new ArrayList<>();
        List<Integer> list_cyl_bnds = new ArrayList<>();
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
            model.result().numerical("int_b").set("expr", new String[]{
                "X", "Y", "Z", "1",
                "(sqrt((Y*1000)^2+(Z*1000)^2) - (11.5 - (11.5-3.2)*((X*1000)/220.0)))^2"
            });
            double[][] res = model.result().numerical("int_b").getReal();
            double area = res[3][0];
            double xm = (res[0][0] / area) * 1000.0;
            double ym = (res[1][0] / area) * 1000.0;
            double zm = (res[2][0] / area) * 1000.0;
            double rm = Math.hypot(ym, zm);
            double mean_sq_err = res[4][0] / area; // in mm^2
            model.result().numerical().remove("int_b");

            // Check Cylinder
            double distCylAxis = Math.hypot(xm - X_cyl, ym - Y_cyl);
            if (distCylAxis <= rCyl + 0.8 && Math.abs(zm) <= L_cyl/2.0 + 0.8) {
                list_cyl_bnds.add(b);
                continue;
            }

            // Check Base
            if (Math.abs(xm) < 0.2) {
                list_base.add(b);
                continue;
            }

            // Check Tip Cap
            if (Math.abs(xm - L_act) < 0.2) {
                list_act_outer.add(b);
                continue;
            }

            // Check True Outer Skin
            if (mean_sq_err < 0.05) {
                list_act_outer.add(b);
                continue;
            }

            // Internal Cavity
            int kCav = -1;
            if (ym > Math.abs(zm)) kCav = 0;
            else if (zm > Math.abs(ym)) kCav = 1;
            else if (ym < -Math.abs(zm)) kCav = 2;
            else kCav = 3;

            if (Math.abs(xm - x_start) < 0.8 || Math.abs(xm - x_end) < 0.8) {
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
        int[] arr_cyl = list_cyl_bnds.stream().mapToInt(i -> i).toArray();
        int[] arr_act_outer = list_act_outer.stream().mapToInt(i -> i).toArray();

        System.out.printf("Identified: %d cylinder bnds, %d act outer bnds, %d base bnds.%n",
                          arr_cyl.length, arr_act_outer.length, arr_base.length);

        // Contact Pair
        model.pair().create("p1", "Contact", "geom1");
        model.pair("p1").source().set(arr_cyl);
        model.pair("p1").destination().set(arr_act_outer);

        // Physics: Solid Mechanics
        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");

        // Actuator Domain (Domain 1): Hyperelastic Neo-Hookean
        model.component("comp1").physics("solid").create("hmm1", "HyperelasticModel", 3);
        model.component("comp1").physics("solid").feature("hmm1").selection().set(new int[]{1});
        model.component("comp1").physics("solid").feature("hmm1").set("MaterialModel", "NeoHookean");
        model.component("comp1").physics("solid").feature("hmm1").set("Compressibility_NeoHookean", "Compressible");
        model.component("comp1").physics("solid").feature("hmm1").set("IsotropicOption", "KG");
        model.component("comp1").physics("solid").feature("hmm1").set("G_mat", "userdef");
        model.component("comp1").physics("solid").feature("hmm1").set("G", "mu_mod");
        model.component("comp1").physics("solid").feature("hmm1").set("K_mat", "userdef");
        model.component("comp1").physics("solid").feature("hmm1").set("K", "kappa_mod");

        // Fixed Base
        model.component("comp1").physics("solid").create("fix_act", "Fixed", 2);
        model.component("comp1").physics("solid").feature("fix_act").selection().set(arr_base);

        // Floating Rigid Cylinder (Domain 2) with SpringFoundation
        model.component("comp1").physics("solid").create("rd_cyl", "RigidDomain", 3);
        model.component("comp1").physics("solid").feature("rd_cyl").selection().set(new int[]{2});
        model.component("comp1").physics("solid").feature("rd_cyl").set("rho_mat", "userdef");
        model.component("comp1").physics("solid").feature("rd_cyl").set("rho", "1000[kg/m^3]");

        model.component("comp1").physics("solid").feature("rd_cyl").create("spf_cyl", "SpringFoundation");
        model.component("comp1").physics("solid").feature("rd_cyl").feature("spf_cyl").set("SpringType", "ku");
        model.component("comp1").physics("solid").feature("rd_cyl").feature("spf_cyl").set("ku", new String[]{"k_float", "k_float", "1000[N/m]"});
        model.component("comp1").physics("solid").feature("rd_cyl").feature("spf_cyl").set("RotSpringType", "kth");
        model.component("comp1").physics("solid").feature("rd_cyl").feature("spf_cyl").set("kth", new String[]{"1[N*m/rad]", "1[N*m/rad]", "kth_float"});

        // Actuator Pressure Loads (Pitch Down on Cavity 1)
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

        // Contact Pair
        model.component("comp1").physics("solid").create("cnt1", "Contact", 2);
        model.component("comp1").physics("solid").feature("cnt1").set("pairSelection", "all");
        model.component("comp1").physics("solid").feature("cnt1").set("ContactMethodCtrl", "Penalty");
        model.component("comp1").physics("solid").feature("cnt1").set("penaltyCtrl", "userDefined");
        model.component("comp1").physics("solid").feature("cnt1").set("pn_penalty", "1e6[N/m^3]");

        model.component("comp1").physics("solid").feature("cnt1").create("fric1", "Friction");
        model.component("comp1").physics("solid").feature("cnt1").feature("fric1").set("FrictionModel", "StaticFriction");
        model.component("comp1").physics("solid").feature("cnt1").feature("fric1").set("mustat", "mu_fric");
        model.component("comp1").physics("solid").feature("cnt1").feature("fric1").set("FrictionMethodCtrl", "Penalty");
        model.component("comp1").physics("solid").feature("cnt1").feature("fric1").set("penaltyCtrlFriction", "userDefined");
        model.component("comp1").physics("solid").feature("cnt1").feature("fric1").set("pt_penalty", "1e6[N/m^3]");

        // Study
        model.study().create("std1");
        model.study("std1").create("stat", "Stationary");
        model.study("std1").feature("stat").set("geometricNonlinearity", true);
        model.study("std1").feature("stat").set("useparam", true);
        model.study("std1").feature("stat").set("pname", new String[]{"P_step"});
        model.study("std1").feature("stat").set("plistarr", new String[]{String.join(" ", pStepList)});
        model.study("std1").feature("stat").set("punit", new String[]{"kPa"});
        model.study("std1").showAutoSequences("all");

        // Evaluators
        model.result().dataset().create("cpt_cyl", "CutPoint3D");
        model.result().dataset("cpt_cyl").set("data", "dset1");
        model.result().dataset("cpt_cyl").set("pointx", String.valueOf(X_cyl));
        model.result().dataset("cpt_cyl").set("pointy", String.valueOf(Y_cyl));
        model.result().dataset("cpt_cyl").set("pointz", "0");

        model.result().numerical().create("pev_cyl", "EvalPoint");
        model.result().numerical("pev_cyl").set("data", "cpt_cyl");
        model.result().numerical("pev_cyl").set("expr", new String[]{"u", "v"});
        model.result().numerical("pev_cyl").set("unit", new String[]{"mm", "mm"});

        model.result().numerical().create("int_Tn_dst", "IntSurface");
        model.result().numerical("int_Tn_dst").set("data", "dset1");
        model.result().numerical("int_Tn_dst").selection().set(arr_act_outer);
        model.result().numerical("int_Tn_dst").set("expr", new String[]{"solid.cnt1.Tn"});
        model.result().numerical("int_Tn_dst").set("unit", new String[]{"N"});

        model.result().numerical().create("max_Tn_dst", "MaxSurface");
        model.result().numerical("max_Tn_dst").set("data", "dset1");
        model.result().numerical("max_Tn_dst").selection().set(arr_act_outer);
        model.result().numerical("max_Tn_dst").set("expr", new String[]{"solid.cnt1.Tn"});
        model.result().numerical("max_Tn_dst").set("unit", new String[]{"kPa"});

        model.result().numerical().create("max_vm", "MaxVolume");
        model.result().numerical("max_vm").set("data", "dset1");
        model.result().numerical("max_vm").selection().all();
        model.result().numerical("max_vm").set("expr", new String[]{"solid.mises"});
        model.result().numerical("max_vm").set("unit", new String[]{"kPa"});

        // Tip Evaluators
        model.result().dataset().create("cpt_tip", "CutPoint3D");
        model.result().dataset("cpt_tip").set("data", "dset1");
        model.result().dataset("cpt_tip").set("pointx", String.valueOf(L_act));
        model.result().dataset("cpt_tip").set("pointy", "0");
        model.result().dataset("cpt_tip").set("pointz", "0");

        model.result().numerical().create("pev_tip", "EvalPoint");
        model.result().numerical("pev_tip").set("data", "cpt_tip");
        model.result().numerical("pev_tip").set("expr", new String[]{"u", "v", "w"});
        model.result().numerical("pev_tip").set("unit", new String[]{"mm", "mm", "mm"});

        model.result().dataset().create("cpt_py", "CutPoint3D");
        model.result().dataset("cpt_py").set("data", "dset1");
        model.result().dataset("cpt_py").set("pointx", String.valueOf(L_act));
        model.result().dataset("cpt_py").set("pointy", String.valueOf(Rt));
        model.result().dataset("cpt_py").set("pointz", "0");

        model.result().numerical().create("pev_py", "EvalPoint");
        model.result().numerical("pev_py").set("data", "cpt_py");
        model.result().numerical("pev_py").set("expr", new String[]{"u", "v"});
        model.result().numerical("pev_py").set("unit", new String[]{"mm", "mm"});

        model.result().dataset().create("cpt_ny", "CutPoint3D");
        model.result().dataset("cpt_ny").set("data", "dset1");
        model.result().dataset("cpt_ny").set("pointx", String.valueOf(L_act));
        model.result().dataset("cpt_ny").set("pointy", String.valueOf(-Rt));
        model.result().dataset("cpt_ny").set("pointz", "0");

        model.result().numerical().create("pev_ny", "EvalPoint");
        model.result().numerical("pev_ny").set("data", "cpt_ny");
        model.result().numerical("pev_ny").set("expr", new String[]{"u", "v"});
        model.result().numerical("pev_ny").set("unit", new String[]{"mm", "mm"});

        return model;
    }
}
