import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;

public class BuildActuatorSleeve {
    public static Model run() {
        Model model = ModelUtil.create("Model");
        model.modelPath("c:\\Users\\rudra\\Documents\\BTP\\comsol");
        model.label("actuator_sleeve.mph");

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
        model.param().set("ks_sleeve", "100*E_mod*R_out", "Sleeve hoop stiffness");
        model.param().set("P_kPa", "0", "Gauge pressure in kPa");
        model.param().set("P_cav", "P_kPa*1[kPa]", "Cavity pressure");

        // 2. Component & 3D Geometry (Half-model z >= 0)
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").lengthUnit("mm");

        model.component("comp1").geom("geom1").create("cyl_out", "Cylinder");
        model.component("comp1").geom("geom1").feature("cyl_out").set("axistype", "x");
        model.component("comp1").geom("geom1").feature("cyl_out").set("pos", new double[]{0.0, 0.0, 0.0});
        model.component("comp1").geom("geom1").feature("cyl_out").set("r", "R_out");
        model.component("comp1").geom("geom1").feature("cyl_out").set("h", "L_act+t_cap");

        model.component("comp1").geom("geom1").create("cyl_cav", "Cylinder");
        model.component("comp1").geom("geom1").feature("cyl_cav").set("axistype", "x");
        model.component("comp1").geom("geom1").feature("cyl_cav").set("pos", new String[]{"0", "r_p", "0"});
        model.component("comp1").geom("geom1").feature("cyl_cav").set("r", "r_cav");
        model.component("comp1").geom("geom1").feature("cyl_cav").set("h", "L_act");

        model.component("comp1").geom("geom1").create("dif1", "Difference");
        model.component("comp1").geom("geom1").feature("dif1").selection("input").set("cyl_out");
        model.component("comp1").geom("geom1").feature("dif1").selection("input2").set("cyl_cav");

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

        // Outer cylinder boundary selection: Cylinder with "allvertices"
        model.component("comp1").selection().create("sel_outer_box", "Cylinder");
        model.component("comp1").selection("sel_outer_box").set("entitydim", 2);
        model.component("comp1").selection("sel_outer_box").set("condition", "allvertices");
        model.component("comp1").selection("sel_outer_box").set("axistype", "x");
        model.component("comp1").selection("sel_outer_box").set("r", "R_out+0.1[mm]");
        model.component("comp1").selection("sel_outer_box").set("rin", "R_out-0.1[mm]");
        model.component("comp1").selection("sel_outer_box").set("top", "L_act+t_cap+0.1[mm]");
        model.component("comp1").selection("sel_outer_box").set("bottom", "-0.1[mm]");

        model.component("comp1").selection().create("sel_outer", "Difference");
        model.component("comp1").selection("sel_outer").set("entitydim", 2);
        model.component("comp1").selection("sel_outer").set("add", new String[]{"sel_outer_box"});
        model.component("comp1").selection("sel_outer").set("subtract", new String[]{"sel_sym", "sel_base"});

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

        model.component("comp1").physics("solid").create("sym1", "SymmetrySolid", 2);
        model.component("comp1").physics("solid").feature("sym1").selection().named("sel_sym");

        model.component("comp1").physics("solid").create("bndl1", "BoundaryLoad", 2);
        model.component("comp1").physics("solid").feature("bndl1").selection().named("sel_cav");
        model.component("comp1").physics("solid").feature("bndl1").set("forceType", "FollowerPressure");
        model.component("comp1").physics("solid").feature("bndl1").set("pressure", "P_cav");

        // Hoop-fibre sleeve on the outer surface, same energy as actuator_fea._sleeve_energy:
        //   W = ks/8 * (lam_h^2 - 1)^2 per unit reference area,  lam_h^2 = |F t|^2,
        //   t = (0, -Z, Y)/sqrt(Y^2+Z^2) the reference hoop direction (material coords X,Y,Z).
        // Uses tangential derivatives (uTY ...), valid because t is tangent to the outer surface.
        // (Previous version penalised (y*v+z*w) in spatial coords, which also resisted rigid
        //  lateral motion and therefore suppressed bending - removed.)
        model.component("comp1").variable().create("var_sleeve");
        model.component("comp1").variable("var_sleeve").selection().geom("geom1", 2);
        model.component("comp1").variable("var_sleeve").selection().named("sel_outer");
        model.component("comp1").variable("var_sleeve").set("Rr", "sqrt(Y^2+Z^2)");
        model.component("comp1").variable("var_sleeve").set("dut", "(-Z*uTY + Y*uTZ)/Rr");
        model.component("comp1").variable("var_sleeve").set("dvt", "(-Z*vTY + Y*vTZ)/Rr");
        model.component("comp1").variable("var_sleeve").set("dwt", "(-Z*wTY + Y*wTZ)/Rr");
        model.component("comp1").variable("var_sleeve").set("eh", "(-Z*dvt + Y*dwt)/Rr + 0.5*(dut^2 + dvt^2 + dwt^2)");
        model.component("comp1").physics("solid").create("wf_sleeve", "WeakContribution", 2);
        model.component("comp1").physics("solid").feature("wf_sleeve").selection().named("sel_outer");
        model.component("comp1").physics("solid").feature("wf_sleeve").set("weakExpression",
            "-ks_sleeve*eh*test(eh)");

        // 5. Mesh
        model.component("comp1").mesh().create("mesh1");
        model.component("comp1").mesh("mesh1").autoMeshSize(4);
        model.component("comp1").mesh("mesh1").run();

        // 6. Study (0 to 110 kPa)
        model.study().create("std1");
        model.study("std1").create("stat", "Stationary");
        model.study("std1").feature("stat").set("geometricNonlinearity", true);
        model.study("std1").feature("stat").set("useparam", true);
        model.study("std1").feature("stat").set("pname", new String[]{"P_kPa"});
        model.study("std1").feature("stat").set("plistarr", new String[]{"0 10 20 30 40 50 60 70 80 90 100 110"});
        model.study("std1").feature("stat").set("punit", new String[]{""});

        model.study("std1").run();

        // 7. Results extraction
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

        double[] p_vals = new double[]{0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110};
        System.out.println("=== COMSOL_SLEEVE_CSV_START ===");
        System.out.println("P_kPa,u_top_mm,v_top_mm,u_bot_mm,v_bot_mm,x_tip_mm,delta_mm,theta_deg");
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
            System.out.printf("%.2f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f%n",
                              p_vals[i], ut, vt, ub, vb, x_tip, delta, theta_deg);
        }
        System.out.println("=== COMSOL_SLEEVE_CSV_END ===");

        // 8. 3D Plot Groups: Displacement and Stress
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
            model.result("pg_disp").set("solnum", "6");
            model.result("pg_disp").run();
            model.result().export().create("exp_disp50", "Image3D");
            model.result().export("exp_disp50").set("sourceobject", "pg_disp");
            model.result().export("exp_disp50").set("filename", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\sleeved\\comsol_sleeve_disp_P50.png");
            model.result().export("exp_disp50").set("size", "manualweb");
            model.result().export("exp_disp50").set("unit", "px");
            model.result().export("exp_disp50").set("width", 1280);
            model.result().export("exp_disp50").set("height", 720);
            model.result().export("exp_disp50").set("colortheme", "default");
            model.result().export("exp_disp50").run();

            model.result("pg_disp").set("solnum", "12");
            model.result("pg_disp").run();
            model.result().export().create("exp_disp110", "Image3D");
            model.result().export("exp_disp110").set("sourceobject", "pg_disp");
            model.result().export("exp_disp110").set("filename", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\sleeved\\comsol_sleeve_disp_P110.png");
            model.result().export("exp_disp110").set("size", "manualweb");
            model.result().export("exp_disp110").set("unit", "px");
            model.result().export("exp_disp110").set("width", 1280);
            model.result().export("exp_disp110").set("height", 720);
            model.result().export("exp_disp110").set("colortheme", "default");
            model.result().export("exp_disp110").run();

            model.result("pg_stress").set("solnum", "12");
            model.result("pg_stress").run();
            model.result().export().create("exp_stress110", "Image3D");
            model.result().export("exp_stress110").set("sourceobject", "pg_stress");
            model.result().export("exp_stress110").set("filename", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\sleeved\\comsol_sleeve_stress_P110.png");
            model.result().export("exp_stress110").set("size", "manualweb");
            model.result().export("exp_stress110").set("unit", "px");
            model.result().export("exp_stress110").set("width", 1280);
            model.result().export("exp_stress110").set("height", 720);
            model.result().export("exp_stress110").set("colortheme", "default");
            model.result().export("exp_stress110").run();
        } catch (Exception e) {
            System.out.println("Export notice: " + e.getMessage());
        }

        return model;
    }

    public static void main(String[] args) { run(); }
}
