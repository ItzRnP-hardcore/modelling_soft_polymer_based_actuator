import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;

public class BuildActuatorBare {
    public static Model run() {
        Model model = ModelUtil.create("Model");
        model.modelPath("c:\\Users\\rudra\\Documents\\BTP\\comsol");
        model.label("actuator_bare.mph");

        // 1. Global Parameters
        model.param().set("R_out", "10[mm]", "Outer radius");
        model.param().set("r_cav", "4[mm]", "Cavity radius");
        model.param().set("r_p", "3[mm]", "Cavity eccentricity (+y)");
        model.param().set("L_act", "100[mm]", "Pressurised cavity length");
        model.param().set("t_cap", "5[mm]", "Solid tip cap thickness");
        model.param().set("E_mod", "0.6[MPa]", "Small-strain Young's modulus");
        model.param().set("mu_mod", "E_mod/3", "Shear modulus (0.2 MPa)");
        model.param().set("nu_val", "0.4995", "Poisson's ratio");
        model.param().set("kappa_mod", "2*mu_mod*(1+nu_val)/(3*(1-2*nu_val))", "Bulk modulus");
        model.param().set("P_kPa", "0", "Gauge pressure in kPa");
        model.param().set("P_cav", "P_kPa*1[kPa]", "Cavity pressure");

        // 2. Component & 3D Geometry (Half-model z >= 0)
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").lengthUnit("mm");

        // Outer cylinder along +x axis: axis (1,0,0), base at (0,0,0), radius R_out, length L_act + t_cap
        model.component("comp1").geom("geom1").create("cyl_out", "Cylinder");
        model.component("comp1").geom("geom1").feature("cyl_out").set("axistype", "x");
        model.component("comp1").geom("geom1").feature("cyl_out").set("pos", new double[]{0.0, 0.0, 0.0});
        model.component("comp1").geom("geom1").feature("cyl_out").set("r", "R_out");
        model.component("comp1").geom("geom1").feature("cyl_out").set("h", "L_act+t_cap");

        // Cavity cylinder along +x axis: base at (0, r_p, 0), radius r_cav, length L_act
        model.component("comp1").geom("geom1").create("cyl_cav", "Cylinder");
        model.component("comp1").geom("geom1").feature("cyl_cav").set("axistype", "x");
        model.component("comp1").geom("geom1").feature("cyl_cav").set("pos", new String[]{"0", "r_p", "0"});
        model.component("comp1").geom("geom1").feature("cyl_cav").set("r", "r_cav");
        model.component("comp1").geom("geom1").feature("cyl_cav").set("h", "L_act");

        // Subtract cavity from outer cylinder
        model.component("comp1").geom("geom1").create("dif1", "Difference");
        model.component("comp1").geom("geom1").feature("dif1").selection("input").set("cyl_out");
        model.component("comp1").geom("geom1").feature("dif1").selection("input2").set("cyl_cav");

        // Keep only half z >= 0 by intersecting with a bounding block
        model.component("comp1").geom("geom1").create("blk_half", "Block");
        model.component("comp1").geom("geom1").feature("blk_half").set("pos", new String[]{"-1", "-R_out-1", "0"});
        model.component("comp1").geom("geom1").feature("blk_half").set("size", new String[]{"L_act+t_cap+2", "2*R_out+2", "R_out+1"});

        model.component("comp1").geom("geom1").create("int1", "Intersection");
        model.component("comp1").geom("geom1").feature("int1").selection("input").set("dif1", "blk_half");

        model.component("comp1").geom("geom1").run();

        // 3. Selections
        model.component("comp1").selection().create("sel_base", "Box");
        model.component("comp1").selection("sel_base").set("entitydim", 2);
        model.component("comp1").selection("sel_base").set("condition", "inside");
        model.component("comp1").selection("sel_base").set("xmin", "-0.01[mm]");
        model.component("comp1").selection("sel_base").set("xmax", "0.01[mm]");
        model.component("comp1").selection("sel_base").set("ymin", "-15[mm]");
        model.component("comp1").selection("sel_base").set("ymax", "15[mm]");
        model.component("comp1").selection("sel_base").set("zmin", "-1[mm]");
        model.component("comp1").selection("sel_base").set("zmax", "15[mm]");

        model.component("comp1").selection().create("sel_sym", "Box");
        model.component("comp1").selection("sel_sym").set("entitydim", 2);
        model.component("comp1").selection("sel_sym").set("condition", "inside");
        model.component("comp1").selection("sel_sym").set("xmin", "-1[mm]");
        model.component("comp1").selection("sel_sym").set("xmax", "110[mm]");
        model.component("comp1").selection("sel_sym").set("ymin", "-15[mm]");
        model.component("comp1").selection("sel_sym").set("ymax", "15[mm]");
        model.component("comp1").selection("sel_sym").set("zmin", "-0.01[mm]");
        model.component("comp1").selection("sel_sym").set("zmax", "0.01[mm]");

        model.component("comp1").selection().create("sel_cav_box", "Box");
        model.component("comp1").selection("sel_cav_box").set("entitydim", 2);
        model.component("comp1").selection("sel_cav_box").set("condition", "inside");
        model.component("comp1").selection("sel_cav_box").set("xmin", "-0.01[mm]");
        model.component("comp1").selection("sel_cav_box").set("xmax", "100.01[mm]");
        model.component("comp1").selection("sel_cav_box").set("ymin", "-1.05[mm]");
        model.component("comp1").selection("sel_cav_box").set("ymax", "7.05[mm]");
        model.component("comp1").selection("sel_cav_box").set("zmin", "-0.01[mm]");
        model.component("comp1").selection("sel_cav_box").set("zmax", "4.05[mm]");

        model.component("comp1").selection().create("sel_cav", "Difference");
        model.component("comp1").selection("sel_cav").set("entitydim", 2);
        model.component("comp1").selection("sel_cav").set("add", new String[]{"sel_cav_box"});
        model.component("comp1").selection("sel_cav").set("subtract", new String[]{"sel_sym", "sel_base"});

        // 4. Solid Mechanics Physics (Hyperelastic Neo-Hookean, Nearly Incompressible mixed u-p)
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

        // Fixed Constraint at Base x = 0
        model.component("comp1").physics("solid").create("fix1", "Fixed", 2);
        model.component("comp1").physics("solid").feature("fix1").selection().named("sel_base");

        // Symmetry Constraint at z = 0
        model.component("comp1").physics("solid").create("sym1", "SymmetrySolid", 2);
        model.component("comp1").physics("solid").feature("sym1").selection().named("sel_sym");

        // Follower Pressure on Cavity Wall & Cap
        model.component("comp1").physics("solid").create("bndl1", "BoundaryLoad", 2);
        model.component("comp1").physics("solid").feature("bndl1").selection().named("sel_cav");
        model.component("comp1").physics("solid").feature("bndl1").set("forceType", "FollowerPressure");
        model.component("comp1").physics("solid").feature("bndl1").set("pressure", "P_cav");

        // 5. Mesh
        model.component("comp1").mesh().create("mesh1");
        model.component("comp1").mesh("mesh1").autoMeshSize(4); // Fine/normal tetrahedral mesh
        model.component("comp1").mesh("mesh1").run();

        // 6. Study (Stationary with Geometric Nonlinearity & Auxiliary Sweep)
        model.study().create("std1");
        model.study("std1").create("stat", "Stationary");
        model.study("std1").feature("stat").set("geometricNonlinearity", true);
        model.study("std1").feature("stat").set("useparam", true);
        model.study("std1").feature("stat").set("pname", new String[]{"P_kPa"});
        model.study("std1").feature("stat").set("plistarr", new String[]{"0 10 20 30 40 50 60 70 80 90"});
        model.study("std1").feature("stat").set("punit", new String[]{""});

        model.study("std1").run();

        // 7. Evaluate displacements u, v at top (100, 10, 0) and bottom (100, -10, 0) at x = L = 100 mm
        model.result().numerical().create("pev_top", "EvalPoint");
        model.result().numerical("pev_top").set("data", "dset1");
        model.result().numerical("pev_top").set("expr", new String[]{"u", "v"});

        // Use CutPoint3D datasets at (100, 10, 0) and (100, -10, 0)
        model.result().dataset().create("cpt_top", "CutPoint3D");
        model.result().dataset("cpt_top").set("data", "dset1");
        model.result().dataset("cpt_top").set("pointx", "100");
        model.result().dataset("cpt_top").set("pointy", "10");
        model.result().dataset("cpt_top").set("pointz", "0");

        model.result().dataset().create("cpt_bot", "CutPoint3D");
        model.result().dataset("cpt_bot").set("data", "dset1");
        model.result().dataset("cpt_bot").set("pointx", "100");
        model.result().dataset("cpt_bot").set("pointy", "-10");
        model.result().dataset("cpt_bot").set("pointz", "0");

        model.result().numerical("pev_top").set("data", "cpt_top");
        model.result().numerical("pev_top").set("unit", new String[]{"mm", "mm"});
        double[][] uv_top = model.result().numerical("pev_top").getReal();

        model.result().numerical().create("pev_bot", "EvalPoint");
        model.result().numerical("pev_bot").set("data", "cpt_bot");
        model.result().numerical("pev_bot").set("expr", new String[]{"u", "v"});
        model.result().numerical("pev_bot").set("unit", new String[]{"mm", "mm"});
        double[][] uv_bot = model.result().numerical("pev_bot").getReal();

        double[] p_vals = new double[]{0, 10, 20, 30, 40, 50, 60, 70, 80, 90};
        try {
            PrintWriter pw = new PrintWriter(new FileWriter("c:\\Users\\rudra\\Documents\\BTP\\comsol\\comsol_bare_summary.csv"));
            pw.println("P_kPa,u_top_mm,v_top_mm,u_bot_mm,v_bot_mm,x_tip_mm,delta_mm,theta_deg");
            int nSteps = Math.min(p_vals.length, uv_top[0].length);
            for (int i = 0; i < nSteps; i++) {
                double ut = uv_top[0][i];
                double vt = uv_top[1][i];
                double ub = uv_bot[0][i];
                double vb = uv_bot[1][i];
                double x_top = 100.0 + ut;
                double y_top = 10.0 + vt;
                double x_bot = 100.0 + ub;
                double y_bot = -10.0 + vb;
                double x_tip = 0.5 * (x_top + x_bot);
                double delta = -0.5 * (y_top + y_bot);
                double theta_deg = Math.atan2(x_top - x_bot, y_top - y_bot) * 180.0 / Math.PI;
                pw.printf("%.2f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f%n",
                          p_vals[i], ut, vt, ub, vb, x_tip, delta, theta_deg);
                System.out.printf("COMSOL P = %5.1f kPa : delta = %8.3f mm, theta = %6.2f deg%n",
                                  p_vals[i], delta, theta_deg);
            }
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return model;
    }

    public static void main(String[] args) {
        run();
    }
}
