import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

public class TestActuatorCylinderContactSingle {
    public static void main(String[] args) throws Exception {
        String baseDir = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp";
        double rCyl = 15.0;
        String[] pStepList = {"0", "5", "10", "15", "20", "25", "30", "35", "40"};

        System.out.println(">>> TESTING CONTACT FOR R = " + rCyl + " mm <<<");
        Model model = buildContactModel(baseDir, rCyl, pStepList);

        System.out.println("Running study...");
        model.study("std1").run();
        System.out.println("Study solved successfully!");

        double[][] resFx = model.result().numerical("int_Fx").getReal();
        double[][] resFy = model.result().numerical("int_Fy").getReal();
        double[][] resTip = model.result().numerical("pev_tip").getReal();
        double[][] resPy = model.result().numerical("pev_py").getReal();
        double[][] resNy = model.result().numerical("pev_ny").getReal();

        int nSteps = resTip[0].length;
        double rp = 1.8;
        double L_act = 140.0;

        for (int s = 0; s < nSteps; s++) {
            double p = resTip[0][s];
            double u = resTip[1][s];
            double v = resTip[2][s];
            double x_py = L_act + resPy[0][s], y_py = rp + resPy[1][s];
            double x_ny = L_act + resNy[0][s], y_ny = -rp + resNy[1][s];
            double theta_deg = Math.atan2(x_py - x_ny, y_py - y_ny) * 180.0 / Math.PI;
            if (theta_deg < -10.0) theta_deg += 360.0;

            double fx = resFx[0][s];
            double fy = resFy[0][s];
            double fn = Math.sqrt(fx * fx + fy * fy);
            double ff = 0.50 * fn; // Coulomb friction capacity

            System.out.printf(Locale.US, "P=%.1f kPa: Tip=(%.2f, %.2f) mm, Theta=%.1f deg, Fx=%.4f N, Fy=%.4f N, Fn=%.4f N, Ff=%.4f N%n",
                p, u, v, theta_deg, fx, fy, fn, ff);
        }

        ModelUtil.remove("Model");
    }

    public static Model buildContactModel(String baseDir, double rCyl, String[] pStepList) {
        Model model = ModelUtil.create("Model");
        model.modelPath(baseDir);

        double L_act = 140.0;
        double R0 = 9.5;
        double Rt = 3.8;
        double tw_0 = 2.0;
        double tw_t = 0.8;
        double depth_0 = 2.5;
        double depth_t = 1.0;
        double x_start = 4.0;
        double x_end = 136.0;

        double X_cyl = 30.0;
        double Y_cyl = -28.0;
        double L_cyl = 40.0;

        model.param().set("L_act", L_act + "[mm]", "Actuator length");
        model.param().set("R0", R0 + "[mm]", "Base outer radius");
        model.param().set("Rt", Rt + "[mm]", "Tip outer radius");
        model.param().set("tw_0", tw_0 + "[mm]", "Base wall thickness");
        model.param().set("tw_t", tw_t + "[mm]", "Tip wall thickness");
        model.param().set("depth_0", depth_0 + "[mm]", "Base cavity depth");
        model.param().set("depth_t", depth_t + "[mm]", "Tip cavity depth");
        model.param().set("x_start", x_start + "[mm]", "Cavity start position");
        model.param().set("x_end", x_end + "[mm]", "Cavity end position");
        model.param().set("L_cav", "x_end - x_start", "Cavity length");

        model.param().set("r_out0", "R0 - tw_0", "Cavity outer radius base");
        model.param().set("r_outt", "Rt - tw_t", "Cavity outer radius tip");
        model.param().set("r_in0", "r_out0 - depth_0", "Cavity inner radius base");
        model.param().set("r_int", "r_outt - depth_t", "Cavity inner radius tip");

        model.param().set("R_cyl", rCyl + "[mm]", "Target cylinder radius");
        model.param().set("X_cyl", X_cyl + "[mm]", "Target cylinder center X");
        model.param().set("Y_cyl", Y_cyl + "[mm]", "Target cylinder center Y");
        model.param().set("L_cyl", L_cyl + "[mm]", "Target cylinder length");

        model.param().set("E_mod", "0.40[MPa]", "Silicone Young's modulus");
        model.param().set("nu_val", "0.45", "Silicone Poisson ratio");
        model.param().set("mu_mod", "E_mod/(2*(1+nu_val))", "Shear modulus");
        model.param().set("kappa_mod", "2*mu_mod*(1+nu_val)/(3*(1-2*nu_val))", "Bulk modulus");

        model.param().set("eta_drive", "0.10", "Follower pressure transfer ratio");
        model.param().set("alpha_balloon", "0.005", "Cavity roof ballooning ratio");
        model.param().set("mu_fric", "0.50", "Static Coulomb friction coefficient");

        model.param().set("P_step", "0[kPa]", "Continuation gauge pressure");
        model.param().set("w1", "1", "Cavity 1 weight (Top +Y Pitch Down)");
        model.param().set("w2", "0", "Cavity 2 weight (Right +Z Yaw Left)");
        model.param().set("w3", "0", "Cavity 3 weight (Bottom -Y Pitch Up)");
        model.param().set("w4", "0", "Cavity 4 weight (Left -Z Yaw Right)");
        model.param().set("P1", "w1*P_step", "Cavity 1 pressure");
        model.param().set("P2", "w2*P_step", "Cavity 2 pressure");
        model.param().set("P3", "w3*P_step", "Cavity 3 pressure");
        model.param().set("P4", "w4*P_step", "Cavity 4 pressure");

        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").lengthUnit("mm");

        // Actuator Body
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
            model.component("comp1").geom("geom1").feature(b1Tag).set("pos", new double[]{x_start - 0.5, 0.0, -50.0});
            model.component("comp1").geom("geom1").feature(b1Tag).set("size", new double[]{134.0, 50.0, 100.0});

            model.component("comp1").geom("geom1").create(r1Tag, "Rotate");
            model.component("comp1").geom("geom1").feature(r1Tag).set("rot", baseRot + rotOffset);
            model.component("comp1").geom("geom1").feature(r1Tag).set("axistype", "x");
            model.component("comp1").geom("geom1").feature(r1Tag).selection("input").set(b1Tag);

            model.component("comp1").geom("geom1").create(b2Tag, "Block");
            model.component("comp1").geom("geom1").feature(b2Tag).set("pos", new double[]{x_start - 0.5, 0.0, -50.0});
            model.component("comp1").geom("geom1").feature(b2Tag).set("size", new double[]{134.0, 50.0, 100.0});

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

        // Target Rigid Cylinder
        model.component("comp1").geom("geom1").create("target_cyl", "Cylinder");
        model.component("comp1").geom("geom1").feature("target_cyl").set("axistype", "z");
        model.component("comp1").geom("geom1").feature("target_cyl").set("pos", new double[]{X_cyl, Y_cyl, -L_cyl/2.0});
        model.component("comp1").geom("geom1").feature("target_cyl").set("r", "R_cyl");
        model.component("comp1").geom("geom1").feature("target_cyl").set("h", "L_cyl");

        model.component("comp1").geom("geom1").feature("fin").set("action", "assembly");
        model.component("comp1").geom("geom1").feature("fin").set("createpairs", true);
        model.component("comp1").geom("geom1").run();

        int nDom = model.component("comp1").geom("geom1").getNDomains();
        int nBnd = model.component("comp1").geom("geom1").getNBoundaries();
        System.out.println("Assembly geometry: " + nDom + " domains, " + nBnd + " boundaries.");

        // Detect boundaries
        model.component("comp1").mesh().create("mesh1");
        model.component("comp1").mesh("mesh1").autoMeshSize(5);
        model.component("comp1").mesh("mesh1").run();

        model.result().dataset().create("dset_mesh", "Mesh");
        model.result().dataset("dset_mesh").set("mesh", "mesh1");

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
            model.result().numerical("int_b").set("expr", new String[]{"X*1000", "Y*1000", "Z*1000", "1"});
            double[][] res = model.result().numerical("int_b").getReal();
            double area = res[3][0];
            double xm = res[0][0] / area;
            double ym = res[1][0] / area;
            double zm = res[2][0] / area;
            double rm = Math.sqrt(ym * ym + zm * zm);
            model.result().numerical().remove("int_b");

            // Cylinder boundary check
            double distCyl = Math.sqrt((xm - X_cyl)*(xm - X_cyl) + (ym - Y_cyl)*(ym - Y_cyl));
            if (distCyl < rCyl + 2.0 && xm > X_cyl - rCyl - 2.0 && xm < X_cyl + rCyl + 2.0) {
                list_cyl_bnds.add(b);
                continue;
            }

            if (xm < 0.1) {
                list_base.add(b);
                continue;
            }
            if (xm > L_act - 0.5) continue;

            double expectedR_outer = R0 - (R0 - Rt)*(xm / L_act);
            // Outer skin boundaries have large area (> 1e-3 m^2) and match outer radius
            if (area > 1.0e-3 && Math.abs(rm - expectedR_outer) < 1.5) {
                list_act_outer.add(b);
                continue;
            }

            int kCav = -1;
            if (ym > Math.abs(zm)) kCav = 0;
            else if (zm > Math.abs(ym)) kCav = 1;
            else if (ym < -Math.abs(zm)) kCav = 2;
            else kCav = 3;

            if (Math.abs(xm - x_start) < 0.8) {
                list_balloon.get(kCav).add(b);
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
        int[] arr_cyl = list_cyl_bnds.stream().mapToInt(i -> i).toArray();
        int[] arr_act_outer = list_act_outer.stream().mapToInt(i -> i).toArray();

        System.out.println("Boundary identification: Base=" + Arrays.toString(arr_base) + ", ActOuter=" + Arrays.toString(arr_act_outer) + ", Cyl=" + Arrays.toString(arr_cyl));

        // CREATE EXPLICIT CONTACT PAIR: Master = Cylinder, Slave = Actuator Outer Skin
        model.pair().create("p1", "Contact", "geom1");
        model.pair("p1").source().set(arr_cyl);
        model.pair("p1").destination().set(arr_act_outer);
        System.out.println("Explicit Contact Pair p1 created successfully!");

        // Physics
        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
        
        // Default Linear Elastic Material 1 (used for stiff rigid cylinder, Domain 2)
        model.component("comp1").physics("solid").feature("lemm1").set("E_mat", "userdef");
        model.component("comp1").physics("solid").feature("lemm1").set("E", "100[GPa]");
        model.component("comp1").physics("solid").feature("lemm1").set("nu_mat", "userdef");
        model.component("comp1").physics("solid").feature("lemm1").set("nu", "0.33");

        // Actuator Hyperelastic Material (Domain 1)
        model.component("comp1").physics("solid").create("hmm1", "HyperelasticModel", 3);
        model.component("comp1").physics("solid").feature("hmm1").selection().set(new int[]{1});
        model.component("comp1").physics("solid").feature("hmm1").set("MaterialModel", "NeoHookean");
        model.component("comp1").physics("solid").feature("hmm1").set("Compressibility_NeoHookean", "Compressible");
        model.component("comp1").physics("solid").feature("hmm1").set("IsotropicOption", "KG");
        model.component("comp1").physics("solid").feature("hmm1").set("G_mat", "userdef");
        model.component("comp1").physics("solid").feature("hmm1").set("G", "mu_mod");
        model.component("comp1").physics("solid").feature("hmm1").set("K_mat", "userdef");
        model.component("comp1").physics("solid").feature("hmm1").set("K", "kappa_mod");

        // Fixed base
        model.component("comp1").physics("solid").create("fix_act", "Fixed", 2);
        model.component("comp1").physics("solid").feature("fix_act").selection().set(arr_base);

        // Fixed rigid cylinder (Domain 2)
        model.component("comp1").physics("solid").create("fix_cyl", "Fixed", 3);
        model.component("comp1").physics("solid").feature("fix_cyl").selection().set(new int[]{2});

        // Actuator Loads
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

        // Contact feature with Penalty formulation
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
        model.result().dataset().create("cpt_tip", "CutPoint3D");
        model.result().dataset("cpt_tip").set("data", "dset1");
        model.result().dataset("cpt_tip").set("pointx", String.valueOf(L_act));
        model.result().dataset("cpt_tip").set("pointy", "0");
        model.result().dataset("cpt_tip").set("pointz", "0");

        double rp = 1.8;
        model.result().dataset().create("cpt_py", "CutPoint3D");
        model.result().dataset("cpt_py").set("data", "dset1");
        model.result().dataset("cpt_py").set("pointx", String.valueOf(L_act));
        model.result().dataset("cpt_py").set("pointy", String.valueOf(rp));
        model.result().dataset("cpt_py").set("pointz", "0");

        model.result().dataset().create("cpt_ny", "CutPoint3D");
        model.result().dataset("cpt_ny").set("data", "dset1");
        model.result().dataset("cpt_ny").set("pointx", String.valueOf(L_act));
        model.result().dataset("cpt_ny").set("pointy", String.valueOf(-rp));
        model.result().dataset("cpt_ny").set("pointz", "0");

        model.result().numerical().create("pev_tip", "EvalPoint");
        model.result().numerical("pev_tip").set("data", "cpt_tip");
        model.result().numerical("pev_tip").set("expr", new String[]{"P_step", "u", "v", "w"});
        model.result().numerical("pev_tip").set("unit", new String[]{"kPa", "mm", "mm", "mm"});

        model.result().numerical().create("pev_py", "EvalPoint");
        model.result().numerical("pev_py").set("data", "cpt_py");
        model.result().numerical("pev_py").set("expr", new String[]{"u", "v"});
        model.result().numerical("pev_py").set("unit", new String[]{"mm", "mm"});

        model.result().numerical().create("pev_ny", "EvalPoint");
        model.result().numerical("pev_ny").set("data", "cpt_ny");
        model.result().numerical("pev_ny").set("expr", new String[]{"u", "v"});
        model.result().numerical("pev_ny").set("unit", new String[]{"mm", "mm"});

        // Reaction forces on the cylinder domain (or boundaries)
        model.result().numerical().create("int_Fx", "IntSurface");
        model.result().numerical("int_Fx").set("data", "dset1");
        model.result().numerical("int_Fx").selection().set(arr_cyl);
        model.result().numerical("int_Fx").set("expr", new String[]{"reacf(u)"});
        model.result().numerical("int_Fx").set("unit", new String[]{"N"});

        model.result().numerical().create("int_Fy", "IntSurface");
        model.result().numerical("int_Fy").set("data", "dset1");
        model.result().numerical("int_Fy").selection().set(arr_cyl);
        model.result().numerical("int_Fy").set("expr", new String[]{"reacf(v)"});
        model.result().numerical("int_Fy").set("unit", new String[]{"N"});

        return model;
    }
}
