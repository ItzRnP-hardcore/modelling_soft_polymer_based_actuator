import com.comsol.model.*;
import com.comsol.model.util.*;

/*
 * 4-chamber segmented finger, matching grasp_model.Finger(R0=10, Rt=10, t0=1.5, tt=1.5, n_seg=4, E=0.15)
 * as used in tentacle_animation.py / grasp_sequence.py / report/grasp_analysis.tex section "Segmented fingers".
 *
 *  - tube R = 10 mm, concentric cavity r = 8.5 mm, L = 100 mm, Neo-Hookean E = 0.15 MPa, nu = 0.4995
 *  - 4 sealed chambers: cavities x in [1,24], [26,49], [51,74], [76,99] (2 mm bulkheads, 1 mm end walls)
 *  - strain-limiting layer: stiff strip (G x 1000) in the wall for Y < -9.25 mm (outer face at Y = -R)
 *  - hoop winding: same sleeve energy as actuator_fea._sleeve_energy, ks = 100*E*R
 *  - half model (Z >= 0), base clamped, follower pressure P_k = w_k * P on chamber k
 *  - pressure sweep 0..50 kPa for each chamber pattern
 * Prints tip data (CSV) and the deformed layer line at 50 kPa (shape CSV) to stdout,
 * and exports 3D displacement & von Mises stress PNGs with dset1 explicitly bound.
 */
public class BuildFinger4Chamber {
    static final double[] XS = {1.0, 26.0, 51.0, 76.0};
    static final double CL = 23.0;

    public static void main(String[] args) {
        Model model = ModelUtil.create("Model");
        model.modelPath("c:\\Users\\rudra\\Documents\\BTP\\comsol\\multi_cavity");

        model.param().set("R_out", "10[mm]");
        model.param().set("r_cav", "8.5[mm]");
        model.param().set("L_tot", "100[mm]");
        model.param().set("E_mod", "0.15[MPa]");
        model.param().set("nu_val", "0.4995");
        model.param().set("mu_mod", "E_mod/(2*(1+nu_val))");
        model.param().set("kappa_mod", "2*mu_mod*(1+nu_val)/(3*(1-2*nu_val))");
        model.param().set("ks_sleeve", "100*E_mod*R_out");
        model.param().set("P_kPa", "0");
        for (int k = 1; k <= 4; k++) {
            model.param().set("w" + k, "1");
            model.param().set("P" + k, "w" + k + "*P_kPa*1[kPa]");
        }

        // ---------------- geometry ----------------
        model.component().create("comp1", true);
        GeomSequence g = model.component("comp1").geom().create("geom1", 3);
        g.lengthUnit("mm");
        g.create("cyl_out", "Cylinder");
        g.feature("cyl_out").set("axistype", "x");
        g.feature("cyl_out").set("r", "R_out");
        g.feature("cyl_out").set("h", "L_tot");
        String[] cavTags = new String[4];
        for (int k = 1; k <= 4; k++) {
            String t = "cav" + k;
            cavTags[k - 1] = t;
            g.create(t, "Cylinder");
            g.feature(t).set("axistype", "x");
            g.feature(t).set("pos", new double[]{XS[k - 1], 0.0, 0.0});
            g.feature(t).set("r", "r_cav");
            g.feature(t).set("h", String.valueOf(CL));
        }
        g.create("dif1", "Difference");
        g.feature("dif1").selection("input").set("cyl_out");
        g.feature("dif1").selection("input2").set(cavTags);
        g.create("blk_half", "Block");
        g.feature("blk_half").set("pos", new double[]{-1, -11, 0});
        g.feature("blk_half").set("size", new double[]{102, 22, 11});
        g.create("int1", "Intersection");
        g.feature("int1").selection("input").set("dif1", "blk_half");
        g.create("blk_strip", "Block");
        g.feature("blk_strip").set("pos", new double[]{-1, -11, -1});
        g.feature("blk_strip").set("size", new double[]{102, 1.75, 13});   // Y < -9.25
        g.create("par1", "Partition");
        g.feature("par1").selection("input").set("int1");
        g.feature("par1").selection("tool").set("blk_strip");
        g.run();

        // ---------------- selections ----------------
        box(model, "sel_base", 2, -0.01, 0.01, -11, 11, -1, 11);
        box(model, "sel_sym", 2, -1, 101, -11, 11, -0.01, 0.01);
        box(model, "sel_strip", 3, -1, 101, -10.5, -9.2, -1, 11);
        model.component("comp1").selection().create("sel_outer_c", "Cylinder");
        model.component("comp1").selection("sel_outer_c").set("entitydim", 2);
        model.component("comp1").selection("sel_outer_c").set("condition", "allvertices");
        model.component("comp1").selection("sel_outer_c").set("axistype", "x");
        model.component("comp1").selection("sel_outer_c").set("r", "R_out+0.05[mm]");
        model.component("comp1").selection("sel_outer_c").set("rin", "R_out-0.05[mm]");
        model.component("comp1").selection("sel_outer_c").set("top", "L_tot+0.1[mm]");
        model.component("comp1").selection("sel_outer_c").set("bottom", "-0.1[mm]");
        diff(model, "sel_outer", "sel_outer_c", new String[]{"sel_sym", "sel_base"});
        for (int k = 1; k <= 4; k++) {
            box(model, "sel_cavbox" + k, 2, XS[k - 1] - 0.01, XS[k - 1] + CL + 0.01, -8.51, 8.51, -0.01, 8.51);
            diff(model, "sel_cav" + k, "sel_cavbox" + k, new String[]{"sel_sym"});
        }

        // ---------------- physics ----------------
        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
        hyper(model, "hmm1", "mu_mod", "kappa_mod");
        model.component("comp1").physics("solid").feature("hmm1").selection().all();
        hyper(model, "hmm2", "1000*mu_mod", "1000*kappa_mod");   // strain-limiting layer
        model.component("comp1").physics("solid").feature("hmm2").selection().named("sel_strip");
        model.component("comp1").physics("solid").create("fix1", "Fixed", 2);
        model.component("comp1").physics("solid").feature("fix1").selection().named("sel_base");
        model.component("comp1").physics("solid").create("sym1", "SymmetrySolid", 2);
        model.component("comp1").physics("solid").feature("sym1").selection().named("sel_sym");
        for (int k = 1; k <= 4; k++) {
            String t = "bndl" + k;
            model.component("comp1").physics("solid").create(t, "BoundaryLoad", 2);
            model.component("comp1").physics("solid").feature(t).selection().named("sel_cav" + k);
            model.component("comp1").physics("solid").feature(t).set("forceType", "FollowerPressure");
            model.component("comp1").physics("solid").feature(t).set("pressure", "P" + k);
        }
        // hoop winding (exact algebraic expansion of (lam_h^2 - 1)/2 so eh = 0 identically at u=v=w=0)
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
        model.component("comp1").physics("solid").feature("wf_sleeve").set("weakExpression", "-ks_sleeve*eh*test(eh)");

        model.component("comp1").mesh().create("mesh1");
        model.component("comp1").mesh("mesh1").autoMeshSize(5);
        model.component("comp1").mesh("mesh1").run();

        model.study().create("std1");
        model.study("std1").create("stat", "Stationary");
        model.study("std1").feature("stat").set("geometricNonlinearity", true);
        model.study("std1").feature("stat").set("useparam", true);
        model.study("std1").feature("stat").set("pname", new String[]{"P_kPa"});
        model.study("std1").feature("stat").set("plistarr", new String[]{"0 5 10 15 20 25 30 35 40 45 50"});
        model.study("std1").feature("stat").set("punit", new String[]{""});

        // ---------------- cases ----------------
        String[] names = {
            "all_4_chambers",
            "chamber_1_base",
            "chamber_2",
            "chamber_3",
            "chamber_4_tip",
            "chambers_1_2_proximal",
            "chambers_3_4_distal"
        };
        int[][] W = {{1,1,1,1},{1,0,0,0},{0,1,0,0},{0,0,1,0},{0,0,0,1},{1,1,0,0},{0,0,1,1}};
        StringBuilder xs = new StringBuilder(), ys = new StringBuilder(), zs = new StringBuilder();
        for (int i = 0; i <= 40; i++) { xs.append(2.5 * i).append(' '); ys.append("-10 "); zs.append("0 "); }

        for (int c = 0; c < names.length; c++) {
            String cname = names[c];
            for (int k = 0; k < 4; k++) model.param().set("w" + (k + 1), String.valueOf(W[c][k]));
            try {
                model.study("std1").run();
            } catch (Exception e) {
                System.out.println("SOLVER_NOTE " + cname + ": " + e.getMessage().split("\n")[0]);
            }
            if (c == 0) {
                cut(model, "cpt_tip", "100 100", "5 -5", "0 0");
                cut(model, "cpt_line", xs.toString().trim(), ys.toString().trim(), zs.toString().trim());
                model.result().numerical().create("pev_tip", "EvalPoint");
                model.result().numerical("pev_tip").set("data", "cpt_tip");
                model.result().numerical("pev_tip").set("expr", new String[]{"x", "y"});
                model.result().numerical("pev_tip").set("unit", new String[]{"mm", "mm"});
                model.result().numerical().create("pev_line", "EvalPoint");
                model.result().numerical("pev_line").set("data", "cpt_line");
                model.result().numerical("pev_line").set("expr", new String[]{"x", "y"});
                model.result().numerical("pev_line").set("unit", new String[]{"mm", "mm"});
                model.result().numerical().create("max_mises", "MaxVolume");
                model.result().numerical("max_mises").set("data", "dset1");
                model.result().numerical("max_mises").selection().all();
                model.result().numerical("max_mises").set("expr", new String[]{"solid.mises"});
                model.result().numerical("max_mises").set("unit", new String[]{"kPa"});

                // Create 3D Plot Groups AFTER std1.run() so dset1 exists and is bound!
                model.result().create("pg_disp", "PlotGroup3D");
                model.result("pg_disp").set("data", "dset1");
                model.result("pg_disp").label("Total Displacement (solid.disp)");
                model.result("pg_disp").create("surf_d", "Surface");
                model.result("pg_disp").feature("surf_d").set("expr", "solid.disp");
                model.result("pg_disp").feature("surf_d").set("unit", "mm");
                model.result("pg_disp").feature("surf_d").create("def", "Deform");
                model.result("pg_disp").feature("surf_d").feature("def").set("expr", new String[]{"u", "v", "w"});
                model.result("pg_disp").feature("surf_d").feature("def").set("scaleactive", true);
                model.result("pg_disp").feature("surf_d").feature("def").set("scale", 1);

                model.result().create("pg_stress", "PlotGroup3D");
                model.result("pg_stress").set("data", "dset1");
                model.result("pg_stress").label("Stress (solid.mises)");
                model.result("pg_stress").create("surf_s", "Surface");
                model.result("pg_stress").feature("surf_s").set("expr", "solid.mises");
                model.result("pg_stress").feature("surf_s").set("unit", "MPa");
                model.result("pg_stress").feature("surf_s").create("def", "Deform");
                model.result("pg_stress").feature("surf_s").feature("def").set("expr", new String[]{"u", "v", "w"});
                model.result("pg_stress").feature("surf_s").feature("def").set("scaleactive", true);
                model.result("pg_stress").feature("surf_s").feature("def").set("scale", 1);
            }
            double[][] tip = model.result().numerical("pev_tip").getReal();
            double[][] line = model.result().numerical("pev_line").getReal();
            double[][] mm = model.result().numerical("max_mises").getReal();
            int nSols = mm[0].length;
            System.out.println("TIPRAW," + cname + "," + flat(tip[0]) + "|" + flat(tip[1]));
            System.out.println("LINERAW," + cname + "," + flat(line[0]) + "|" + flat(line[1]));
            System.out.println("MISESRAW," + cname + "," + flat(mm[0]));

            // Export 3D Displacement and Stress PNGs at last solved pressure step (50 kPa)
            try {
                String solStr = String.valueOf(nSols);
                model.result("pg_disp").set("solnum", solStr);
                model.result("pg_disp").run();
                exportImg(model, "exp_d_" + cname, "pg_disp",
                    "c:\\Users\\rudra\\Documents\\BTP\\comsol\\multi_cavity\\comsol_" + cname + "_disp.png");

                model.result("pg_stress").set("solnum", solStr);
                model.result("pg_stress").run();
                exportImg(model, "exp_s_" + cname, "pg_stress",
                    "c:\\Users\\rudra\\Documents\\BTP\\comsol\\multi_cavity\\comsol_" + cname + "_stress.png");
            } catch (Exception e) {
                System.out.println("EXPORT_NOTE " + cname + ": " + e.getMessage());
            }

            if (c == 0) {
                try {
                    model.save("c:\\Users\\rudra\\Documents\\BTP\\comsol\\multi_cavity\\actuator_4cavity_solved_Model.mph");
                } catch (Exception e) {
                    System.out.println("SAVE_NOTE " + e.getMessage());
                }
            }
        }
    }

    static void exportImg(Model m, String tag, String pg, String filename) {
        m.result().export().create(tag, "Image3D");
        m.result().export(tag).set("sourceobject", pg);
        m.result().export(tag).set("filename", filename);
        m.result().export(tag).set("size", "manualweb");
        m.result().export(tag).set("unit", "px");
        m.result().export(tag).set("width", 1280);
        m.result().export(tag).set("height", 720);
        m.result().export(tag).set("colortheme", "default");
        m.result().export(tag).run();
    }

    static String flat(double[] a) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < a.length; i++) { if (i > 0) s.append(';'); s.append(a[i]); }
        return s.toString();
    }

    static void box(Model m, String tag, int dim, double x0, double x1, double y0, double y1, double z0, double z1) {
        m.component("comp1").selection().create(tag, "Box");
        m.component("comp1").selection(tag).set("entitydim", dim);
        m.component("comp1").selection(tag).set("condition", "inside");
        m.component("comp1").selection(tag).set("xmin", x0 + "[mm]");
        m.component("comp1").selection(tag).set("xmax", x1 + "[mm]");
        m.component("comp1").selection(tag).set("ymin", y0 + "[mm]");
        m.component("comp1").selection(tag).set("ymax", y1 + "[mm]");
        m.component("comp1").selection(tag).set("zmin", z0 + "[mm]");
        m.component("comp1").selection(tag).set("zmax", z1 + "[mm]");
    }

    static void diff(Model m, String tag, String add, String[] sub) {
        m.component("comp1").selection().create(tag, "Difference");
        m.component("comp1").selection(tag).set("entitydim", 2);
        m.component("comp1").selection(tag).set("add", new String[]{add});
        m.component("comp1").selection(tag).set("subtract", sub);
    }

    static void hyper(Model m, String tag, String G, String K) {
        m.component("comp1").physics("solid").create(tag, "HyperelasticModel", 3);
        m.component("comp1").physics("solid").feature(tag).set("MaterialModel", "NeoHookean");
        m.component("comp1").physics("solid").feature(tag).set("Compressibility_NeoHookean", "NearlyIncompressible");
        m.component("comp1").physics("solid").feature(tag).set("VolumetricEnergyUncoupled", "quadratic");
        m.component("comp1").physics("solid").feature(tag).set("MixedFormulation", "pFormulation");
        m.component("comp1").physics("solid").feature(tag).set("IsotropicOption", "KG");
        m.component("comp1").physics("solid").feature(tag).set("G_mat", "userdef");
        m.component("comp1").physics("solid").feature(tag).set("G", G);
        m.component("comp1").physics("solid").feature(tag).set("K_mat", "userdef");
        m.component("comp1").physics("solid").feature(tag).set("K", K);
    }

    static void cut(Model m, String tag, String px, String py, String pz) {
        m.result().dataset().create(tag, "CutPoint3D");
        m.result().dataset(tag).set("data", "dset1");
        m.result().dataset(tag).set("pointx", px);
        m.result().dataset(tag).set("pointy", py);
        m.result().dataset(tag).set("pointz", pz);
    }
}
