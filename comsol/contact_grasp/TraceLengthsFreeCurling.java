import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

/**
 * TraceLengthsFreeCurling.java
 * Solves free curling for actuator lengths L = 220, 250, 280 mm up to P = 100 kPa.
 * Outputs trajectory via System.out for python processing.
 */
public class TraceLengthsFreeCurling {

    public static void main(String[] args) {
        String baseDir = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp";
        String imgDir = baseDir + "\\images";

        double[] testLengths = {220.0, 250.0, 280.0};
        String[] pStepList = {"0", "20", "40", "60", "80", "100"};

        for (double L_act : testLengths) {
            String caseTag = "FreeCurl_L" + (int)L_act + "mm";
            System.out.printf(Locale.US, "%n================================================================%n");
            System.out.printf(Locale.US, "  SOLVING FREE CURLING FOR L = %.0f mm%n", L_act);
            System.out.printf(Locale.US, "================================================================%n");

            try {
                Model model = ModelUtil.create("Model_" + (int)L_act);
                model.modelPath(baseDir);

                double R0 = 11.5;
                double Rt = 3.2;
                double tw_0 = 2.2;
                double tw_t = 0.65;
                double depth_0 = 2.8;
                double depth_t = 0.85;
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

                // Cone outer body
                model.component("comp1").geom("geom1").create("cone_body", "Cone");
                model.component("comp1").geom("geom1").feature("cone_body").set("axistype", "x");
                model.component("comp1").geom("geom1").feature("cone_body").set("pos", new double[]{0.0, 0.0, 0.0});
                model.component("comp1").geom("geom1").feature("cone_body").set("r", "R0");
                model.component("comp1").geom("geom1").feature("cone_body").set("specifytop", "radius");
                model.component("comp1").geom("geom1").feature("cone_body").set("rtop", "Rt");
                model.component("comp1").geom("geom1").feature("cone_body").set("h", "L_act");

                // Cavities
                double[] rotAngles = {0.0, 90.0, 180.0, 270.0};
                String[] cavNames = new String[4];

                for (int k = 1; k <= 4; k++) {
                    String cTag = "cav" + k;
                    cavNames[k-1] = cTag;
                    double ang = rotAngles[k-1];

                    model.component("comp1").geom("geom1").create(cTag, "Cone");
                    model.component("comp1").geom("geom1").feature(cTag).set("axistype", "x");
                    model.component("comp1").geom("geom1").feature(cTag).set("pos", new String[]{"x_start", "0", "0"});
                    model.component("comp1").geom("geom1").feature(cTag).set("r", "r_out0");
                    model.component("comp1").geom("geom1").feature(cTag).set("specifytop", "radius");
                    model.component("comp1").geom("geom1").feature(cTag).set("rtop", "r_outt");
                    model.component("comp1").geom("geom1").feature(cTag).set("h", "L_cav");

                    String subTag = "sub_core" + k;
                    model.component("comp1").geom("geom1").create(subTag, "Cone");
                    model.component("comp1").geom("geom1").feature(subTag).set("axistype", "x");
                    model.component("comp1").geom("geom1").feature(subTag).set("pos", new String[]{"x_start", "0", "0"});
                    model.component("comp1").geom("geom1").feature(subTag).set("r", "r_in0");
                    model.component("comp1").geom("geom1").feature(subTag).set("specifytop", "radius");
                    model.component("comp1").geom("geom1").feature(subTag).set("rtop", "r_int");
                    model.component("comp1").geom("geom1").feature(subTag).set("h", "L_cav");

                    String difTag = "dif_shell" + k;
                    model.component("comp1").geom("geom1").create(difTag, "Difference");
                    model.component("comp1").geom("geom1").feature(difTag).selection("input").set(cTag);
                    model.component("comp1").geom("geom1").feature(difTag).selection("input2").set(subTag);

                    String blkTag = "blk_quad" + k;
                    model.component("comp1").geom("geom1").create(blkTag, "Block");
                    model.component("comp1").geom("geom1").feature(blkTag).set("size", new String[]{"L_cav*1.2", "R0*2", "R0*2"});
                    model.component("comp1").geom("geom1").feature(blkTag).set("pos", new String[]{"x_start*0.9", "0", "0"});

                    if (ang != 0.0) {
                        String rotTag = "rot_quad" + k;
                        model.component("comp1").geom("geom1").create(rotTag, "Rotate");
                        model.component("comp1").geom("geom1").feature(rotTag).selection("input").set(blkTag);
                        model.component("comp1").geom("geom1").feature(rotTag).set("rot", ang);
                        model.component("comp1").geom("geom1").feature(rotTag).set("pos", new double[]{0.0, 0.0, 0.0});
                        model.component("comp1").geom("geom1").feature(rotTag).set("axis", new double[]{1.0, 0.0, 0.0});
                    }

                    String intTag = "int_cav" + k;
                    model.component("comp1").geom("geom1").create(intTag, "Intersection");
                    model.component("comp1").geom("geom1").feature(intTag).selection("input").set(difTag, blkTag);
                }

                String difFinal = "dif_final";
                model.component("comp1").geom("geom1").create(difFinal, "Difference");
                model.component("comp1").geom("geom1").feature(difFinal).selection("input").set("cone_body");
                model.component("comp1").geom("geom1").feature(difFinal).selection("input2").set(cavNames);

                model.component("comp1").geom("geom1").run();

                // Mesh
                model.component("comp1").mesh().create("mesh1");
                model.component("comp1").mesh("mesh1").autoMeshSize(5);
                model.component("comp1").mesh("mesh1").run();

                // Physics
                model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
                model.component("comp1").physics("solid").prop("ShapeProperty").set("order_displacement", "1");
                model.component("comp1").physics("solid").feature("lemm1").create("hyp1", "HyperelasticMaterialModel");
                model.component("comp1").physics("solid").feature("lemm1").feature("hyp1").set("HyperelasticModel", "NeoHookean");
                model.component("comp1").physics("solid").feature("lemm1").feature("hyp1").set("SpecifyParameters", "muAndKappa");
                model.component("comp1").physics("solid").feature("lemm1").feature("hyp1").set("mu", "mu_mod");
                model.component("comp1").physics("solid").feature("lemm1").feature("hyp1").set("kappa", "kappa_mod");

                model.result().dataset().create("dset_mesh", "Mesh");
                model.result().dataset("dset_mesh").set("mesh", "mesh1");

                int nBnd = model.component("comp1").geom("geom1").getNBoundaries();
                List<Integer> list_base = new ArrayList<>();
                List<List<Integer>> list_drive = new ArrayList<>();
                List<List<Integer>> list_balloon = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    list_drive.add(new ArrayList<>());
                    list_balloon.add(new ArrayList<>());
                }

                for (int b = 1; b <= nBnd; b++) {
                    String numTag = "int_b_" + b;
                    model.result().numerical().create(numTag, "IntSurface");
                    model.result().numerical(numTag).set("data", "dset_mesh");
                    model.result().numerical(numTag).selection().set(new int[]{b});
                    model.result().numerical(numTag).set("expr", new String[]{"X", "Y", "Z", "1"});
                    double[][] res = model.result().numerical(numTag).getReal();
                    double area = res[3][0];
                    double xm = (res[0][0] / area) * 1000.0;
                    double ym = (res[1][0] / area) * 1000.0;
                    double zm = (res[2][0] / area) * 1000.0;
                    model.result().numerical().remove(numTag);

                    if (xm < 0.1) {
                        list_base.add(b);
                    } else if (xm > 4.5 && xm < L_act - 5.5) {
                        double ang = Math.atan2(zm, ym) * (180.0 / Math.PI);
                        if (ang < 0) ang += 360.0;
                        int cavIdx = 0;
                        if (ang >= 315.0 || ang < 45.0) cavIdx = 0;
                        else if (ang >= 45.0 && ang < 135.0) cavIdx = 1;
                        else if (ang >= 135.0 && ang < 225.0) cavIdx = 2;
                        else cavIdx = 3;

                        double r_bnd = Math.hypot(ym, zm);
                        double r_act = R0 - (R0 - Rt) * (xm / L_act);
                        double d_skin = r_act - r_bnd;

                        if (d_skin < 1.6) {
                            list_drive.get(cavIdx).add(b);
                        } else {
                            list_balloon.get(cavIdx).add(b);
                        }
                    }
                }

                // Base fixed
                int[] arr_base = list_base.stream().mapToInt(i -> i).toArray();
                model.component("comp1").physics("solid").create("fix1", "Fixed", 2);
                model.component("comp1").physics("solid").feature("fix1").selection().set(arr_base);

                // Cavity Follower Pressure
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
                model.study("std1").feature("stat").set("geometricNonlinearity", "on");
                model.study("std1").feature("stat").set("pname", new String[]{"P_step"});
                model.study("std1").feature("stat").set("plistarr", new String[]{String.join(" ", pStepList)});
                model.study("std1").feature("stat").set("punit", new String[]{"kPa"});

                model.sol().create("sol1");
                model.sol("sol1").study("std1");
                model.sol("sol1").create("st1", "StudyStep");
                model.sol("sol1").create("v1", "Variables");
                model.sol("sol1").create("s1", "Stationary");
                model.sol("sol1").feature("s1").create("p1", "Parametric");
                model.sol("sol1").feature("s1").feature("p1").set("pname", new String[]{"P_step"});
                model.sol("sol1").feature("s1").feature("p1").set("plistarr", new String[]{String.join(" ", pStepList)});
                model.sol("sol1").feature("s1").feature("p1").set("punit", new String[]{"kPa"});
                model.sol("sol1").feature("s1").create("fc1", "FullyCoupled");
                model.sol("sol1").feature("s1").feature("fc1").set("maxiter", 50);
                model.sol("sol1").attach("std1");

                long t0 = System.currentTimeMillis();
                try {
                    model.study("std1").run();
                } catch (Exception eRun) {
                    System.out.println("Notice: Study run stopped at: " + eRun.getMessage());
                }
                long dt = (System.currentTimeMillis() - t0) / 1000;
                System.out.println("Solve completed in " + dt + " seconds.");

                double[] solvedP = model.sol("sol1").getPVals();
                int nSolved = (solvedP != null) ? solvedP.length : 0;
                System.out.println("Solved steps: " + nSolved);
                if (nSolved == 0) continue;

                int lastIdx = nSolved - 1;
                double pLast = solvedP[lastIdx] / 1000.0;

                // Centerline extraction at P_last
                int nPts = 45;
                double[] sVals = new double[nPts];
                for (int i = 0; i < nPts; i++) {
                    sVals[i] = (i / (double)(nPts - 1)) * L_act;
                }

                model.result().dataset().create("cpt_spine", "CutPoint3D");
                model.result().dataset("cpt_spine").set("data", "dset1");
                model.result().dataset("cpt_spine").set("pointx", sVals);
                model.result().dataset("cpt_spine").set("pointy", new double[nPts]);
                model.result().dataset("cpt_spine").set("pointz", new double[nPts]);

                model.result().numerical().create("pev_x", "EvalPoint");
                model.result().numerical("pev_x").set("data", "cpt_spine");
                model.result().numerical("pev_x").set("expr", new String[]{"X+u"});
                model.result().numerical("pev_x").set("unit", new String[]{"mm"});

                model.result().numerical().create("pev_y", "EvalPoint");
                model.result().numerical("pev_y").set("data", "cpt_spine");
                model.result().numerical("pev_y").set("expr", new String[]{"Y+v"});
                model.result().numerical("pev_y").set("unit", new String[]{"mm"});

                double[][] vx = model.result().numerical("pev_x").getReal();
                double[][] vy = model.result().numerical("pev_y").getReal();

                for (int i = 0; i < nPts; i++) {
                    System.out.printf(Locale.US, ">> TRAJ_ROW: L=%.1f, P=%.1f, s=%.2f, X=%.4f, Y=%.4f%n",
                                      L_act, pLast, sVals[i], vx[i][lastIdx], vy[i][lastIdx]);
                }

                // Print summary metrics
                double xTip = vx[nPts-1][lastIdx];
                double yTip = vy[nPts-1][lastIdx];
                System.out.printf(Locale.US, ">> FREE_CURL_SUMMARY: L=%.0f mm, P=%.1f kPa, xTip=%.2f, yTip=%.2f%n",
                                  L_act, pLast, xTip, yTip);

                // Export 3D displacement render
                String pgTag = "pg_disp";
                model.result().create(pgTag, "PlotGroup3D");
                model.result(pgTag).set("data", "dset1");
                model.result(pgTag).setIndex("looplevel", lastIdx, 0);
                model.result(pgTag).create("surf1", "Surface");
                model.result(pgTag).feature("surf1").set("expr", "solid.disp");
                model.result(pgTag).feature("surf1").set("unit", "mm");
                model.result(pgTag).feature("surf1").create("def1", "Deform");
                model.result(pgTag).feature("surf1").feature("def1").set("expr", new String[]{"u", "v", "w"});
                model.result(pgTag).feature("surf1").feature("def1").set("unit", new String[]{"mm", "mm", "mm"});
                model.result(pgTag).run();

                String imgPath = imgDir + "\\actuator_free_curl_L" + (int)L_act + "mm_disp.png";
                model.result().export().create("img1", "Image");
                model.result().export("img1").set("plotgroup", pgTag);
                model.result().export("img1").set("size", "manualprint");
                model.result().export("img1").set("unit", "px");
                model.result().export("img1").set("webheight", 600);
                model.result().export("img1").set("webwidth", 800);
                model.result().export("img1").set("pngfilename", imgPath);
                model.result().export("img1").run();
                model.result().export().remove("img1");
                System.out.println("Exported render: " + imgPath);

                ModelUtil.remove("Model_" + (int)L_act);
            } catch (Exception e) {
                System.err.println("Error solving L=" + L_act + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println("All lengths completed!");
    }
}
