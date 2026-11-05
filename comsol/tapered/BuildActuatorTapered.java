import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;

public class BuildActuatorTapered {
    public static Model run() {
        Model model = ModelUtil.create("Model");
        model.modelPath("c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered");

        // 1. Parameters
        model.param().set("L_act", "100[mm]", "Actuator length");
        model.param().set("R0", "10[mm]", "Base outer radius");
        model.param().set("Rt", "5[mm]", "Tip outer radius");
        model.param().set("r0", "4[mm]", "Base cavity radius");
        model.param().set("rt", "2[mm]", "Tip cavity radius");
        model.param().set("rp0", "3[mm]", "Base cavity offset");
        model.param().set("rpt", "1.5[mm]", "Tip cavity offset");
        model.param().set("t_cap", "2[mm]", "End-cap thickness");
        model.param().set("E_mod", "0.6[MPa]", "Young's modulus");
        model.param().set("nu_val", "0.4995", "Poisson ratio");
        model.param().set("mu_mod", "E_mod/(2*(1+nu_val))", "Shear modulus");
        model.param().set("kappa_mod", "2*mu_mod*(1+nu_val)/(3*(1-2*nu_val))", "Bulk modulus");
        model.param().set("P_kPa", "0", "Gauge pressure in kPa");
        model.param().set("P_cav", "P_kPa*1[kPa]", "Cavity pressure");

        // 2. Geometry
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").lengthUnit("mm");

        // Outer Cone
        model.component("comp1").geom("geom1").create("cone_out", "Cone");
        model.component("comp1").geom("geom1").feature("cone_out").set("axistype", "x");
        model.component("comp1").geom("geom1").feature("cone_out").set("pos", new double[]{0.0, 0.0, 0.0});
        model.component("comp1").geom("geom1").feature("cone_out").set("r", "R0");
        model.component("comp1").geom("geom1").feature("cone_out").set("specifytop", "radius");
        model.component("comp1").geom("geom1").feature("cone_out").set("rtop", "R0 - (R0-Rt)*(L_act+t_cap)/L_act");
        model.component("comp1").geom("geom1").feature("cone_out").set("h", "L_act+t_cap");

        // Inner Cavity Cone
        double L_val = 100.0;
        double dy = 1.5 - 3.0; // rpt - rp0 = -1.5
        double cav_len = Math.sqrt(L_val * L_val + dy * dy);

        model.component("comp1").geom("geom1").create("cone_cav", "Cone");
        model.component("comp1").geom("geom1").feature("cone_cav").set("axistype", "cartesian");
        model.component("comp1").geom("geom1").feature("cone_cav").set("pos", new double[]{0.0, 3.0, 0.0});
        model.component("comp1").geom("geom1").feature("cone_cav").set("axis", new double[]{L_val / cav_len, dy / cav_len, 0.0});
        model.component("comp1").geom("geom1").feature("cone_cav").set("r", "r0");
        model.component("comp1").geom("geom1").feature("cone_cav").set("specifytop", "radius");
        model.component("comp1").geom("geom1").feature("cone_cav").set("rtop", "rt");
        model.component("comp1").geom("geom1").feature("cone_cav").set("h", cav_len);

        model.component("comp1").geom("geom1").create("dif1", "Difference");
        model.component("comp1").geom("geom1").feature("dif1").selection("input").set("cone_out");
        model.component("comp1").geom("geom1").feature("dif1").selection("input2").set("cone_cav");

        // Half-symmetry block (z >= 0)
        model.component("comp1").geom("geom1").create("blk_half", "Block");
        model.component("comp1").geom("geom1").feature("blk_half").set("pos", new double[]{-1.0, -15.0, 0.0});
        model.component("comp1").geom("geom1").feature("blk_half").set("size", new double[]{110.0, 30.0, 15.0});

        model.component("comp1").geom("geom1").create("int1", "Intersection");
        model.component("comp1").geom("geom1").feature("int1").selection("input").set("dif1", "blk_half");

        model.component("comp1").geom("geom1").run();

        // 3. Selections
        // Base: x = 0
        model.component("comp1").selection().create("sel_base", "Box");
        model.component("comp1").selection("sel_base").set("entitydim", 2);
        model.component("comp1").selection("sel_base").set("condition", "inside");
        model.component("comp1").selection("sel_base").set("xmin", "-0.01[mm]");
        model.component("comp1").selection("sel_base").set("xmax", "0.01[mm]");
        model.component("comp1").selection("sel_base").set("ymin", "-15[mm]");
        model.component("comp1").selection("sel_base").set("ymax", "15[mm]");
        model.component("comp1").selection("sel_base").set("zmin", "-1[mm]");
        model.component("comp1").selection("sel_base").set("zmax", "15[mm]");

        // Symmetry: z = 0
        model.component("comp1").selection().create("sel_sym", "Box");
        model.component("comp1").selection("sel_sym").set("entitydim", 2);
        model.component("comp1").selection("sel_sym").set("condition", "inside");
        model.component("comp1").selection("sel_sym").set("xmin", "-1[mm]");
        model.component("comp1").selection("sel_sym").set("xmax", "110[mm]");
        model.component("comp1").selection("sel_sym").set("ymin", "-15[mm]");
        model.component("comp1").selection("sel_sym").set("ymax", "15[mm]");
        model.component("comp1").selection("sel_sym").set("zmin", "-0.01[mm]");
        model.component("comp1").selection("sel_sym").set("zmax", "0.01[mm]");

        // Cavity boundaries: inside box around cavity
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

        // 4. Physics
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

        model.component("comp1").physics("solid").create("sym1", "SymmetrySolid", 2);
        model.component("comp1").physics("solid").feature("sym1").selection().named("sel_sym");

        model.component("comp1").physics("solid").create("bndl1", "BoundaryLoad", 2);
        model.component("comp1").physics("solid").feature("bndl1").selection().named("sel_cav");
        model.component("comp1").physics("solid").feature("bndl1").set("forceType", "FollowerPressure");
        model.component("comp1").physics("solid").feature("bndl1").set("pressure", "P_cav");

        // 5. Mesh
        model.component("comp1").mesh().create("mesh1");
        model.component("comp1").mesh("mesh1").autoMeshSize(4);
        model.component("comp1").mesh("mesh1").run();

        // 6. Study
        model.study().create("std1");
        model.study("std1").create("stat", "Stationary");
        model.study("std1").feature("stat").set("geometricNonlinearity", true);
        model.study("std1").feature("stat").set("useparam", true);
        model.study("std1").feature("stat").set("pname", new String[]{"P_kPa"});
        model.study("std1").feature("stat").set("plistarr", new String[]{"0 10 20 30 40 50 60 70 80 90"});
        model.study("std1").feature("stat").set("punit", new String[]{""});

        model.study("std1").run();

        // 7. Tip Evaluation at x = 100: Top (100, 5, 0), Bottom (100, -5, 0)
        model.result().dataset().create("cpt_top", "CutPoint3D");
        model.result().dataset("cpt_top").set("data", "dset1");
        model.result().dataset("cpt_top").set("pointx", "100");
        model.result().dataset("cpt_top").set("pointy", "5");
        model.result().dataset("cpt_top").set("pointz", "0");

        model.result().dataset().create("cpt_bot", "CutPoint3D");
        model.result().dataset("cpt_bot").set("data", "dset1");
        model.result().dataset("cpt_bot").set("pointx", "100");
        model.result().dataset("cpt_bot").set("pointy", "-5");
        model.result().dataset("cpt_bot").set("pointz", "0");

        model.result().numerical().create("pev_top", "EvalPoint");
        model.result().numerical("pev_top").set("data", "cpt_top");
        model.result().numerical("pev_top").set("expr", new String[]{"u", "v"});
        model.result().numerical("pev_top").set("unit", new String[]{"mm", "mm"});
        double[][] uv_top = model.result().numerical("pev_top").getReal();

        model.result().numerical().create("pev_bot", "EvalPoint");
        model.result().numerical("pev_bot").set("data", "cpt_bot");
        model.result().numerical("pev_bot").set("expr", new String[]{"u", "v"});
        model.result().numerical("pev_bot").set("unit", new String[]{"mm", "mm"});
        double[][] uv_bot = model.result().numerical("pev_bot").getReal();

        double[] p_vals = new double[]{0, 10, 20, 30, 40, 50, 60, 70, 80, 90};
        System.out.println("=== COMSOL_TAPERED_CSV_START ===");
        System.out.println("P_kPa,u_top_mm,v_top_mm,u_bot_mm,v_bot_mm,x_tip_mm,delta_mm,theta_deg");
        int nSteps = Math.min(p_vals.length, uv_top[0].length);
        for (int i = 0; i < nSteps; i++) {
            double ut = uv_top[0][i];
            double vt = uv_top[1][i];
            double ub = uv_bot[0][i];
            double vb = uv_bot[1][i];
            double x_top = 100.0 + ut;
            double y_top = 5.0 + vt;
            double x_bot = 100.0 + ub;
            double y_bot = -5.0 + vb;
            double x_tip = 0.5 * (x_top + x_bot);
            double delta = -0.5 * (y_top + y_bot);
            double theta_deg = Math.atan2(x_top - x_bot, y_top - y_bot) * 180.0 / Math.PI;
            System.out.printf("%.2f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f%n",
                              p_vals[i], ut, vt, ub, vb, x_tip, delta, theta_deg);
        }
        System.out.println("=== COMSOL_TAPERED_CSV_END ===");

        // 8. 3D Plot Groups: Displacement & Stress
        model.result().create("pg_disp", "PlotGroup3D");
        model.result("pg_disp").set("data", "dset1");
        model.result("pg_disp").label("Total Displacement (solid.disp)");
        model.result("pg_disp").create("surf_d", "Surface");
        model.result("pg_disp").feature("surf_d").set("expr", "solid.disp");
        model.result("pg_disp").feature("surf_d").set("unit", "mm");
        model.result("pg_disp").feature("surf_d").create("def", "Deform");
        model.result("pg_disp").feature("surf_d").feature("def").set("expr", new String[]{"u", "v", "w"});

        model.result().create("pg_stress", "PlotGroup3D");
        model.result("pg_stress").set("data", "dset1");
        model.result("pg_stress").label("Stress (solid.mises)");
        model.result("pg_stress").create("surf_s", "Surface");
        model.result("pg_stress").feature("surf_s").set("expr", "solid.mises");
        model.result("pg_stress").feature("surf_s").set("unit", "MPa");
        model.result("pg_stress").feature("surf_s").create("def", "Deform");
        model.result("pg_stress").feature("surf_s").feature("def").set("expr", new String[]{"u", "v", "w"});

        try {
            model.result("pg_disp").set("solnum", "10"); // 90 kPa
            model.result("pg_disp").run();
            model.result().export().create("exp_disp90", "Image3D");
            model.result().export("exp_disp90").set("sourceobject", "pg_disp");
            model.result().export("exp_disp90").set("filename", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered\\comsol_tapered_disp_P90.png");
            model.result().export("exp_disp90").set("size", "manualweb");
            model.result().export("exp_disp90").set("unit", "px");
            model.result().export("exp_disp90").set("width", 1280);
            model.result().export("exp_disp90").set("height", 720);
            model.result().export("exp_disp90").set("colortheme", "default");
            model.result().export("exp_disp90").run();

            model.result("pg_stress").set("solnum", "10"); // 90 kPa
            model.result("pg_stress").run();
            model.result().export().create("exp_stress90", "Image3D");
            model.result().export("exp_stress90").set("sourceobject", "pg_stress");
            model.result().export("exp_stress90").set("filename", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered\\comsol_tapered_stress_P90.png");
            model.result().export("exp_stress90").set("size", "manualweb");
            model.result().export("exp_stress90").set("unit", "px");
            model.result().export("exp_stress90").set("width", 1280);
            model.result().export("exp_stress90").set("height", 720);
            model.result().export("exp_stress90").set("colortheme", "default");
            model.result().export("exp_stress90").run();
        } catch (Exception e) {
            System.out.println("Export notice: " + e.getMessage());
        }

        return model;
    }

    public static void main(String[] args) {
        run();
    }
}

